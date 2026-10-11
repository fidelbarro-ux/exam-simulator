$(function () {
    const sampleContent = `No. 1) Which of the following qualities is MORE likely to be found in a tester's mindset rather than in a developer's?

a. A tester's mindset tends to grow and mature as the tester gains experience
b. Ability to see what might go wrong
c. Good communication with team members
d. Focus on getting all things done

Correct Answer: B
Explanation: A tester anticipates problems and investigates what could fail.

No. 2) Which statement best describes unit testing?

a. Testing the complete application
b. Testing individual components in isolation
c. Testing network connectivity
d. Testing production deployment

Correct Answer: B
Explanation: Unit testing verifies an individual component independently.

No. 3) Which activity is most closely associated with identifying potential defects early?

a. Static testing
b. Production monitoring
c. Database backup
d. Release packaging

Correct Answer: A
Explanation: Static testing finds issues before the software is executed.

No. 4) What is a useful benefit of independent testing?

a. It removes the need for developers
b. It can provide a different perspective on the software
c. It guarantees zero defects
d. It eliminates requirements

Correct Answer: B
Explanation: Independent testers bring a perspective separate from implementation.

No. 5) Which statement best describes regression testing?

a. Testing only new code
b. Testing whether changes have adversely affected existing functionality
c. Testing network cables
d. Testing user passwords

Correct Answer: B
Explanation: Regression testing checks whether changes damaged existing behavior.`;

    if (!$('#questionContent').val()) {
        $('#questionContent').val(sampleContent);
    }

    function showMessage(text, type) {
        $('#statusMessage')
            .removeClass('hidden success error')
            .addClass(type)
            .text(text);
    }

    $('#generateBtn').on('click', function () {
        const content = $('#questionContent').val().trim();

        if (!content) {
            showMessage('Please paste exam questions before generating the test.', 'error');
            return;
        }

        const $button = $(this);
        $button.prop('disabled', true).text('Importing...');

        $.ajax({
            url: '/api/questions/import',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ content: content }),
            success: function (response) {
                showMessage(
                    response.totalQuestions + ' questions successfully imported.',
                    'success'
                );

                $('<button>')
                    .addClass('btn btn-primary')
                    .attr('id', 'startExamBtn')
                    .text('Start Exam')
                    .on('click', function () {
                        window.location.href = '/exam.html';
                    })
                    .appendTo('.button-row');
            },
            error: function (xhr) {
                const response = xhr.responseJSON;
                const message = response && response.details
                    ? response.details
                    : 'Unable to import the questions. Please check the format.';
                showMessage(message, 'error');
            },
            complete: function () {
                $button.prop('disabled', false).text('Generate Test');
            }
        });
    });

    $('#clearBtn').on('click', function () {
        $('#questionContent').val('');
        $('#statusMessage').addClass('hidden').text('');
        $('#startExamBtn').remove();
    });
});
