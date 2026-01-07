// Login page interactions and validation
document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const usernameError = document.getElementById('usernameError');
    const passwordError = document.getElementById('passwordError');
    const togglePassword = document.getElementById('togglePassword');
    const submitBtn = document.getElementById('submitBtn');

    // Password visibility toggle
    if (togglePassword && passwordInput) {
        togglePassword.addEventListener('click', function () {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);

            // Update icon
            const eyeIcon = document.getElementById('eyeIcon');
            if (type === 'password') {
                eyeIcon.innerHTML = `
                    <path d="M1 12C1 12 5 4 12 4C19 4 23 12 23 12C23 12 19 20 12 20C5 20 1 12 1 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                `;
            } else {
                eyeIcon.innerHTML = `
                    <path d="M17.94 17.94C16.2306 19.243 14.1491 19.9649 12 20C5 20 1 12 1 12C2.24389 9.68192 3.96914 7.65663 6.06 6.06M9.9 4.24C10.5883 4.0789 11.2931 3.99834 12 4C19 4 23 12 23 12C22.393 13.1356 21.6691 14.2048 20.84 15.19M14.12 14.12C13.8454 14.4148 13.5141 14.6512 13.1462 14.8151C12.7782 14.9791 12.3809 15.0673 11.9781 15.0744C11.5753 15.0815 11.1752 15.0074 10.8016 14.8565C10.4281 14.7056 10.0887 14.4811 9.80385 14.1962C9.51897 13.9113 9.29439 13.5719 9.14351 13.1984C8.99262 12.8248 8.91853 12.4247 8.92563 12.0219C8.93274 11.6191 9.02091 11.2218 9.18488 10.8538C9.34884 10.4858 9.58525 10.1546 9.88 9.88" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                `;
            }
        });
    }

    // Form validation
    if (loginForm) {
        loginForm.addEventListener('submit', function (e) {
            let isValid = true;

            // Clear previous errors
            usernameError.textContent = '';
            passwordError.textContent = '';

            // Validate username
            if (!usernameInput.value.trim()) {
                usernameError.textContent = 'Username is required';
                isValid = false;
            } else if (usernameInput.value.trim().length < 2) {
                usernameError.textContent = 'Username must be at least 2 characters';
                isValid = false;
            }

            // Validate password
            if (!passwordInput.value) {
                passwordError.textContent = 'Password is required';
                isValid = false;
            } else if (passwordInput.value.length < 3) {
                passwordError.textContent = 'Password must be at least 3 characters';
                isValid = false;
            }

            if (!isValid) {
                e.preventDefault();
                return false;
            }

            // Add loading state to button
            submitBtn.disabled = true;
            submitBtn.innerHTML = `
                <svg class="spinner" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="width: 20px; height: 20px; animation: spin 1s linear infinite;">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" opacity="0.25"/>
                    <path d="M12 2C6.47715 2 2 6.47715 2 12" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                </svg>
                <span>Signing in...</span>
            `;

            // Add spinner animation
            const style = document.createElement('style');
            style.textContent = `
                @keyframes spin {
                    to { transform: rotate(360deg); }
                }
            `;
            document.head.appendChild(style);
        });

        // Real-time validation feedback
        usernameInput.addEventListener('blur', function () {
            if (!this.value.trim()) {
                usernameError.textContent = 'Username is required';
            } else if (this.value.trim().length < 2) {
                usernameError.textContent = 'Username must be at least 2 characters';
            } else {
                usernameError.textContent = '';
            }
        });

        passwordInput.addEventListener('blur', function () {
            if (!this.value) {
                passwordError.textContent = 'Password is required';
            } else if (this.value.length < 3) {
                passwordError.textContent = 'Password must be at least 3 characters';
            } else {
                passwordError.textContent = '';
            }
        });

        // Clear errors on input
        usernameInput.addEventListener('input', function () {
            if (usernameError.textContent && this.value.trim().length >= 2) {
                usernameError.textContent = '';
            }
        });

        passwordInput.addEventListener('input', function () {
            if (passwordError.textContent && this.value.length >= 3) {
                passwordError.textContent = '';
            }
        });
    }

    // Add smooth focus transitions
    const inputs = document.querySelectorAll('.form-input');
    inputs.forEach(input => {
        input.addEventListener('focus', function () {
            this.parentElement.style.transform = 'translateY(-2px)';
            this.parentElement.style.transition = 'transform 0.3s ease';
        });

        input.addEventListener('blur', function () {
            this.parentElement.style.transform = 'translateY(0)';
        });
    });

    // Keyboard shortcuts
    document.addEventListener('keydown', function (e) {
        // Alt + U focuses username
        if (e.altKey && e.key === 'u') {
            e.preventDefault();
            usernameInput.focus();
        }
        // Alt + P focuses password
        if (e.altKey && e.key === 'p') {
            e.preventDefault();
            passwordInput.focus();
        }
    });
});

// Language selector function
function changeLanguage(lang) {
    const url = new URL(window.location);
    url.searchParams.set('lang', lang);
    window.location.href = url.toString();
}
