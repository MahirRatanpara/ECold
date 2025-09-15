#!/bin/bash

# ECold Email Setup Script
# This script helps you configure email functionality quickly

echo "🚀 ECold Email Configuration Setup"
echo "=================================="

# Check if running on Windows
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    PROPERTIES_FILE="backend/src/main/resources/application.properties"
else
    PROPERTIES_FILE="backend/src/main/resources/application.properties"
fi

echo ""
echo "📧 Choose your email provider:"
echo "1) Gmail (Recommended)"
echo "2) Outlook/Office 365"
echo "3) Custom SMTP Server"
echo "4) Skip configuration"

read -p "Enter your choice (1-4): " choice

case $choice in
    1)
        echo ""
        echo "🔧 Gmail Configuration Setup"
        echo "=============================="
        echo ""
        echo "⚠️  IMPORTANT SETUP STEPS:"
        echo "1. Go to https://myaccount.google.com/"
        echo "2. Click Security → 2-Step Verification (enable if not already)"
        echo "3. Click Security → App passwords"
        echo "4. Select 'Mail' and 'Other (Custom name)'"
        echo "5. Enter 'ECold Application' as the name"
        echo "6. Copy the 16-character password generated"
        echo ""
        
        read -p "📧 Enter your Gmail address: " gmail_address
        read -p "🔑 Enter your App Password (16 characters): " app_password
        
        # Backup existing properties file
        if [ -f "$PROPERTIES_FILE" ]; then
            cp "$PROPERTIES_FILE" "$PROPERTIES_FILE.backup.$(date +%Y%m%d_%H%M%S)"
            echo "📁 Backed up existing properties file"
        fi
        
        # Add Gmail configuration
        cat >> "$PROPERTIES_FILE" << EOF

# ECold Email Configuration - Gmail SMTP
app.email.enabled=true
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=$gmail_address
spring.mail.password=$app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000
app.email.from-name=ECold Application
EOF
        
        echo "✅ Gmail configuration added to $PROPERTIES_FILE"
        ;;
        
    2)
        echo ""
        echo "🔧 Outlook Configuration Setup"
        echo "==============================="
        
        read -p "📧 Enter your Outlook email: " outlook_email
        read -p "🔑 Enter your password: " outlook_password
        
        # Backup existing properties file
        if [ -f "$PROPERTIES_FILE" ]; then
            cp "$PROPERTIES_FILE" "$PROPERTIES_FILE.backup.$(date +%Y%m%d_%H%M%S)"
            echo "📁 Backed up existing properties file"
        fi
        
        # Add Outlook configuration
        cat >> "$PROPERTIES_FILE" << EOF

# ECold Email Configuration - Outlook SMTP
app.email.enabled=true
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=$outlook_email
spring.mail.password=$outlook_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.email.from-name=ECold Application
EOF
        
        echo "✅ Outlook configuration added to $PROPERTIES_FILE"
        ;;
        
    3)
        echo ""
        echo "🔧 Custom SMTP Configuration"
        echo "============================="
        
        read -p "🌐 SMTP Host: " smtp_host
        read -p "🔌 SMTP Port (587): " smtp_port
        smtp_port=${smtp_port:-587}
        read -p "📧 Username: " smtp_user
        read -p "🔑 Password: " smtp_pass
        read -p "📛 From Name (ECold Application): " from_name
        from_name=${from_name:-"ECold Application"}
        
        # Backup existing properties file
        if [ -f "$PROPERTIES_FILE" ]; then
            cp "$PROPERTIES_FILE" "$PROPERTIES_FILE.backup.$(date +%Y%m%d_%H%M%S)"
            echo "📁 Backed up existing properties file"
        fi
        
        # Add custom SMTP configuration
        cat >> "$PROPERTIES_FILE" << EOF

# ECold Email Configuration - Custom SMTP
app.email.enabled=true
spring.mail.host=$smtp_host
spring.mail.port=$smtp_port
spring.mail.username=$smtp_user
spring.mail.password=$smtp_pass
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.email.from-name=$from_name
EOF
        
        echo "✅ Custom SMTP configuration added to $PROPERTIES_FILE"
        ;;
        
    4)
        echo "⏭️  Skipping email configuration"
        exit 0
        ;;
        
    *)
        echo "❌ Invalid choice. Exiting."
        exit 1
        ;;
esac

echo ""
echo "🧪 Testing Configuration"
echo "========================"

# Check if Spring Boot is running
if curl -f -s "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
    echo "✅ Application is running"
    
    echo ""
    read -p "📨 Enter email address to send test email to: " test_email
    
    if [ ! -z "$test_email" ]; then
        echo "📤 Sending test email..."
        
        response=$(curl -s -X POST "http://localhost:8080/api/emails/test?toEmail=$test_email" \
                   -H "Content-Type: application/json")
        
        if echo "$response" | grep -q '"success":true'; then
            echo "✅ Test email sent successfully!"
            echo "📬 Check $test_email inbox for the test message"
        else
            echo "❌ Test email failed"
            echo "Response: $response"
        fi
    fi
else
    echo "⚠️  Application is not running on http://localhost:8080"
    echo "Please start your Spring Boot application and then test manually:"
    echo ""
    echo "1. Start application: ./mvnw spring-boot:run (or gradle bootRun)"
    echo "2. Test configuration:"
    echo "   curl -X GET http://localhost:8080/api/emails/config/status"
    echo "3. Send test email:"
    echo "   curl -X POST \"http://localhost:8080/api/emails/test?toEmail=test@example.com\""
fi

echo ""
echo "📚 Next Steps:"
echo "=============="
echo "1. ✅ Email configuration is complete"
echo "2. 🚀 Start/restart your ECold application"
echo "3. 🧪 Test email sending from the UI:"
echo "   - Go to Recruiters page"
echo "   - Click 'Send Email' for any recruiter"
echo "   - Select a template or write custom email"
echo "   - Click 'Send Email' - it will now send directly from the app!"
echo "4. 📖 Read EMAIL_SETUP_GUIDE.md for detailed documentation"
echo ""
echo "🎉 Email setup complete! Happy emailing with ECold!"

