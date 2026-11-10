/* Shared drawer and scrolling behavior for every panel route. */
(function () {
    'use strict';
    var sidebar = document.getElementById('sidebar');
    var toggle = document.getElementById('sidebarToggle');
    var close = document.getElementById('sidebarClose');
    var backdrop = document.getElementById('sidebarBackdrop');
    var shell = document.getElementById('appShell');
    var main = shell.querySelector('.main');
    var compact = window.matchMedia('(max-width: 1024px)');
    var opened = false;

    function setOpen(value, restoreFocus) {
        opened = !!value && compact.matches && !shell.hidden;
        document.body.classList.toggle('sidebar-open', opened);
        toggle.setAttribute('aria-expanded', String(opened));
        backdrop.hidden = !opened;
        if (opened) {
            main.setAttribute('inert', '');
            sidebar.setAttribute('role', 'dialog');
            sidebar.setAttribute('aria-modal', 'true');
            sidebar.setAttribute('aria-label', 'Menu điều hướng');
            close.focus();
        } else {
            main.removeAttribute('inert');
            sidebar.removeAttribute('role');
            sidebar.removeAttribute('aria-modal');
            sidebar.removeAttribute('aria-label');
            if (restoreFocus && compact.matches && !shell.hidden) toggle.focus();
        }
    }
    toggle.addEventListener('click', function () { setOpen(!opened, true); });
    close.addEventListener('click', function () { setOpen(false, true); });
    backdrop.addEventListener('click', function () { setOpen(false, true); });
    sidebar.addEventListener('click', function (event) {
        if (event.target.closest('[data-route], .sidebar__footer a, #btnLogout')) setOpen(false, true);
    });
    window.addEventListener('hashchange', function () { if (opened) setOpen(false, true); });
    function resize() {
        var focusedInside = sidebar.contains(document.activeElement);
        setOpen(false, focusedInside);
    }
    if (compact.addEventListener) compact.addEventListener('change', resize);
    else compact.addListener(resize);
    document.addEventListener('keydown', function (event) {
        if (!opened) return;
        if (event.key === 'Escape') { event.preventDefault(); setOpen(false, true); return; }
        if (event.key !== 'Tab') return;
        var buttons = Array.prototype.filter.call(sidebar.querySelectorAll('button:not(:disabled), a[href], [tabindex="0"]'), function (el) {
            return el.getClientRects().length && getComputedStyle(el).visibility !== 'hidden';
        });
        var first = buttons[0], last = buttons[buttons.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    });
    new MutationObserver(function () { if (shell.hidden) setOpen(false, false); })
        .observe(shell, { attributes: true, attributeFilter: ['hidden'] });

    // Keep long modal forms scrollable while the page underneath stays in place.
    function updateOverlay() {
        document.body.classList.toggle('panel-overlay-open', !!document.querySelector('.modal:not([hidden]), dialog[open]'));
    }
    new MutationObserver(updateOverlay).observe(document.body, {
        subtree: true, attributes: true, attributeFilter: ['hidden', 'open']
    });
    document.querySelectorAll('.table-wrap, .spawn-wrap').forEach(function (wrap) {
        wrap.tabIndex = 0;
        wrap.setAttribute('role', 'region');
        wrap.setAttribute('aria-label', 'Bảng dữ liệu, có thể cuộn ngang để xem thêm');
    });
    updateOverlay();
})();
