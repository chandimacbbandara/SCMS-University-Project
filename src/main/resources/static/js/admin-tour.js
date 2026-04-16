(function () {
    var cfg = window.adminTourConfig;
    if (!cfg || !Array.isArray(cfg.steps) || cfg.steps.length === 0) {
        return;
    }

    var storageKey = cfg.storageKey || 'akb_admin_tour_completed';
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
            currentTarget.classList.remove('admin-tour-highlight');
        }
        currentTarget = null;
    }

    function cleanup() {
        if (positionTimer) {
            clearTimeout(positionTimer);
            positionTimer = null;
        }
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
        var target = String(cfg.nextPage);
        var hashIndex = target.indexOf('#');
        var hashPart = hashIndex >= 0 ? target.slice(hashIndex) : '';
        var basePart = hashIndex >= 0 ? target.slice(0, hashIndex) : target;
        var hasQuery = basePart.indexOf('?') >= 0;
        return basePart + (hasQuery ? '&' : '?') + 'tour=1' + hashPart;
    }

    function activateStepContext(step) {
        if (!step) {
            return;
        }

        if (step.activateHash) {
            var targetHash = String(step.activateHash).replace(/^#/, '');
            var currentHash = String(window.location.hash || '').replace(/^#/, '');
            if (targetHash) {
                if (currentHash !== targetHash) {
                    window.location.hash = targetHash;
                } else {
                    window.dispatchEvent(new Event('hashchange'));
                }
            }
        }

        if (step.activateTabSelector) {
            var tabTrigger = q(step.activateTabSelector);
            if (tabTrigger && typeof tabTrigger.click === 'function') {
                tabTrigger.click();
            }
        }
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

        // Reposition repeatedly because smooth scrolling/layout can shift target bounds.
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

        activateStepContext(step);

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
        currentTarget.classList.add('admin-tour-highlight');
        currentTarget.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });

        var stepNo = currentIndex + 1;
        var total = cfg.steps.length;

        tooltip.innerHTML = '' +
            '<span class="admin-tour-step">Step ' + stepNo + ' of ' + total + '</span>' +
            '<h4 class="admin-tour-title">' + (step.title || 'Tour Step') + '</h4>' +
            '<p class="admin-tour-text">' + (step.content || '') + '</p>' +
            '<div class="admin-tour-actions">' +
                '<div class="admin-tour-left">' +
                    '<button type="button" class="admin-tour-btn" data-tour="skip">Skip</button>' +
                '</div>' +
                '<div class="admin-tour-right">' +
                    (stepNo > 1 ? '<button type="button" class="admin-tour-btn" data-tour="prev">Back</button>' : '') +
                    '<button type="button" class="admin-tour-btn primary" data-tour="next">' +
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
        overlay.className = 'admin-tour-overlay';
        overlay.addEventListener('click', completeTour);

        tooltip = document.createElement('div');
        tooltip.className = 'admin-tour-tooltip';
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
        launchBtn.className = 'admin-tour-launch';
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
