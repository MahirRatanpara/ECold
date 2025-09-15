#!/bin/sh

# Docker entrypoint script for ECold frontend

set -e

echo "Starting ECold Frontend..."

# Replace environment variables in JavaScript files if needed
if [ -n "$API_URL" ]; then
    echo "Setting API URL to: $API_URL"
    find /usr/share/nginx/html -name "*.js" -exec sed -i "s|http://localhost:8080/api|$API_URL|g" {} \;
fi

# Create nginx directories if they don't exist
mkdir -p /var/cache/nginx /var/log/nginx

# Start nginx
echo "Starting Nginx..."
exec "$@"