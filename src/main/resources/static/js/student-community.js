(function () {
    function getModerationEndpoint() {
        if (window.location.pathname.startsWith('/admin/community')) {
            return '/admin/community/moderate';
        }
        return '/student/community/moderate';
    }

    function debounce(fn, delay) {
        let timer;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    async function callModeration(text, contentType) {
        const response = await fetch(getModerationEndpoint(), {
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
        let moderationRequestId = 0;

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

            const requestId = ++moderationRequestId;

            const result = await callModeration(value, contentType);
            if (requestId !== moderationRequestId) {
                return;
            }

            if (!hint) {
                return;
            }

            const normalized = (result.decision || 'ALLOW').toLowerCase();
            hint.className = 'moderation-hint ' + normalized;
            
            if (normalized === 'allow' && result.correctedText && result.correctedText !== value) {
                const cursorStart = textarea.selectionStart;
                const cursorEnd = textarea.selectionEnd;
                const diff = result.correctedText.length - value.length;
                
                textarea.value = result.correctedText;
                
                try {
                    textarea.setSelectionRange(
                        Math.max(0, cursorStart + diff),
                        Math.max(0, cursorEnd + diff)
                    );
                } catch(e) {}
                
                hint.textContent = 'Autocorrected spelling.';
            } else {
                hint.textContent = result.reason || 'Moderation checked.';
            }

            if (submitBtn) {
                submitBtn.disabled = normalized !== 'allow';
            }
        }, 800);

        textarea.addEventListener('input', function () {
            run();
        });

    }

    function toggleById(id) {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }
        element.style.display = element.style.display === 'none' || element.style.display === '' ? 'block' : 'none';
    }

    function toBool(value) {
        return String(value).toLowerCase() === 'true';
    }

    function bindThreadFilters() {
        const searchInput = document.getElementById('communitySearchInput');
        const filterSelect = document.getElementById('communityFilterSelect');
        const resetBtn = document.getElementById('communityFilterReset');
        const noResultsCard = document.getElementById('communityNoResults');

        if (!searchInput || !filterSelect || !resetBtn) {
            return;
        }

        const posts = Array.from(document.querySelectorAll('.post-thread[data-post-id]'));
        if (posts.length === 0) {
            return;
        }

        const jumpLinks = new Map();
        document.querySelectorAll('.thread-jump-link[data-jump-post]').forEach(function (link) {
            jumpLinks.set(link.dataset.jumpPost, link);
        });

        posts.forEach(function (post) {
            const title = post.dataset.postTitle || '';
            const message = post.dataset.postMessage || '';
            const author = post.dataset.postAuthor || '';
            const repliesText = Array.from(post.querySelectorAll('.reply-text'))
                .map(function (el) { return el.textContent || ''; })
                .join(' ');

            post.dataset.searchBlob = (title + ' ' + message + ' ' + author + ' ' + repliesText).toLowerCase();
        });

        function updateScanCards(visiblePosts) {
            const visibleThreadCount = document.getElementById('visibleThreadCount');
            const unansweredThreadCount = document.getElementById('unansweredThreadCount');
            const activeThreadCount = document.getElementById('activeThreadCount');

            const unanswered = visiblePosts.filter(function (post) {
                return Number(post.dataset.replies || 0) === 0;
            }).length;

            const active = visiblePosts.filter(function (post) {
                return Number(post.dataset.replies || 0) >= 2;
            }).length;

            if (visibleThreadCount) {
                visibleThreadCount.textContent = String(visiblePosts.length);
            }

            if (unansweredThreadCount) {
                unansweredThreadCount.textContent = String(unanswered);
            }

            if (activeThreadCount) {
                activeThreadCount.textContent = String(active);
            }
        }

        function applyFilters() {
            const searchText = (searchInput.value || '').trim().toLowerCase();
            const mode = filterSelect.value || 'all';

            const visiblePosts = [];

            posts.forEach(function (post) {
                const isOwned = toBool(post.dataset.owned);
                const replies = Number(post.dataset.replies || 0);

                let modePass = true;
                if (mode === 'mine') {
                    modePass = isOwned;
                } else if (mode === 'peer') {
                    modePass = !isOwned;
                } else if (mode === 'unanswered') {
                    modePass = replies === 0;
                } else if (mode === 'active') {
                    modePass = replies >= 2;
                }

                const searchPass = !searchText || (post.dataset.searchBlob || '').indexOf(searchText) !== -1;
                const shouldShow = modePass && searchPass;

                post.style.display = shouldShow ? '' : 'none';

                const jumpLink = jumpLinks.get(post.dataset.postId);
                if (jumpLink) {
                    jumpLink.style.display = shouldShow ? '' : 'none';
                }

                if (shouldShow) {
                    visiblePosts.push(post);
                }
            });

            if (noResultsCard) {
                noResultsCard.classList.toggle('hidden', visiblePosts.length !== 0);
            }

            updateScanCards(visiblePosts);
        }

        searchInput.addEventListener('input', applyFilters);
        filterSelect.addEventListener('change', applyFilters);
        resetBtn.addEventListener('click', function () {
            searchInput.value = '';
            filterSelect.value = 'all';
            applyFilters();
            searchInput.focus();
        });

        applyFilters();
    }

    function bindReplyVisibilityToggles() {
        document.querySelectorAll('[data-reply-toggle]').forEach(function (button) {
            const targetId = button.getAttribute('data-reply-toggle');
            const target = targetId ? document.getElementById(targetId) : null;
            if (!target) {
                return;
            }

            button.addEventListener('click', function () {
                const collapsed = target.classList.toggle('collapsed');
                button.textContent = collapsed ? 'Expand' : 'Collapse';
            });
        });
    }

    function bindThreadJumpFocus() {
        document.querySelectorAll('.thread-jump-link[data-jump-post]').forEach(function (link) {
            link.addEventListener('click', function () {
                const postId = this.dataset.jumpPost;
                if (!postId) {
                    return;
                }

                window.setTimeout(function () {
                    const target = document.getElementById('post-' + postId);
                    if (!target) {
                        return;
                    }

                    target.classList.add('focus-thread');
                    window.setTimeout(function () {
                        target.classList.remove('focus-thread');
                    }, 1800);
                }, 80);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('textarea[data-mod-type]').forEach(bindModerationForField);

        document.querySelectorAll('[data-toggle-id]').forEach(btn => {
            btn.addEventListener('click', function () {
                toggleById(this.dataset.toggleId);
            });
        });

        bindThreadFilters();
        bindReplyVisibilityToggles();
        bindThreadJumpFocus();
    });
})();
