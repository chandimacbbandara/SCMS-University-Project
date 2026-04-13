(function () {
    'use strict';

    function toArray(nodeList) {
        return Array.prototype.slice.call(nodeList || []);
    }

    function toggleNotifPanel(forceOpen) {
        var panel = document.getElementById('notifPanel');
        var overlay = document.getElementById('notifOverlay');
        if (!panel || !overlay) return;

        var shouldOpen = typeof forceOpen === 'boolean'
            ? forceOpen
            : !panel.classList.contains('active');

        panel.classList.toggle('active', shouldOpen);
        overlay.classList.toggle('active', shouldOpen);
        document.body.style.overflow = shouldOpen ? 'hidden' : '';
    }

    function resetNotifPanelState() {
        var panel = document.getElementById('notifPanel');
        var overlay = document.getElementById('notifOverlay');

        if (panel) panel.classList.remove('active');
        if (overlay) overlay.classList.remove('active');
        document.body.style.overflow = '';
    }

    function refreshNotifCount() {
        var countEl = document.querySelector('.notif-count');
        var list = document.getElementById('notifList');
        if (!countEl || !list) return;

        var count = list.querySelectorAll('.notif-item').length;
        countEl.textContent = count + (count === 1 ? ' notification' : ' notifications');

        var empty = list.querySelector('.notif-empty');
        if (count === 0) {
            if (!empty) {
                empty = document.createElement('div');
                empty.className = 'notif-empty';
                empty.innerHTML = '<i class="fas fa-bell-slash"></i><p>No notifications yet.</p>';
                list.appendChild(empty);
            }
        } else if (empty) {
            empty.remove();
        }
    }

    function updateBadge() {
        fetch('/api/notifications/unread-count')
            .then(function (res) { return res.json(); })
            .then(function (data) {
                var badge = document.getElementById('bellBadge');
                if (!badge) return;

                if (data.unreadCount > 0) {
                    badge.textContent = data.unreadCount;
                    badge.classList.remove('hidden');
                } else {
                    badge.classList.add('hidden');
                }
            })
            .catch(function () {
                /* Keep UI stable even if unread count endpoint fails. */
            });
    }

    function markNotifRead(el) {
        if (!el) return;
        var id = el.getAttribute('data-id');
        if (!id) return;

        if (el.classList.contains('unread')) {
            fetch('/api/notifications/' + id + '/read', { method: 'POST' })
                .then(function (res) { return res.json(); })
                .then(function () {
                    el.classList.remove('unread');
                    updateBadge();
                })
                .catch(function () {
                    /* No-op */
                });
        }
    }

    function markAllRead() {
        fetch('/api/notifications/mark-all-read', { method: 'POST' })
            .then(function (res) { return res.json(); })
            .then(function () {
                toArray(document.querySelectorAll('.notif-item.unread')).forEach(function (item) {
                    item.classList.remove('unread');
                });
                updateBadge();
            })
            .catch(function () {
                /* No-op */
            });
    }

    function removeNotif(event, el) {
        if (event) event.stopPropagation();
        if (!el) return;

        var id = el.getAttribute('data-id');
        if (!id) return;

        fetch('/api/notifications/' + id, { method: 'DELETE' })
            .then(function (res) { return res.json(); })
            .then(function () {
                el.remove();
                refreshNotifCount();
                updateBadge();
            })
            .catch(function () {
                /* No-op */
            });
    }

    function toggleConcern(row) {
        if (!row) return;
        var card = row.closest('.concern-card');
        if (!card) return;

        var wasExpanded = card.classList.contains('expanded');

        toArray(document.querySelectorAll('.concern-card.expanded')).forEach(function (other) {
            if (other !== card) {
                other.classList.remove('expanded');
            }
        });

        card.classList.toggle('expanded', !wasExpanded);
    }

    function toggleReplies(header) {
        if (!header) return;

        header.classList.toggle('open');
        var collapsible = header.nextElementSibling;
        if (!collapsible) return;

        if (collapsible.classList.contains('open')) {
            collapsible.style.opacity = '0';
            setTimeout(function () {
                collapsible.classList.remove('open');
                collapsible.style.opacity = '';
                collapsible.style.transition = '';
            }, 200);
        } else {
            collapsible.classList.add('open');
            collapsible.style.opacity = '0';
            requestAnimationFrame(function () {
                collapsible.style.transition = 'opacity 0.3s ease';
                collapsible.style.opacity = '1';
            });
        }
    }

    function countCharacters(text) {
        return (text || '').trim().length;
    }

    function bindNotificationInteractions() {
        var bellBtn = document.getElementById('notifBellBtn');
        var overlay = document.getElementById('notifOverlay');
        var closeBtn = document.querySelector('.notif-close');
        var markAllBtn = document.querySelector('.mark-all-btn');
        var notifList = document.getElementById('notifList');

        if (bellBtn) {
            bellBtn.addEventListener('click', function () {
                toggleNotifPanel();
            });
        }

        if (overlay) {
            overlay.addEventListener('click', function () {
                toggleNotifPanel(false);
            });
        }

        if (closeBtn) {
            closeBtn.addEventListener('click', function () {
                toggleNotifPanel(false);
            });
        }

        if (markAllBtn) {
            markAllBtn.addEventListener('click', function () {
                markAllRead();
            });
        }

        if (notifList) {
            notifList.addEventListener('click', function (event) {
                var removeBtn = event.target.closest('.notif-remove-btn');
                if (removeBtn) {
                    removeNotif(event, removeBtn.closest('.notif-item'));
                    return;
                }

                var item = event.target.closest('.notif-item');
                if (item) {
                    markNotifRead(item);
                }
            });
        }
    }

    function bindConcernInteractions() {
        toArray(document.querySelectorAll('.concern-list-row')).forEach(function (row) {
            row.addEventListener('click', function () {
                toggleConcern(row);
            });
        });

        toArray(document.querySelectorAll('.replies-header')).forEach(function (header) {
            header.addEventListener('click', function (event) {
                event.stopPropagation();
                toggleReplies(header);
            });
        });
    }

    function setupCardAnimations() {
        var cards = toArray(document.querySelectorAll('.concern-card'));
        if (cards.length === 0) return;

        if ('IntersectionObserver' in window) {
            var observer = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry, idx) {
                    if (entry.isIntersecting) {
                        entry.target.style.animationDelay = (idx * 0.08) + 's';
                        entry.target.classList.add('animate-in');
                        observer.unobserve(entry.target);
                    }
                });
            }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

            cards.forEach(function (card) {
                observer.observe(card);
            });
        } else {
            cards.forEach(function (card) {
                card.classList.add('animate-in');
            });
        }

        toArray(document.querySelectorAll('.stat-pill')).forEach(function (pill, idx) {
            pill.style.opacity = '0';
            pill.style.transform = 'translateY(20px)';
            setTimeout(function () {
                pill.style.transition = 'all 0.5s cubic-bezier(.22,1,.36,1)';
                pill.style.opacity = '1';
                pill.style.transform = 'translateY(0)';
            }, 300 + idx * 100);
        });
    }

    function setupFilterBar() {
        var filterBtns = toArray(document.querySelectorAll('.filter-btn'));
        var cards = toArray(document.querySelectorAll('.concern-card'));
        var noResults = document.getElementById('noFilterResults');
        var searchInput = document.getElementById('concernSearchInput');
        var visibleCount = document.getElementById('visibleConcernCount');
        var activeFilter = 'all';

        if (filterBtns.length === 0 || cards.length === 0) return;

        function updateVisibleCount(totalVisible) {
            if (!visibleCount) return;
            visibleCount.textContent = totalVisible + (totalVisible === 1 ? ' concern shown' : ' concerns shown');
        }

        function applyFilters() {
            var searchTerm = searchInput ? searchInput.value.trim().toLowerCase() : '';
            var visible = 0;

            cards.forEach(function (card) {
                var status = card.getAttribute('data-status') || '';
                var haystack = (card.getAttribute('data-search') || '').toLowerCase();
                var matchesFilter = activeFilter === 'all' || status === activeFilter;
                var matchesSearch = searchTerm === '' || haystack.indexOf(searchTerm) !== -1;

                if (matchesFilter && matchesSearch) {
                    card.style.display = '';
                    card.style.opacity = '1';
                    card.style.transform = 'translateY(0)';
                    card.style.pointerEvents = '';
                    visible += 1;
                } else {
                    card.style.display = 'none';
                    card.style.pointerEvents = 'none';
                }
            });

            if (noResults) {
                noResults.style.display = visible === 0 ? 'block' : 'none';
            }
            updateVisibleCount(visible);
        }

        filterBtns.forEach(function (btn) {
            btn.addEventListener('click', function () {
                filterBtns.forEach(function (b) {
                    b.classList.remove('active');
                });
                btn.classList.add('active');
                activeFilter = btn.getAttribute('data-filter') || 'all';
                applyFilters();
            });
        });

        if (searchInput) {
            searchInput.addEventListener('input', applyFilters);
        }

        applyFilters();
    }

    function setupFeedbackValidation() {
        toArray(document.querySelectorAll('.feedback-form')).forEach(function (form) {
            var ratingInputs = toArray(form.querySelectorAll('input[name="rating"]'));
            var commentField = form.querySelector('textarea[name="comments"]');
            var errorText = form.querySelector('.feedback-error-text');
            var wordCounter = form.querySelector('.feedback-word-count');
            var ratingHint = form.querySelector('.feedback-rating-hint');
            var quickTags = toArray(form.querySelectorAll('.feedback-tag-btn'));

            if (!commentField || ratingInputs.length === 0) {
                return;
            }

            var ratingMessages = {
                1: 'Very poor: please explain what should have been handled better.',
                2: 'Poor: tell us what improvement you expected from the admin response.',
                3: 'Average: mention what was okay and what can still improve.',
                4: 'Good: share what worked well in this concern handling.',
                5: 'Excellent: thank you, your positive feedback helps us keep this standard.'
            };

            function selectedRating() {
                var checked = ratingInputs.find(function (input) { return input.checked; });
                return checked ? Number(checked.value) : null;
            }

            function updateRatingHint(rating) {
                if (!ratingHint) return;
                ratingHint.textContent = ratingMessages[rating] || 'Select a rating to see guidance.';
            }

            function appendQuickTag(tagText) {
                if (!tagText) return;

                var current = commentField.value.trim();
                var lowerCurrent = current.toLowerCase();
                var lowerTag = tagText.toLowerCase();
                if (lowerCurrent.indexOf(lowerTag) !== -1) {
                    return;
                }

                var merged = current.length > 0 ? current + ' ' + tagText : tagText;
                commentField.value = merged.length > 100 ? merged.slice(0, 100) : merged;
                validateFeedbackRules();
                commentField.focus();
            }

            function validateFeedbackRules() {
                var rating = selectedRating();
                var comment = commentField.value.trim();
                var characters = countCharacters(comment);
                var hasComment = comment.length > 0;
                var errors = [];

                ratingInputs.forEach(function (input) {
                    input.setCustomValidity('');
                });

                updateRatingHint(rating);

                if (wordCounter) {
                    wordCounter.textContent = 'Characters: ' + characters;
                }

                if (rating === null) {
                    var ratingMessage = 'Please select a rating before submitting feedback.';
                    errors.push(ratingMessage);
                    ratingInputs[0].setCustomValidity(ratingMessage);
                }

                if (rating !== null && rating <= 3 && !hasComment) {
                    errors.push('Comment is required when rating is 3 stars or below.');
                }

                if (hasComment && (characters < 10 || characters > 100)) {
                    errors.push('Comment must contain between 10 and 100 characters.');
                }

                if (errors.length > 0) {
                    var message = errors.join(' ');
                    commentField.setCustomValidity(message);
                    if (errorText) {
                        errorText.textContent = message;
                        errorText.style.display = 'block';
                    }
                } else {
                    commentField.setCustomValidity('');
                    if (errorText) {
                        errorText.textContent = '';
                        errorText.style.display = 'none';
                    }
                }
            }

            ratingInputs.forEach(function (input) {
                input.addEventListener('change', validateFeedbackRules);
            });

            commentField.addEventListener('input', validateFeedbackRules);

            quickTags.forEach(function (tagBtn) {
                tagBtn.addEventListener('click', function () {
                    appendQuickTag(tagBtn.getAttribute('data-tag') || '');
                });
            });

            form.addEventListener('submit', function (event) {
                validateFeedbackRules();
                if (!form.checkValidity()) {
                    event.preventDefault();
                    form.reportValidity();
                }
            });

            validateFeedbackRules();
        });
    }

    function setupToastAutoDismiss() {
        toArray(document.querySelectorAll('.alert-toast')).forEach(function (toast) {
            setTimeout(function () {
                toast.style.transition = 'all 0.5s ease';
                toast.style.opacity = '0';
                toast.style.transform = 'translateY(-10px)';
                setTimeout(function () {
                    toast.remove();
                }, 500);
            }, 5000);
        });
    }

    function initConcernHistoryPage() {
        if (document.body.dataset.concernHistoryInit === '1') {
            return;
        }
        document.body.dataset.concernHistoryInit = '1';
        window.__historyScriptLoaded = true;

        resetNotifPanelState();
        bindNotificationInteractions();
        bindConcernInteractions();
        setupCardAnimations();
        setupFilterBar();
        setupFeedbackValidation();
        setupToastAutoDismiss();

        window.addEventListener('pageshow', function () {
            resetNotifPanelState();
        });

        setInterval(updateBadge, 30000);
    }

    // Expose for fallback compatibility.
    window.toggleNotifPanel = toggleNotifPanel;
    window.toggleConcern = toggleConcern;
    window.toggleReplies = toggleReplies;
    window.markNotifRead = markNotifRead;
    window.markAllRead = markAllRead;
    window.removeNotif = removeNotif;
    window.updateBadge = updateBadge;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initConcernHistoryPage, { once: true });
    } else {
        initConcernHistoryPage();
    }
})();
