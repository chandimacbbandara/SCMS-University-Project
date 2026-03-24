(function () {
    function debounce(fn, delay) {
        let timer;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    async function callModeration(text, contentType) {
        const response = await fetch('/student/community/moderate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: text, contentType: contentType })
        });

        if (!response.ok) {
            return { decision: 'WARN', reason: 'Moderation service temporarily unavailable.', riskScore: 70 };
        }

        return await response.json();
    }

    function bindModerationForField(textarea) {
        const contentType = textarea.dataset.modType || 'post';
        const hint = textarea.parentElement.querySelector('.moderation-hint');
        const form = textarea.closest('form');
        const submitBtn = form ? form.querySelector('button[type="submit"]') : null;

        const run = debounce(async () => {
            const value = textarea.value || '';
            if (!value.trim()) {
                if (hint) {
                    hint.textContent = '';
                    hint.className = 'moderation-hint';
                }
                if (submitBtn) {
                    submitBtn.disabled = false;
                }
                return;
            }

            const result = await callModeration(value, contentType);
            if (!hint) {
                return;
            }

            const normalized = (result.decision || 'ALLOW').toLowerCase();
            hint.className = 'moderation-hint ' + normalized;
            hint.textContent = result.reason || 'Moderation checked.';

            if (submitBtn) {
                submitBtn.disabled = normalized === 'block';
            }
        }, 800);

        textarea.addEventListener('input', run);
    }

    function toggleById(id) {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }
        element.style.display = element.style.display === 'none' || element.style.display === '' ? 'block' : 'none';
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('textarea[data-mod-type]').forEach(bindModerationForField);

        document.querySelectorAll('[data-toggle-id]').forEach(btn => {
            btn.addEventListener('click', function () {
                toggleById(this.dataset.toggleId);
            });
        });
    });
})();
