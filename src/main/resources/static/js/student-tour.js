(function () {
    var cfg = window.studentTourConfig;
    if (!cfg || !Array.isArray(cfg.steps) || cfg.steps.length === 0) {
        return;
    }

    var storageKey = cfg.storageKey || 'akb_student_tour_completed';
    var overlay = null;
    var tooltip = null;
    var launchBtn = null;
    var currentIndex = 0;
    var currentTarget = null;
    var positionTimer = null;

    function q(selector) {
        return selector ? document.querySelector(selector) : null;
    }

    function removeHighlight() {
        if (currentTarget) {
            currentTarget.classList.remove('student-tour-highlight');
        }
        currentTarget = null;
    }

    function cleanup() {
        removeHighlight();
        if (overlay) {
            overlay.remove();
            overlay = null;
        }
        if (tooltip) {
            tooltip.remove();
            tooltip = null;
        }
    }

    function completeTour() {
        localStorage.setItem(storageKey, 'true');
        cleanup();
    }

    function nextPageUrl() {
        if (!cfg.nextPage) {
            return '';
        }
        return cfg.nextPage + (cfg.nextPage.indexOf('?') >= 0 ? '&' : '?') + 'tour=1';
    }

    function calcPosition(targetRect) {
        var width = Math.min(360, window.innerWidth - 24);
        var left = Math.max(12, Math.min(targetRect.left, window.innerWidth - width - 12));
        var top = targetRect.bottom + 12;
        var tooltipHeight = tooltip ? tooltip.offsetHeight : 180;

        if (top + tooltipHeight > window.innerHeight - 12) {
            top = Math.max(12, targetRect.top - tooltipHeight - 12);
        }

        return { left: left, top: top };
    }

    function placeTooltip() {
        if (!tooltip || !currentTarget) {
            return;
        }
        var rect = currentTarget.getBoundingClientRect();
        var pos = calcPosition(rect);
        tooltip.style.left = pos.left + 'px';
        tooltip.style.top = pos.top + 'px';
    }

    function schedulePlaceTooltip() {
        if (positionTimer) {
            clearTimeout(positionTimer);
            positionTimer = null;
        }

        // Reposition a few times because smooth scrolling and layout transitions can shift target bounds.
        placeTooltip();
        requestAnimationFrame(placeTooltip);
        positionTimer = setTimeout(function () {
            placeTooltip();
            setTimeout(placeTooltip, 180);
        }, 120);
    }

    function renderStep() {
        var step = cfg.steps[currentIndex];
        if (!step) {
            completeTour();
            return;
        }

        var target = q(step.selector);
        if (!target) {
            var i;
            for (i = currentIndex + 1; i < cfg.steps.length; i += 1) {
                if (q(cfg.steps[i].selector)) {
                    currentIndex = i;
                    renderStep();
                    return;
                }
            }
            if (cfg.nextPage) {
                window.location.href = nextPageUrl();
                return;
            }
            completeTour();
            return;
        }

        removeHighlight();
        currentTarget = target;
        currentTarget.classList.add('student-tour-highlight');
        currentTarget.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });

        var stepNo = currentIndex + 1;
        var total = cfg.steps.length;

        tooltip.innerHTML = '' +
            '<span class="student-tour-step">Step ' + stepNo + ' of ' + total + '</span>' +
            '<h4 class="student-tour-title">' + (step.title || 'Tour Step') + '</h4>' +
            '<p class="student-tour-text">' + (step.content || '') + '</p>' +
            '<div class="student-tour-actions">' +
                '<div class="student-tour-left">' +
                    '<button type="button" class="student-tour-btn" data-tour="skip">Skip</button>' +
                '</div>' +
                '<div class="student-tour-right">' +
                    (stepNo > 1 ? '<button type="button" class="student-tour-btn" data-tour="prev">Back</button>' : '') +
                    '<button type="button" class="student-tour-btn primary" data-tour="next">' +
                        (stepNo === total ? (cfg.nextPage ? 'Next Page' : 'Finish') : 'Next') +
                    '</button>' +
                '</div>' +
            '</div>';

        schedulePlaceTooltip();
    }

    function startTour(force) {
        if (!force && localStorage.getItem(storageKey) === 'true') {
            return;
        }

        cleanup();

        overlay = document.createElement('div');
        overlay.className = 'student-tour-overlay';
        overlay.addEventListener('click', completeTour);

        tooltip = document.createElement('div');
        tooltip.className = 'student-tour-tooltip';
        tooltip.addEventListener('click', function (evt) {
            evt.stopPropagation();
            var action = evt.target && evt.target.getAttribute('data-tour');
            if (!action) {
                return;
            }
            if (action === 'skip') {
                completeTour();
                return;
            }
            if (action === 'prev') {
                currentIndex = Math.max(0, currentIndex - 1);
                renderStep();
                return;
            }
            if (action === 'next') {
                if (currentIndex < cfg.steps.length - 1) {
                    currentIndex += 1;
                    renderStep();
                } else if (cfg.nextPage) {
                    completeTour();
                    window.location.href = nextPageUrl();
                } else {
                    completeTour();
                }
            }
        });

        document.body.appendChild(overlay);
        document.body.appendChild(tooltip);
        currentIndex = 0;
        renderStep();
    }

    function mountLaunchButton() {
        launchBtn = document.createElement('button');
        launchBtn.type = 'button';
        launchBtn.className = 'student-tour-launch';
        launchBtn.textContent = 'Start Tour';
        launchBtn.addEventListener('click', function () {
            startTour(true);
        });
        document.body.appendChild(launchBtn);
    }

    window.addEventListener('resize', function () {
        schedulePlaceTooltip();
    });

    window.addEventListener('scroll', function () {
        schedulePlaceTooltip();
    }, true);

    document.addEventListener('keydown', function (evt) {
        if (evt.key === 'Escape') {
            completeTour();
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        mountLaunchButton();

        var params = new URLSearchParams(window.location.search);
        if (params.get('tour') === '1') {
            startTour(true);
            params.delete('tour');
            var qs = params.toString();
            var cleanUrl = window.location.pathname + (qs ? '?' + qs : '') + window.location.hash;
            window.history.replaceState({}, '', cleanUrl);
            return;
        }

        if (cfg.autoStart && localStorage.getItem(storageKey) !== 'true') {
            setTimeout(function () {
                startTour(false);
            }, 700);
        }
    });
})();
