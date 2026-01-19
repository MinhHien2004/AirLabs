# Webhook listener using webhook package
FROM almir/webhook:latest

# Copy webhook configuration
COPY hooks.json /etc/webhook/hooks.json
COPY scripts/ /scripts/

# Expose webhook port
EXPOSE 9000

# Make scripts executable and run webhook
CMD chmod +x /scripts/*.sh && webhook -hooks /etc/webhook/hooks.json -verbose -port 9000 -hotreload
