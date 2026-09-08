(function () {
    var theme = localStorage.getItem('canchasya_theme') || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    document.documentElement.setAttribute('data-bs-theme', theme);

    function setupThemeToggle() {
        if (document.getElementById('themeToggleBtn')) return;

        var currentTheme = document.documentElement.getAttribute('data-bs-theme') || 'light';

        var btn = document.createElement('button');
        btn.id = 'themeToggleBtn';
        btn.type = 'button';
        btn.className = 'theme-toggle-btn shadow-sm';
        btn.setAttribute('aria-label', 'Modo oscuro');
        btn.setAttribute('title', 'Modo oscuro');
        btn.innerHTML = '<i class="' + (currentTheme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill') + '"></i>';

        btn.addEventListener('click', function () {
            var curr = document.documentElement.getAttribute('data-bs-theme') || 'light';
            var next = curr === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-bs-theme', next);
            localStorage.setItem('canchasya_theme', next);
            var icon = btn.querySelector('i');
            if (icon) {
                icon.className = next === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill';
            }
        });

        var navUserArea = document.querySelector('.navbar .d-flex.align-items-center');
        if (navUserArea) {
            navUserArea.appendChild(btn);
        } else {
            var navContainer = document.querySelector('.navbar .container, .navbar .container-fluid');
            if (navContainer) {
                btn.classList.add('ms-auto', 'me-2');
                navContainer.appendChild(btn);
            } else {
                btn.classList.add('theme-toggle-floating');
                document.body.appendChild(btn);
            }
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setupThemeToggle);
    } else {
        setupThemeToggle();
    }
})();
