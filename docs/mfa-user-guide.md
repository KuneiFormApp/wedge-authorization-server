# Multi-Factor Authentication (MFA) User Guide

This guide explains how to use Multi-Factor Authentication (MFA) in WedgeAuth for enhanced account security.

## Table of Contents

- [What is MFA?](#what-is-mfa)
- [Setting Up MFA](#setting-up-mfa)
- [Using MFA to Log In](#using-mfa-to-log-in)
- [Troubleshooting](#troubleshooting)
- [Security Best Practices](#security-best-practices)
- [FAQs](#faqs)

---

## What is MFA?

Multi-Factor Authentication (MFA) adds an extra layer of security to your account by requiring two forms of verification:
1. **Something you know**: Your username and password
2. **Something you have**: A time-based code from your authenticator app

Even if someone discovers your password, they cannot access your account without the code from your authenticator app.

### How It Works

WedgeAuth uses TOTP (Time-based One-Time Password), standardized in RFC 6238. Your authenticator app generates a new 6-digit code every 30 seconds that is synchronized with our servers.

---

## Setting Up MFA

### Prerequisites

You need one of these authenticator apps installed on your smartphone:
- **Google Authenticator** (Android / iOS)
- **Microsoft Authenticator** (Android / iOS)
- **Authy** (Android / iOS / Desktop)
- **1Password** (with TOTP support)
- **LastPass Authenticator**
- **Duo Mobile**

### Setup Steps

1. **Log In with Your Credentials**
   - Enter your username and password as usual
   - If MFA is enabled for your account, you'll be redirected to the MFA setup page

2. **Scan the QR Code**
   - Open your authenticator app
   - Tap **Add Account** or **Scan QR Code**
   - Point your camera at the QR code displayed on screen
   - Your app will automatically add the account

3. **Manual Entry (Alternative)**
   - If you can't scan the QR code, tap **"Enter manually"**
   - Copy the secret key displayed on screen
   - In your authenticator app, choose **Manual Entry**
   - Paste the secret key and save

4. **Verify Setup**
   - Your authenticator app will now show a 6-digit code
   - Enter this code in the **"Verification Code"** field
   - Click **Verify and Complete Setup**

5. **Setup Complete!**
   - You'll be logged in automatically
   - From now on, you'll need your authenticator app to log in

> ⚠️ **IMPORTANT**: Save your secret key in a secure location! If you lose your phone, you'll need this key to restore access to your authenticator app on a new device.

---

## Using MFA to Log In

After MFA is set up, the login process includes an additional step:

1. **Enter Username and Password**
   - Log in with your normal credentials

2. **Enter TOTP Code**
   - You'll be redirected to the MFA verification page
   - Open your authenticator app
   - Find the 6-digit code for your account
   - Enter the code in the verification field
   - The code auto-submits when you type the 6th digit

3. **Login Complete**
   - You're now logged in securely!

### Code Expiration

- Each code is valid for 30 seconds
- A new code is generated automatically every 30 seconds
- If a code expires while you're entering it, wait for the new code and try again

---

## Troubleshooting

### "Invalid Code" Error

**Possible Causes:**
1. **Time Sync Issue**: Your phone's clock is not synchronized
   - **Solution**: Enable automatic date &time in your phone settings
   - On Android: Settings → System → Date & time → Use network-provided time
   - On iOS: Settings → General → Date & Time → Set Automatically

2. **Expired Code**: The code changed while you were typing
   - **Solution**: Wait for a new code and enter it quickly

3. **Wrong Account**: You're using a code from a different account
   - **Solution**: Make sure you're looking at the correct entry in your authenticator app (look for the account name/email)

### Lost Phone / Cannot Access Authenticator App

**If you lose your device:**
1. Contact your administrator immediately
2. They can disable MFA for your account temporarily
3. You'll need to set up MFA again with a new device

**Prevent This:**
- **Back up your secret key** when you first set up MFA
- **Use cloud-backup authenticators** like Authy or Microsoft Authenticator
- **Keep your secret key in a secure location** (password manager, safe, etc.)

### QR Code Won't Scan

**Solutions:**
1. Ensure your phone camera has permission to access the camera
2. Try increasing your screen brightness
3. Try moving your phone closer or farther from the screen
4. Use the **manual entry** option instead:
   - Click "Enter manually" on the setup page
   - Copy the secret key
   - Enter it manually in your authenticator app

### Code Doesn't Auto-Submit

If the verification code doesn't automatically submit after typing 6 digits:
1. Click the **Verify** button manually
2. Ensure JavaScript is enabled in your browser
3. Try a different browser (Chrome, Firefox, Safari, Edge)

---

## Security Best Practices

### DO:
✅ **Keep your secret key secure**  
✅ **Use a reputable authenticator app** from official app stores  
✅ **Enable automatic time synchronization** on your phone  
✅ **Set up MFA on a personal device** you control  
✅ **Use cloud backup** for your authenticator (Authy, Microsoft Authenticator)  
✅ **Contact your admin immediately** if you suspect unauthorized access  

### DON'T:
❌ **Never share your TOTP codes** with anyone  
❌ **Don't email or text your secret key**  
❌ **Don't screenshot your QR code** and store it insecurely  
❌ **Don't rely solely on one device** - back up your secret key  
❌ **Don't disable automatic time sync** on your phone  

---

## FAQs

### Can I use the same authenticator app for multiple accounts?

Yes! Your authenticator app can store codes for multiple accounts (work, personal, other services). Each account will have its own entry with a unique code.

### Do I need internet access to use my authenticator app?

No! TOTP codes are generated locally on your device using the secret key and your device's clock. You don't need internet access to generate codes.

### What happens if my code expires while I'm typing it?

Simply wait a few seconds for the new code to appear in your authenticator app, then enter the new code.

### Can I use MFA on multiple devices?

Yes! You can add the same account to multiple devices:
1. During setup, scan the QR code with all devices you want to use
2. Or manually enter the secret key on each device
3. All devices will generate the same codes at the same time

### How do I transfer MFA to a new phone?

**Option 1: Cloud Backup (Recommended)**
- If using Authy or Microsoft Authenticator with cloud backup enabled, simply log in on your new device

**Option 2: Manual Transfer**
1. Before retiring your old phone, note down or screenshot all your QR codes
2. On your new phone, manually add each account using the secret key
3. Verify the codes match between old and new phone before retiring the old one

**Option 3: Re-setup**
1. Contact your administrator to temporarily disable MFA
2. Set up MFA again on your new device

### Is my secret key stored securely?

Yes! WedgeAuth and your user provider should:
- Store the secret key encrypted at rest in the database
- Only transmit it over HTTPS
- Never log or expose it after initial setup

### Can administrators bypass MFA?

Administrators can disable MFA for an account if necessary (e.g., lost device), but they cannot generate valid codes or access your account without proper authorization.

### What if I accidentally close the setup page before completing?

Just log out and log back in. You'll be redirected to the MFA setup page again to complete the process.

---

## Additional Resources

- [RFC 6238 - TOTP Specification](https://tools.ietf.org/html/rfc6238)
- [NIST Digital Identity Guidelines](https://pages.nist.gov/800-63-3/)
- [Google Authenticator Setup](https://support.google.com/accounts/answer/1066447)
- [Microsoft Authenticator Setup](https://support.microsoft.com/en-us/account-billing/download-and-install-the-microsoft-authenticator-app-351498fc-850a-45da-b7b6-27e523b8702a)
- [Authy Setup](https://authy.com/guides/)

---

## Need Help?

If you encounter issues not covered in this guide:

1. **Check Troubleshooting Section**: See if your issue is listed above
2. **Contact Your Administrator**: They can help with account-specific issues
3. **Contact IT Support**: For technical issues with the WedgeAuth system

**When contacting support, include:**
- Your username (never your password or TOTP code!)
- What step you're stuck on
- Error messages you're seeing
- Screenshots (make sure not to include sensitive information)
