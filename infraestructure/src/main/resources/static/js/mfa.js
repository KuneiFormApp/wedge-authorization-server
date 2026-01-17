// MFA JavaScript utilities

// Auto-focus and auto-submit for verification code input
document.addEventListener('DOMContentLoaded', function() {
    const codeInput = document.getElementById('code');
    
    if (codeInput) {
        // Auto-format: only allow digits
        codeInput.addEventListener('input', function(e) {
            this.value = this.value.replace(/\D/g, '');
            
            // Auto-submit when 6 digits are entered
            if (this.value.length === 6) {
                // Small delay for better UX
                setTimeout(() => {
                    const form = this.closest('form');
                    if (form) {
                        form.submit();
                    }
                }, 300);
            }
        });

        // Prevent paste of non-numeric content
        codeInput.addEventListener('paste', function(e) {
            e.preventDefault();
            const pastedText = (e.clipboardData || window.clipboardData).getData('text');
            const numericOnly = pastedText.replace(/\D/g, '').substring(0, 6);
            this.value = numericOnly;
            
            // Trigger input event to activate auto-submit if applicable
            const event = new Event('input', { bubbles: true });
            this.dispatchEvent(event);
        });
    }
});

// Copy secret to clipboard function
function copySecret() {
    const secretElement = document.getElementById('mfaSecret');
    const copyBtn = document.querySelector('.copy-btn');
    
    if (!secretElement) return;
    
    const secret = secretElement.textContent;
    
    // Modern clipboard API
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(secret).then(() => {
            showCopySuccess(copyBtn);
        }).catch(err => {
            console.error('Failed to copy:', err);
            fallbackCopy(secret, copyBtn);
        });
    } else {
        fallbackCopy(secret, copyBtn);
    }
}

// Fallback copy method for older browsers
function fallbackCopy(text, copyBtn) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    
    try {
        document.execCommand('copy');
        showCopySuccess(copyBtn);
    } catch (err) {
        console.error('Fallback copy failed:', err);
    }
    
    document.body.removeChild(textarea);
}

// Show visual feedback for successful copy
function showCopySuccess(copyBtn) {
    if (!copyBtn) return;
    
    copyBtn.classList.add('copied');
    
    // Reset after 2 seconds
    setTimeout(() => {
        copyBtn.classList.remove('copied');
    }, 2000);
}

// Form submission loading state
document.addEventListener('DOMContentLoaded', function() {
    const forms = document.querySelectorAll('.mfa-form');
    
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = this.querySelector('.btn-submit');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.style.opacity = '0.7';
                submitBtn.querySelector('.btn-text').textContent = 'Verifying...';
            }
        });
    });
});
