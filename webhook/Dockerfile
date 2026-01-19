# Webhook listener using webhook package
FROM almir/webhook:latest

# Copy webhook configuration
COPY hooks.json /etc/webhook/hooks.json
COPY scripts/ /scripts/

# Make scripts executable
RUN chmod +x /scripts/*.sh

# Expose webhook port
EXPOSE 9000

# Run webhook
CMD ["-hooks", "/etc/webhook/hooks.json", "-verbose", "-port", "9000", "-hotreload"]
