$(function () {
    const examSessionId = getExamSessionId();
    let examQuestions = [];
    let answerReviews = [];
    const matchingAssignments = new Map();
    const selectedMatchingTerms = new Map();
    let matchingTermsObserver;

    function loadQuestions() {
        $('#questionsContainer').empty();
        $('#validationMessage').addClass('hidden').text('');
        $('#submitExamBtn').prop('disabled', true);
        matchingAssignments.clear();
        selectedMatchingTerms.clear();

        $.ajax({
            url: '/api/exam/questions',
            type: 'GET',
            data: questionCountParameter(),
            dataType: 'json',
            headers: { 'X-Exam-Session-Id': examSessionId },
            success: function (questions) {
                examQuestions = questions || [];

                if (examQuestions.length === 0) {
                    $('#questionsContainer').html(
                        '<div class="message error">No questions are available. Please import an exam first.</div>'
                    );
                    $('#examCount').text('No questions available');
                    return;
                }

                renderQuestions();
                updateProgress();
                $('#submitExamBtn').prop('disabled', false);
            },
            error: function (xhr) {
                showValidationError(getErrorMessage(xhr, 'Unable to load the exam questions.'));
            }
        });
    }

    function questionCountParameter() {
        const value = $('#questionCount').val().trim();
        return value ? { count: value } : {};
    }

    function renderQuestions() {
        $('#questionsContainer').empty();

        $('#examCount').text(
            examQuestions.length + ' question' + (examQuestions.length === 1 ? '' : 's')
        );

        const renderedMatchingGroups = new Set();
        $.each(examQuestions, function (index, question) {
            if (question.questionType === 'MATCHING') {
                const group = question.matchingGroup || ('matching-' + question.id);
                if (!renderedMatchingGroups.has(group)) {
                    renderMatchingGroup(group);
                    renderedMatchingGroups.add(group);
                }
                return;
            }
            const $card = $('<article>')
                .addClass('question-card')
                .attr('id', 'question-card-' + question.id)
                .attr('data-question-id', question.id);

            $('<div>')
                .addClass('question-title')
                .append(
                    $('<span>').addClass('question-number').text((index + 1) + '.'),
                    document.createTextNode(' ' + question.questionText)
                )
                .appendTo($card);

            const $options = $('<div>').addClass('options');

            $.each(question.options, function (_, option) {
                const inputId = 'q' + question.id + '-' + option.letter;

                const $input = $('<input>')
                    .attr({
                        type: 'checkbox',
                        id: inputId,
                        name: 'question-' + question.id,
                        value: option.letter
                    })
                    .addClass('answer-option')
                    .attr('data-question-id', question.id);

                const $label = $('<label>')
                    .addClass('option-label')
                    .attr('for', inputId)
                    .append(
                        $input,
                        $('<span>').addClass('option-letter').text(option.letter + '.'),
                        $('<span>').addClass('option-text').text(option.text)
                    );

                $options.append($label);
            });

            $card.append($options);
            $('#questionsContainer').append($card);
        });

        observeMatchingTerms();
    }

    function renderMatchingGroup(group) {
        const questions = examQuestions.filter(question => question.questionType === 'MATCHING'
            && (question.matchingGroup || ('matching-' + question.id)) === group);
        const $card = $('<section>').addClass('question-card matching-card').attr('data-matching-group', group);
        $card.append($('<h2>').text('Matching'));
        $card.append($('<p>').addClass('matching-help').text('Drag a term to an answer blank, or select a term then activate a blank. Press Delete on an assigned blank to remove it.'));
        const $terms = $('<div>').addClass('matching-terms').attr('aria-label', 'Answer terms');
        $.each(questions[0].options, function (_, option) {
            $terms.append($('<button type="button">').addClass('matching-term').attr({
                draggable: 'true', 'data-group': group, 'data-term': option.letter, 'aria-pressed': 'false'
            }).text(option.text));
        });
        const $rows = $('<div>').addClass('matching-questions');
        $.each(questions, function (_, question) {
            const display = examQuestions.indexOf(question) + 1;
            $rows.append($('<div>').addClass('matching-row').attr('id', 'question-card-' + question.id)
                .append($('<button type="button">').addClass('matching-blank').attr({
                        'data-group': group, 'data-question-id': question.id,
                        'aria-label': 'Answer blank for question ' + display
                    }).text('Drop answer here'),
                    $('<span>').text(display + '. ' + question.questionText)));
        });
        const $termsBox = $('<div>').addClass('matching-terms-box')
            .append($('<h3>').text('Answer terms'), $terms);
        const $stickyTerms = $('<div>').addClass('matching-terms-sticky')
            .append($termsBox);
        const $termsSentinel = $('<div>').addClass('matching-terms-sentinel').attr('aria-hidden', 'true');
        $('#questionsContainer').append($card.append($termsSentinel, $stickyTerms, $rows));
    }

    function observeMatchingTerms() {
        if (matchingTermsObserver) matchingTermsObserver.disconnect();

        // IntersectionObserver avoids work on every scroll frame. The sentinel marks
        // the point where the sticky box reaches its viewport offset.
        matchingTermsObserver = new IntersectionObserver(function (entries) {
            $.each(entries, function (_, entry) {
                $(entry.target).next('.matching-terms-sticky').toggleClass('is-floating', !entry.isIntersecting);
            });
        }, { rootMargin: '-12px 0px 0px 0px', threshold: 0 });

        $('.matching-terms-sentinel').each(function () {
            matchingTermsObserver.observe(this);
        });
    }

    // Checkboxes are required by the specification, but the exam allows one answer only.
    $(document).on('change', '.answer-option', function () {
        const questionId = $(this).data('question-id');

        if ($(this).is(':checked')) {
            $('.answer-option[data-question-id="' + questionId + '"]')
                .not(this)
                .prop('checked', false);
        }

        const $card = $('#question-card-' + questionId);
        const answered = $('.answer-option[data-question-id="' + questionId + '"]:checked').length > 0;
        $card.toggleClass('unanswered', !answered);

        $card.find('.option-label').removeClass('selected');
        $('.answer-option[data-question-id="' + questionId + '"]:checked')
            .closest('.option-label')
            .addClass('selected');

        updateProgress();
        $('#validationMessage').addClass('hidden').text('');
    });

    function matchingAnswer(question) {
        const group = question.matchingGroup || ('matching-' + question.id);
        const assignments = matchingAssignments.get(group);
        return assignments ? assignments.get(question.id) : null;
    }

    function setMatchingAnswer(group, questionId, term) {
        if (!term) return;
        const assignments = matchingAssignments.get(group) || new Map();
        $.each(Array.from(assignments.entries()), function (_, assignment) {
            if (assignment[1] === term) assignments.delete(assignment[0]);
        });
        assignments.set(Number(questionId), term);
        matchingAssignments.set(group, assignments);
        selectedMatchingTerms.delete(group);
        renderMatchingAssignments(group);
        updateProgress();
    }

    function removeMatchingAnswer(group, questionId) {
        const assignments = matchingAssignments.get(group);
        if (assignments) assignments.delete(Number(questionId));
        renderMatchingAssignments(group);
        updateProgress();
    }

    function renderMatchingAssignments(group) {
        const assignments = matchingAssignments.get(group) || new Map();
        const selectedTerm = selectedMatchingTerms.get(group);
        const $card = $('.matching-card[data-matching-group="' + group + '"]');
        const questions = examQuestions.filter(question => question.questionType === 'MATCHING'
            && (question.matchingGroup || ('matching-' + question.id)) === group);
        $card.find('.matching-term').each(function () {
            const term = $(this).data('term');
            $(this).prop('disabled', Array.from(assignments.values()).includes(term))
                .toggleClass('selected', term === selectedTerm).attr('aria-pressed', term === selectedTerm);
        });
        $.each(questions, function (_, question) {
            const term = assignments.get(question.id);
            const text = term && question.options.find(option => option.letter === term).text;
            const $blank = $card.find('.matching-blank[data-question-id="' + question.id + '"]');
            $blank.text(text || 'Drop answer here').toggleClass('assigned', Boolean(term))
                .attr('aria-label', text ? 'Assigned ' + text + '. Press Delete to remove.' : 'Answer blank');
            $('#question-card-' + question.id).toggleClass('unanswered', !term);
        });
    }

    $(document).on('dragstart', '.matching-term', function (event) {
        event.originalEvent.dataTransfer.setData('text/plain', $(this).data('term'));
    });
    $(document).on('dragover', '.matching-blank', function (event) { event.preventDefault(); });
    $(document).on('drop', '.matching-blank', function (event) {
        event.preventDefault(); setMatchingAnswer($(this).data('group'), $(this).data('question-id'), event.originalEvent.dataTransfer.getData('text/plain'));
    });
    $(document).on('click', '.matching-term', function () {
        const group = $(this).data('group'); selectedMatchingTerms.set(group, $(this).data('term')); renderMatchingAssignments(group);
    });
    $(document).on('click', '.matching-blank', function () {
        const group = $(this).data('group'); const term = selectedMatchingTerms.get(group);
        if (term) setMatchingAnswer(group, $(this).data('question-id'), term);
        else if ($(this).hasClass('assigned')) removeMatchingAnswer(group, $(this).data('question-id'));
    });
    $(document).on('keydown', '.matching-blank', function (event) {
        if (event.key === 'Delete' || event.key === 'Backspace') {
            event.preventDefault(); removeMatchingAnswer($(this).data('group'), $(this).data('question-id'));
        }
    });

    function updateProgress() {
        const total = examQuestions.length;
        const answered = examQuestions.filter(function (question) {
            return question.questionType === 'MATCHING'
                ? Boolean(matchingAnswer(question))
                : $('.answer-option[data-question-id="' + question.id + '"]:checked').length > 0;
        }).length;

        $('#progressText').text('Answered: ' + answered + ' / ' + total);
        $('#progressBar').css('width', total ? ((answered / total) * 100) + '%' : '0%');
    }

    $('#submitExamBtn').on('click', function () {
        const unanswered = examQuestions.filter(function (question) {
            return question.questionType === 'MATCHING'
                ? !matchingAnswer(question)
                : $('.answer-option[data-question-id="' + question.id + '"]:checked').length === 0;
        });
        if (unanswered.length > 0) {
            $.each(unanswered, function (_, question) {
                $('#question-card-' + question.id).addClass('unanswered');
            });
            showValidationError('Answer every question before submitting.');
            const first = $('#question-card-' + unanswered[0].id)[0];
            if (first) first.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return;
        }

        const payload = {
            answers: examQuestions.map(function (question) {
                return {
                    questionId: question.id,
                    selectedAnswer: question.questionType === 'MATCHING'
                        ? matchingAnswer(question)
                        : $('.answer-option[data-question-id="' + question.id + '"]:checked').val() || null
                };
            })
        };

        const $button = $(this);
        $button.prop('disabled', true).text('Submitting...');

        $.ajax({
            url: '/api/exam/submit',
            type: 'POST',
            contentType: 'application/json',
            dataType: 'json',
            headers: { 'X-Exam-Session-Id': examSessionId },
            data: JSON.stringify(payload),
            success: function (result) {
                showResult(result);
            },
            error: function (xhr) {
                showValidationError(getErrorMessage(xhr, 'Unable to submit the exam.'));
            },
            complete: function () {
                $button.prop('disabled', false).text('Submit Exam');
            }
        });
    });

    function showResult(result) {
        answerReviews = result.answerReviews || [];
        $('#examSection').addClass('hidden');
        $('#resultSection').removeClass('hidden');
        renderAnswerReviews();
        $('#answerReviewSection').removeClass('hidden');
        $('#reviewAnswersBtn').attr('aria-expanded', 'true');

        $('#scoreDisplay').text(
            result.correctAnswers + ' / ' + result.totalQuestions +
            ' CORRECT — SCORE: ' + result.scorePercentage + '%'
        );
        const passed = result.scorePercentage >= 75;
        $('#scoreDisplay')
            .toggleClass('score-passed', passed)
            .toggleClass('score-failed', !passed);
        $('<span>')
            .addClass('score-status')
            .toggleClass('passed', passed)
            .toggleClass('failed', !passed)
            .text(' (' + (passed ? 'Passed' : 'Failed') + ')')
            .appendTo('#scoreDisplay');
        $('#correctAnswers').text(result.correctAnswers);
        $('#incorrectAnswers').text(result.incorrectAnswers);
        $('#totalQuestions').text(result.totalQuestions);

        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    $('#reviewAnswersBtn').on('click', function () {
        const $section = $('#answerReviewSection');
        const isHidden = $section.hasClass('hidden');

        if (isHidden) {
            renderAnswerReviews();
            $section.removeClass('hidden');
            $(this).attr('aria-expanded', 'true');
            $section[0].scrollIntoView({ behavior: 'smooth', block: 'start' });
        } else {
            $section.addClass('hidden');
            $(this).attr('aria-expanded', 'false');
        }
    });

    function renderAnswerReviews() {
        const $container = $('#answerReviewContainer').empty();

        $.each(answerReviews, function (index, review) {
            const $card = $('<article>')
                .addClass('review-card')
                .toggleClass('correct', review.correct)
                .toggleClass('incorrect', !review.correct && Boolean(review.selectedAnswer))
                .toggleClass('neutral', !review.selectedAnswer);

            $('<h3>')
                .text('Question ' + (index + 1))
                .appendTo($card);
            $('<p>')
                .addClass('review-question')
                .text(review.questionText)
                .appendTo($card);

            const $options = $('<div>').addClass('review-options');
            if (review.questionType === 'MATCHING') {
                const $answer = $('<div>').addClass('review-option matching-review-answer')
                    .toggleClass('selected-correct', review.correct)
                    .toggleClass('selected-incorrect', !review.correct && Boolean(review.selectedAnswer))
                    .toggleClass('unanswered-answer', !review.selectedAnswer)
                    .append($('<span>').addClass('review-option-text').text(review.selectedAnswerText || 'Unanswered'));
                if (review.selectedAnswer) {
                    $answer.append($('<span>').addClass('review-answer-status')
                        .text(review.correct ? '✓ Correct' : '✗ Incorrect'));
                }
                $options.append($answer);
            } else {
            $.each(review.options || [], function (_, option) {
                const isSelected = option.letter === review.selectedAnswer;
                const $option = $('<div>')
                    .addClass('review-option')
                    .toggleClass('selected-correct', isSelected && review.correct)
                    .toggleClass('selected-incorrect', isSelected && !review.correct)
                    .append(
                        $('<span>').addClass('review-option-letter').text(option.letter + '.'),
                        $('<span>').addClass('review-option-text').text(option.text)
                    );

                if (isSelected) {
                    $option.append(
                        $('<span>')
                            .addClass('review-answer-status')
                            .text(review.correct ? '✓ Correct' : '✕ Incorrect')
                    );
                }
                $options.append($option);
            });
            }
            $card.append($options);

            const $details = $('<dl>').addClass('review-details');
            const correctAnswerDisplay = review.questionType === 'MATCHING'
                ? review.correctAnswerText
                : review.correctAnswer + '. ' + review.correctAnswerText;
            addReviewDetail($details, 'Correct answer', correctAnswerDisplay, false, true);
            addReviewDetail($details, 'Explanation', review.explanation, true);
            $card.append($details);
            $container.append($card);
        });
    }

    function addReviewDetail($details, label, value, isExplanation, isCorrectAnswer) {
        $('<dt>').text(label).appendTo($details);
        $('<dd>')
            .toggleClass('review-explanation', Boolean(isExplanation))
            .toggleClass('correct-answer-value', Boolean(isCorrectAnswer))
            .text(value)
            .appendTo($details);
    }

    $('#retryExamBtn').on('click', function () {
        // Fetch a fresh randomized set using the selected exam length.
        $('#resultSection').addClass('hidden');
        $('#examSection').removeClass('hidden');
        loadQuestions();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    $('#newSetBtn').on('click', function () {
        if (!window.confirm('It will remove your answers (if any). Are you sure you want to refresh the exam?')) {
            return;
        }

        const value = $('#questionCount').val().trim();
        if (value && (!/^\d+$/.test(value) || Number(value) < 1)) {
            showValidationError('Enter a whole number of at least 1, or leave the field blank for all questions.');
            return;
        }
        loadQuestions();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    $('#newTestBtn').on('click', function () {
        window.location.href = '/';
    });

    function showValidationError(message) {
        $('#validationMessage')
            .removeClass('hidden')
            .text(message);
    }

    function getErrorMessage(xhr, fallback) {
        if (xhr.responseJSON) {
            return xhr.responseJSON.details || xhr.responseJSON.message || fallback;
        }
        return fallback;
    }

    function getExamSessionId() {
        const key = 'examSimulatorSessionId';
        let id = window.sessionStorage.getItem(key);
        if (!id) {
            id = window.crypto && window.crypto.randomUUID
                ? window.crypto.randomUUID().replace(/[^A-Za-z0-9-]/g, '')
                : Date.now().toString(36) + Math.random().toString(36).slice(2);
            window.sessionStorage.setItem(key, id);
        }
        return id;
    }

    loadQuestions();
});
