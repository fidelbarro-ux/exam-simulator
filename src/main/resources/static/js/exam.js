$(function () {
    let examQuestions = [];
    let answerReviews = [];

    function loadQuestions() {
        $('#questionsContainer').empty();
        $('#validationMessage').addClass('hidden').text('');
        $('#submitExamBtn').prop('disabled', true);

        $.ajax({
            url: '/api/exam/questions',
            type: 'GET',
            data: questionCountParameter(),
            dataType: 'json',
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

        $.each(examQuestions, function (index, question) {
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

    function updateProgress() {
        const total = examQuestions.length;
        const answered = examQuestions.filter(function (question) {
            return $('.answer-option[data-question-id="' + question.id + '"]:checked').length > 0;
        }).length;

        $('#progressText').text('Answered: ' + answered + ' / ' + total);
        $('#progressBar').css('width', total ? ((answered / total) * 100) + '%' : '0%');
    }

    $('#submitExamBtn').on('click', function () {
        const unanswered = [];

        $.each(examQuestions, function (_, question) {
            const selected = $('.answer-option[data-question-id="' + question.id + '"]:checked');
            if (selected.length === 0) {
                unanswered.push({
                    displayNumber: examQuestions.indexOf(question) + 1,
                    questionNumber: question.questionNumber,
                    id: question.id
                });
                $('#question-card-' + question.id).addClass('unanswered');
            }
        });

        if (unanswered.length > 0) {
            const labels = unanswered.map(function (item) {
                return 'Question ' + item.displayNumber;
            });

            showValidationError(
                'Please answer the following questions: ' + labels.join(', ')
            );

            const first = $('#question-card-' + unanswered[0].id)[0];
            if (first) {
                first.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
            return;
        }

        const payload = {
            answers: examQuestions.map(function (question) {
                return {
                    questionId: question.id,
                    selectedAnswer: $('.answer-option[data-question-id="' + question.id + '"]:checked')
                        .val()
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
                .toggleClass('incorrect', !review.correct);

            $('<h3>')
                .text('Question ' + (index + 1))
                .appendTo($card);
            $('<p>')
                .addClass('review-question')
                .text(review.questionText)
                .appendTo($card);

            const $options = $('<div>').addClass('review-options');
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
            $card.append($options);

            const $details = $('<dl>').addClass('review-details');
            addReviewDetail($details, 'Correct answer', review.correctAnswer + '. ' + review.correctAnswerText, false, true);
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

    loadQuestions();
});
