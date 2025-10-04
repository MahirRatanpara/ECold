# 📧 Gmail App Password Setup - Detailed Guide

## Why Do You Need an App Password?

When you use Gmail's SMTP to send emails from applications like ECold, Google requires an **App Password** instead of your regular Gmail password for security reasons. This is a 16-character password that Google generates specifically for third-party applications.

---

## 🔐 Step-by-Step Gmail App Password Setup

### Prerequisites ⚠️
- You must have **2-Factor Authentication (2FA)** enabled on your Gmail account
- You need access to your phone for 2FA verification

---

### Step 1: Enable 2-Factor Authentication (If Not Already Enabled)

1. **Open your web browser** and go to [https://myaccount.google.com/](https://myaccount.google.com/)

2. **Sign in** to your Gmail account if not already signed in

3. **Click on "Security"** in the left sidebar
   ```
   📱 Personal info
   🔒 Security          ← Click here
   🔒 Privacy & personalization
   ```

4. **Look for "2-Step Verification"** section
   - If it says "Off" → Click on it to enable
   - If it says "On" → You're ready for the next step

5. **If enabling 2FA for the first time:**
   - Click "Get started"
   - Enter your phone number
   - Choose SMS or Voice call
   - Enter the verification code you receive
   - Click "Turn on"

---

### Step 2: Generate App Password (Detailed Steps)

#### 2.1: Navigate to Security Settings
1. **Stay on** [https://myaccount.google.com/](https://myaccount.google.com/)
2. **Click "Security"** in the left sidebar (if not already there)

#### 2.2: Find App Passwords Section
3. **Scroll down** to the "Signing in to Google" section
4. **Look for "App passwords"** option
   ```
   🔒 Signing in to Google
   │
   ├── Password
   ├── 2-Step Verification: On ✓
   └── App passwords          ← Click here
   ```

**⚠️ Important Notes:**
- **If you don't see "App passwords"**: 2FA must be enabled first
- **If it's grayed out**: You might be using a work/school account with restrictions

#### 2.3: Access App Passwords
5. **Click on "App passwords"**
6. **You might be prompted to sign in again** for security - enter your Gmail password
7. **Complete 2FA verification** if prompted (SMS/Authenticator app)

#### 2.4: Generate the App Password
8. **You'll see the App passwords page** with dropdown menus:
   ```
   Generate app password
   
   Select app:     [Dropdown ▼]
   Select device:  [Dropdown ▼]
                   
   [GENERATE] button
   ```

9. **For "Select app" dropdown:**
   - Click the dropdown
   - Look for "Mail" option
   - **Click "Mail"**

10. **For "Select device" dropdown:**
    - Click the dropdown  
    - Look for "Other (Custom name)" at the bottom
    - **Click "Other (Custom name)"**

11. **Enter custom name:**
    - A text box will appear
    - **Type: "ECold Application"** (or any name you prefer)
    - **Click "GENERATE"**

#### 2.5: Copy Your App Password
12. **Google will display a 16-character password** like this:
    ```
    ┌─────────────────────────────────────────┐
    │  Your app password for ECold Application │
    │                                         │
    │       abcd efgh ijkl mnop              │
    │                                         │
    │  [Copy] [Done]                         │
    └─────────────────────────────────────────┘
    ```

13. **IMPORTANT: Copy this password immediately!**
    - Click the "Copy" button, OR
    - Select all text and copy manually
    - **This password will NEVER be shown again**

14. **Click "Done"** after copying

---

## 🔧 Using the App Password in ECold

### Method 1: Direct Configuration
1. **Open** your ECold backend folder
2. **Navigate to:** `backend/src/main/resources/application.properties`
3. **Add these lines:**
   ```properties
   # Gmail SMTP Configuration
   app.email.enabled=true
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your.email@gmail.com
   spring.mail.password=abcdefghijklmnop
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   app.email.from-name=ECold Application
   ```

4. **Replace:**
   - `your.email@gmail.com` → Your actual Gmail address
   - `abcdefghijklmnop` → Your 16-character app password (no spaces!)

### Method 2: Environment Variables (Recommended for Security)
1. **Set environment variables:**
   ```bash
   # Windows (Command Prompt)
   set SPRING_MAIL_USERNAME=your.email@gmail.com
   set SPRING_MAIL_PASSWORD=abcdefghijklmnop

   # Windows (PowerShell)
   $env:SPRING_MAIL_USERNAME="your.email@gmail.com"
   $env:SPRING_MAIL_PASSWORD="abcdefghijklmnop"

   # Linux/Mac
   export SPRING_MAIL_USERNAME=your.email@gmail.com
   export SPRING_MAIL_PASSWORD=abcdefghijklmnop
   ```

2. **Update application.properties:**
   ```properties
   spring.mail.username=${SPRING_MAIL_USERNAME}
   spring.mail.password=${SPRING_MAIL_PASSWORD}
   ```

---

## ❓ Common Issues & Solutions

### "I don't see App passwords option"
**Causes & Solutions:**
- **2FA not enabled** → Enable 2-Step Verification first
- **Work/School account** → Contact your admin or use personal Gmail
- **Account too new** → Wait 24 hours after enabling 2FA

### "App password not working"
**Solutions:**
- **Check for typos** → App password is case-sensitive
- **Remove spaces** → Password should be 16 characters with no spaces
- **Use full Gmail address** → Include @gmail.com
- **Check SMTP settings** → Verify host is smtp.gmail.com, port 587

### "Authentication failed"
**Solutions:**
- **Don't use your regular Gmail password** → Must use App Password
- **Check username** → Must be your full email address
- **Verify 2FA is working** → Try logging into Gmail normally first

### "Less secure app access"
**Note:** This is **NOT** the same as App Passwords
- Google deprecated "Less secure app access"
- **You MUST use App Passwords** for new applications
- Don't try to enable "Less secure apps" - it won't work

---

## 🛡️ Security Best Practices

### 1. Treat App Passwords Like Regular Passwords
- **Don't share** your app password
- **Don't commit** to code repositories
- **Store securely** in environment variables

### 2. Manage Your App Passwords
- **View existing passwords:** Go back to App passwords page
- **Revoke unused passwords:** Remove old/unused app passwords
- **Use descriptive names:** "ECold App", "My Email Tool", etc.

### 3. Monitor Account Security
- **Check recent activity:** Google Account → Security → Recent security activity
- **Review app passwords:** Periodically clean up unused passwords

---

## 🧪 Testing Your Setup

### 1. Quick Test via Browser
Open this URL in your browser (replace with your details):
```
http://localhost:8080/api/emails/config/status
```

### 2. Send Test Email via API
```bash
curl -X POST "http://localhost:8080/api/emails/test?toEmail=test@example.com" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Test from ECold Application
1. **Start ECold application**
2. **Go to Recruiters page**
3. **Click "Send Email" for any recruiter**
4. **Choose a template or write custom email**
5. **Click "Send Email"**
6. **Check if email was sent successfully**

---

## 📱 Visual Reference

Here's what you should see at each step:

### Step 1: Google Account Security Page
```
🔒 Security

Signing in to Google
├── Password                    [●●●●●●●●] Change
├── 2-Step Verification        ✓ On
└── App passwords              → Click here
```

### Step 2: App Passwords Generation
```
Generate app password

Select app:    [Mail ▼]
Select device: [Other (Custom name) ▼]

Name: [ECold Application    ]

[GENERATE]
```

### Step 3: Generated Password Display
```
┌─────────────────────────────────────┐
│ Generated app password              │
│                                     │
│ ECold Application                   │
│ abcd efgh ijkl mnop                │
│                                     │
│ [Copy to clipboard] [Done]          │
└─────────────────────────────────────┘
```

---

## 🎯 Final Checklist

Before using your app password:

- ✅ **2FA is enabled** on your Gmail account
- ✅ **App password generated** and copied
- ✅ **Application properties updated** with correct settings
- ✅ **Password is 16 characters** (no spaces)
- ✅ **Username is full email address** (including @gmail.com)
- ✅ **SMTP settings correct** (smtp.gmail.com:587)

---

## 🆘 Still Having Issues?

### Common Final Troubleshooting Steps:

1. **Try a new app password:**
   - Generate a fresh app password
   - Use a different custom name
   - Make sure to copy it correctly

2. **Verify Gmail access:**
   - Log into Gmail normally
   - Make sure account isn't locked
   - Check for any security alerts

3. **Test with email client:**
   - Configure the same settings in Outlook/Thunderbird
   - If it works there, the app password is correct

4. **Check application logs:**
   - Look for specific error messages
   - Enable debug logging for email

---

🎉 **Congratulations!** Once you have your App Password working, ECold will be able to send emails directly through Gmail's servers, providing a seamless email experience for your job search outreach!

**Remember**: Keep your app password secure and never share it with anyone!
