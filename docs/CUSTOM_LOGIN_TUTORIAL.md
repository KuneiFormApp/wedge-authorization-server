# Custom Login Page Tutorial

This tutorial shows you how to externalize and customize the WedgeAuth login page without modifying the application code. You can create your own branded login experience by providing custom templates, styles, and translations.

## Table of Contents

- [Quick Start](#quick-start)
- [Step-by-Step Guide](#step-by-step-guide)
- [Customization Examples](#customization-examples)
- [Advanced Topics](#advanced-topics)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

**Goal**: Create a custom login page in 5 minutes.

1. **Create external directory structure**:
   ```
   C:/wedge-custom/
   ├── templates/
   │   └── login.html
   ├── static/
   │   ├── css/
   │   │   └── login.css
   │   └── js/
   │       └── login.js
   └── i18n/
       ├── messages_en.properties
       └── messages_es.properties
   ```

2. **Configure WedgeAuth** (`application.yaml`):
   ```yaml
   wedge:
     frontend:
       templates-path: file:///C:/wedge-custom/templates
       static-path: file:///C:/wedge-custom/static
       i18n-basename: file:///C:/wedge-custom/i18n/messages
   ```

3. **Copy default files** from `infraestructure/src/main/resources/` to your custom directory

4. **Customize** the files to match your brand

5. **Restart** WedgeAuth - your custom login page is now active!

---

## Step-by-Step Guide

### Step 1: Create Directory Structure

Create a directory to store your custom login page files. This can be anywhere on your system.

**Windows**:
```powershell
New-Item -ItemType Directory -Force -Path "C:\wedge-custom\templates"
New-Item -ItemType Directory -Force -Path "C:\wedge-custom\static\css"
New-Item -ItemType Directory -Force -Path "C:\wedge-custom\static\js"
New-Item -ItemType Directory -Force -Path "C:\wedge-custom\i18n"
```

**Linux/Mac**:
```bash
mkdir -p /opt/wedge-custom/{templates,static/{css,js},i18n}
```

---

### Step 2: Copy Default Files

Copy the default login page files from the WedgeAuth installation to your custom directory:

**From** (WedgeAuth installation):
- `infraestructure/src/main/resources/templates/login.html`
- `infraestructure/src/main/resources/static/css/login.css`
- `infraestructure/src/main/resources/static/js/login.js`
- `infraestructure/src/main/resources/i18n/messages_en.properties`
- `infraestructure/src/main/resources/i18n/messages_es.properties`

**To** (Your custom directory):
- `C:/wedge-custom/templates/login.html`
- `C:/wedge-custom/static/css/login.css`
- `C:/wedge-custom/static/js/login.js`
- `C:/wedge-custom/i18n/messages_en.properties`
- `C:/wedge-custom/i18n/messages_es.properties`

---

### Step 3: Configure WedgeAuth

Edit your `application.yaml` (or use environment variables) to point to your custom directory:

```yaml
wedge:
  frontend:
    # Point to your custom templates directory
    templates-path: file:///C:/wedge-custom/templates
    
    # Point to your custom static resources directory
    static-path: file:///C:/wedge-custom/static
    
    # Point to your custom i18n messages (without .properties extension)
    i18n-basename: file:///C:/wedge-custom/i18n/messages
    
    # Set default language
    default-locale: en
    
    # Supported languages
    supported-locales:
      - en
      - es
```

**Using Environment Variables** (alternative to YAML):
```bash
export WEDGE_FRONTEND_TEMPLATES_PATH=file:///C:/wedge-custom/templates
export WEDGE_FRONTEND_STATIC_PATH=file:///C:/wedge-custom/static
export WEDGE_FRONTEND_I18N_BASENAME=file:///C:/wedge-custom/i18n/messages
```

---

### Step 4: Customize Your Login Page

Now you can edit the files in your custom directory to match your brand!

#### Customize HTML Template

Edit `C:/wedge-custom/templates/login.html`:

```html
<!-- Change the logo and branding -->
<h1 class="logo-text">YourCompany Auth</h1>

<!-- Customize the subtitle -->
<p class="subtitle" th:text="#{login.subtitle}">Welcome to YourCompany</p>

<!-- Add your company logo -->
<img src="/images/company-logo.png" alt="Company Logo" class="company-logo">
```

#### Customize CSS Styles

Edit `C:/wedge-custom/static/css/login.css`:

```css
/* Change color scheme */
:root {
  --primary-color: #ff6b6b;  /* Your brand color */
  --secondary-color: #4ecdc4;
  --background: #1a1a2e;
}

/* Customize the login card */
.login-card {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  border-radius: 20px;
}

/* Add your company logo styling */
.company-logo {
  width: 150px;
  margin-bottom: 20px;
}
```

#### Customize Translations

Edit `C:/wedge-custom/i18n/messages_en.properties`:

```properties
# Customize English messages
login.title=YourCompany - Sign In
login.subtitle=Welcome to YourCompany Portal
login.footer.secured=Powered by YourCompany
```

Edit `C:/wedge-custom/i18n/messages_es.properties`:

```properties
# Customize Spanish messages
login.title=YourCompany - Iniciar Sesión
login.subtitle=Bienvenido al Portal de YourCompany
login.footer.secured=Desarrollado por YourCompany
```

---

### Step 5: Test Your Changes

1. **Restart WedgeAuth**:
   ```bash
   ./gradlew :infraestructure:bootRun
   ```

2. **Access the login page**:
   ```
   http://localhost:9001/login
   ```

3. **Verify customizations**:
   - Check that your branding appears
   - Test different languages: `http://localhost:9001/login?lang=es`
   - Verify error messages: `http://localhost:9001/login?error=true`

---

## Customization Examples

### Example 1: Corporate Branding

**Goal**: Match your company's brand guidelines.

**login.html** - Add company logo:
```html
<div class="login-header">
  <img src="/images/acme-logo.svg" alt="ACME Corp" class="brand-logo">
  <h1 class="logo-text">ACME Authentication</h1>
  <p class="subtitle" th:text="#{login.subtitle}">Secure Access Portal</p>
</div>
```

**login.css** - Brand colors:
```css
:root {
  --acme-blue: #0066cc;
  --acme-gray: #333333;
  --acme-light: #f5f5f5;
}

.login-card {
  background: var(--acme-light);
  border: 2px solid var(--acme-blue);
}

.btn-submit {
  background: var(--acme-blue);
}

.btn-submit:hover {
  background: #0052a3;
}
```

---

### Example 2: Dark Mode Theme

**login.css** - Dark theme:
```css
:root {
  --bg-dark: #0f0f23;
  --card-dark: #1a1a2e;
  --text-light: #e0e0e0;
  --accent: #00d9ff;
}

body {
  background: var(--bg-dark);
  color: var(--text-light);
}

.login-card {
  background: var(--card-dark);
  box-shadow: 0 8px 32px rgba(0, 217, 255, 0.2);
}

.form-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 217, 255, 0.3);
  color: var(--text-light);
}

.btn-submit {
  background: linear-gradient(135deg, var(--accent), #0099cc);
}
```

---

### Example 3: Minimalist Design

**login.html** - Simplified layout:
```html
<div class="login-card minimal">
  <h1>Sign In</h1>
  
  <form th:action="@{/login}" method="post" class="minimal-form">
    <input type="text" name="username" th:placeholder="#{login.username}" required>
    <input type="password" name="password" th:placeholder="#{login.password}" required>
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <button type="submit" th:text="#{login.submit}">Sign In</button>
  </form>
</div>
```

**login.css** - Clean styling:
```css
.login-card.minimal {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  max-width: 400px;
}

.minimal-form input {
  width: 100%;
  padding: 12px;
  margin: 10px 0;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.minimal-form button {
  width: 100%;
  padding: 12px;
  background: #000;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
```

---

### Example 4: Multi-Language Support

Add French support:

**Step 1**: Create `messages_fr.properties`:
```properties
login.title=YourCompany - Connexion
login.subtitle=Bienvenue sur le portail YourCompany
login.username=Nom d'utilisateur
login.password=Mot de passe
login.username.placeholder=Entrez votre nom d'utilisateur
login.password.placeholder=Entrez votre mot de passe
login.remember=Se souvenir de moi
login.submit=Se connecter
login.error=Nom d'utilisateur ou mot de passe invalide
login.logout.success=Vous avez été déconnecté avec succès
login.footer.secured=Sécurisé par
login.footer.version=OAuth 2.1 / OpenID Connect
```

**Step 2**: Update `application.yaml`:
```yaml
wedge:
  frontend:
    supported-locales:
      - en
      - es
      - fr
```

**Step 3**: Access French version:
```
http://localhost:9001/login?lang=fr
```

---

## Advanced Topics

### Dynamic Branding per Client

You can customize the login page based on the OAuth client by using query parameters:

**login.html**:
```html
<!-- Show different logos based on client_id -->
<img th:if="${param.client_id != null and param.client_id[0] == 'mobile-app'}" 
     src="/images/mobile-logo.png" alt="Mobile App">
<img th:if="${param.client_id != null and param.client_id[0] == 'web-app'}" 
     src="/images/web-logo.png" alt="Web App">
```

---

### Custom JavaScript Behavior

Edit `C:/wedge-custom/static/js/login.js`:

```javascript
// Add custom validation
document.getElementById('loginForm').addEventListener('submit', function(e) {
  const username = document.getElementById('username').value;
  
  // Custom validation: username must be email format
  if (!username.includes('@')) {
    e.preventDefault();
    alert('Please enter a valid email address');
    return false;
  }
});

// Add analytics tracking
document.getElementById('submitBtn').addEventListener('click', function() {
  // Track login attempts (replace with your analytics)
  console.log('Login attempt tracked');
});
```

---

### Adding Social Login Buttons

**login.html** - Add social login section:
```html
<div class="social-login">
  <p class="divider"><span>Or sign in with</span></p>
  
  <div class="social-buttons">
    <a href="/oauth2/authorization/google" class="social-btn google">
      <img src="/images/google-icon.svg" alt="Google">
      Google
    </a>
    
    <a href="/oauth2/authorization/github" class="social-btn github">
      <img src="/images/github-icon.svg" alt="GitHub">
      GitHub
    </a>
  </div>
</div>
```

**login.css** - Style social buttons:
```css
.social-login {
  margin-top: 30px;
}

.divider {
  text-align: center;
  position: relative;
  margin: 20px 0;
}

.divider span {
  background: white;
  padding: 0 10px;
  color: #666;
}

.social-buttons {
  display: flex;
  gap: 10px;
}

.social-btn {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-decoration: none;
  color: #333;
}

.social-btn:hover {
  background: #f5f5f5;
}
```

---

## Troubleshooting

### Issue: Changes not appearing

**Solution**: 
1. Clear browser cache (Ctrl+Shift+R)
2. Verify file paths in `application.yaml` are correct
3. Check file permissions (files must be readable)
4. Restart WedgeAuth application

---

### Issue: CSS not loading

**Solution**:
1. Verify static path configuration: `static-path: file:///C:/wedge-custom/static`
2. Ensure CSS file is in correct location: `C:/wedge-custom/static/css/login.css`
3. Check browser console for 404 errors
4. Verify Thymeleaf link syntax: `th:href="@{/css/login.css}"`

---

### Issue: Translations not working

**Solution**:
1. Verify i18n basename: `i18n-basename: file:///C:/wedge-custom/i18n/messages`
2. Check file naming: `messages_en.properties`, `messages_es.properties`
3. Ensure UTF-8 encoding for special characters
4. Test with query parameter: `?lang=es`

---

### Issue: Template not found

**Solution**:
1. Verify templates path: `templates-path: file:///C:/wedge-custom/templates`
2. Ensure file exists: `C:/wedge-custom/templates/login.html`
3. Check file name is exactly `login.html` (case-sensitive on Linux)
4. Verify file permissions

---

### Issue: Windows path issues

**Solution**:
Use forward slashes even on Windows:
```yaml
# ✅ Correct
templates-path: file:///C:/wedge-custom/templates

# ❌ Incorrect
templates-path: file:///C:\wedge-custom\templates
```

---

## Best Practices

### 1. Version Control

Keep your custom login page in version control:

```bash
cd C:/wedge-custom
git init
git add .
git commit -m "Initial custom login page"
```

### 2. Environment-Specific Configuration

Use different customizations per environment:

**Development** (`application-dev.yaml`):
```yaml
wedge:
  frontend:
    templates-path: file:///C:/dev/wedge-custom/templates
```

**Production** (`application-prod.yaml`):
```yaml
wedge:
  frontend:
    templates-path: file:///opt/wedge-custom/templates
```

### 3. Backup Default Files

Always keep a copy of the original files before customizing.

### 4. Test All Languages

After customization, test all supported languages:
- `http://localhost:9001/login?lang=en`
- `http://localhost:9001/login?lang=es`
- `http://localhost:9001/login?lang=fr`

### 5. Mobile Responsive

Ensure your custom design works on mobile devices:

```css
@media (max-width: 768px) {
  .login-card {
    width: 95%;
    padding: 20px;
  }
}
```

---

## Complete Example

Here's a complete example of a custom branded login page:

### Directory Structure
```
/opt/acme-auth/
├── templates/
│   └── login.html
├── static/
│   ├── css/
│   │   └── login.css
│   ├── js/
│   │   └── login.js
│   └── images/
│       ├── acme-logo.svg
│       └── background.jpg
└── i18n/
    ├── messages_en.properties
    └── messages_es.properties
```

### Configuration
```yaml
wedge:
  frontend:
    templates-path: file:///opt/acme-auth/templates
    static-path: file:///opt/acme-auth/static
    i18n-basename: file:///opt/acme-auth/i18n/messages
    default-locale: en
    supported-locales:
      - en
      - es
```

### Result
- Custom ACME branding
- Company logo and colors
- Bilingual support (English/Spanish)
- Responsive design
- Custom background image

---

## Next Steps

- **Customize further**: Add animations, transitions, or interactive elements
- **Add more languages**: Create additional `.properties` files
- **Integrate analytics**: Track login attempts and user behavior
- **A/B testing**: Create multiple versions and test which performs better
- **Accessibility**: Ensure your custom page meets WCAG guidelines

---

## Resources

- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [i18n Best Practices](https://www.w3.org/International/questions/qa-i18n)
- [OAuth 2.1 Specification](https://oauth.net/2.1/)

---

## Support

If you encounter issues:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review WedgeAuth logs for error messages
3. Verify file paths and permissions
4. Test with default configuration first

Happy customizing! 🎨
