#!/bin/bash

# YouCan Integration Setup Script
# This script helps initialize the YouCan CLI and set up the integration

echo "🚀 YouCan Integration Setup"
echo "============================"
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js first."
    exit 1
fi

echo "✅ Node.js found: $(node --version)"
echo ""

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed. Please install npm first."
    exit 1
fi

echo "✅ npm found: $(npm --version)"
echo ""

# Prompt for setup method
echo "Choose setup method:"
echo "1) Initialize YouCan CLI app (npm init @youcan/app@latest)"
echo "2) Manual setup (skip CLI, configure manually)"
read -p "Enter choice [1-2]: " choice

case $choice in
    1)
        echo ""
        echo "📦 Initializing YouCan CLI..."
        echo "Follow the prompts to configure your app."
        echo ""
        npm init @youcan/app@latest --yes
        
        echo ""
        echo "✅ YouCan CLI initialization complete!"
        echo ""
        echo "📝 Next steps:"
        echo "1. Note your Client ID and Client Secret from the CLI output"
        echo "2. Update src/main/resources/application.properties with:"
        echo "   - youcan.oauth.client-id=YOUR_CLIENT_ID"
        echo "   - youcan.oauth.client-secret=YOUR_CLIENT_SECRET"
        echo "3. Ensure redirect URI is set to:"
        echo "   - Development: http://localhost:8080/api/youcan/oauth/callback"
        echo "   - Production: https://yourdomain.com/api/youcan/oauth/callback"
        echo "4. Restart your Spring Boot application"
        ;;
    2)
        echo ""
        echo "📝 Manual Setup Instructions:"
        echo ""
        echo "1. Go to YouCan Developer Dashboard:"
        echo "   https://partners.youcan.shop"
        echo ""
        echo "2. Create a new app with:"
        echo "   - Redirect URL: http://localhost:8080/api/youcan/oauth/callback"
        echo "   - Scopes: orders:read orders:write"
        echo ""
        echo "3. Copy your Client ID and Client Secret"
        echo ""
        echo "4. Update src/main/resources/application.properties:"
        echo "   youcan.oauth.client-id=YOUR_CLIENT_ID"
        echo "   youcan.oauth.client-secret=YOUR_CLIENT_SECRET"
        echo ""
        echo "5. Restart your Spring Boot application"
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "📚 For more details, see YOUCAN_SETUP.md"
echo ""

