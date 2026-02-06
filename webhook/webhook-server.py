#!/usr/bin/env python3
"""
Webhook server để nhận thông báo từ GitHub và tự động deploy
Chạy trên server EC2
"""

import os
import subprocess
import hmac
import hashlib
import json
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime

# Configuration
WEBHOOK_PORT = 9001
WEBHOOK_SECRET = os.environ.get('WEBHOOK_SECRET', 'your-webhook-secret')
DEPLOY_QA_SCRIPT = '/opt/airlabs/deploy-qa.sh'
DEPLOY_PRODUCTION_SCRIPT = '/opt/airlabs/deploy-production.sh'
LOG_FILE = '/opt/airlabs/webhook.log'

def log(message):
    """Ghi log với timestamp"""
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    log_message = f"[{timestamp}] {message}"
    print(log_message)
    try:
        with open(LOG_FILE, 'a') as f:
            f.write(log_message + '\n')
    except:
        pass

def verify_signature(payload, signature, secret):
    """Xác thực signature từ GitHub"""
    if not signature:
        return False
    
    try:
        sha_name, signature_hash = signature.split('=')
        if sha_name != 'sha256':
            return False
        
        mac = hmac.new(secret.encode(), msg=payload, digestmod=hashlib.sha256)
        return hmac.compare_digest(mac.hexdigest(), signature_hash)
    except:
        return False

class WebhookHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        # Đọc payload
        content_length = int(self.headers.get('Content-Length', 0))
        payload = self.rfile.read(content_length)
        
        # Verify GitHub signature (optional nhưng recommended)
        signature = self.headers.get('X-Hub-Signature-256')
        if WEBHOOK_SECRET != 'your-webhook-secret' and not verify_signature(payload, signature, WEBHOOK_SECRET):
            log("Invalid signature!")
            self.send_response(401)
            self.end_headers()
            return
        
        # Parse JSON payload
        try:
            data = json.loads(payload.decode('utf-8'))
        except json.JSONDecodeError:
            log("Invalid JSON payload")
            self.send_response(400)
            self.end_headers()
            return
        
        # Xác định loại event
        event = self.headers.get('X-GitHub-Event', 'unknown')
        log(f"Received event: {event}")
        
        # Xử lý theo path
        if self.path == '/deploy/qa':
            log("Triggering QA deployment...")
            tag = data.get('tag', 'latest')
            self.trigger_deploy('qa', tag)
        elif self.path == '/deploy/production':
            log("Triggering Production deployment...")
            tag = data.get('tag', 'prod-latest')
            self.trigger_deploy('production', tag)
        elif self.path == '/webhook':
            # Auto-detect từ GitHub workflow dispatch hoặc PR merge event
            self.handle_github_webhook(data, event)
        else:
            log(f"Unknown path: {self.path}")
            self.send_response(404)
            self.end_headers()
            return
        
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps({'status': 'ok'}).encode())
    
    def handle_github_webhook(self, data, event):
        """Xử lý webhook từ GitHub"""
        if event == 'workflow_run':
            workflow_name = data.get('workflow_run', {}).get('name', '')
            conclusion = data.get('workflow_run', {}).get('conclusion', '')
            head_branch = data.get('workflow_run', {}).get('head_branch', '')
            
            log(f"Workflow: {workflow_name}, Conclusion: {conclusion}, Branch: {head_branch}")
            
            if conclusion == 'success':
                if 'QA' in head_branch or head_branch == 'QA':
                    log("QA workflow completed, triggering deploy...")
                    self.trigger_deploy('qa')
                elif 'Production' in head_branch or head_branch == 'Production':
                    log("Production workflow completed, triggering deploy...")
                    self.trigger_deploy('production')
        
        elif event == 'push':
            ref = data.get('ref', '')
            log(f"Push to: {ref}")
            
            # Bạn có thể thêm logic ở đây nếu muốn deploy khi push
        
        elif event == 'ping':
            log("Ping received - webhook is configured correctly!")
    
    def trigger_deploy(self, environment, tag=None):
        """Chạy script deploy với tag cụ thể"""
        script = DEPLOY_QA_SCRIPT if environment == 'qa' else DEPLOY_PRODUCTION_SCRIPT
        
        try:
            # Thêm tag vào command nếu có
            cmd = [script]
            if tag:
                cmd.append(tag)
                log(f"Running: {script} {tag}")
            else:
                log(f"Running: {script}")
                
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=300  # 5 minutes timeout
            )
            
            if result.returncode == 0:
                log(f"{environment.upper()} deployment successful!")
                log(f"Output: {result.stdout[-500:] if len(result.stdout) > 500 else result.stdout}")
            else:
                log(f"{environment.upper()} deployment failed!")
                log(f"Error: {result.stderr[-500:] if len(result.stderr) > 500 else result.stderr}")
        
        except subprocess.TimeoutExpired:
            log(f"Deployment timeout!")
        except Exception as e:
            log(f"Error running deploy script: {str(e)}")
    
    def do_GET(self):
        """Health check endpoint"""
        if self.path == '/health':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({
                'status': 'healthy',
                'service': 'airlabs-webhook',
                'timestamp': datetime.now().isoformat()
            }).encode())
        else:
            self.send_response(404)
            self.end_headers()
    
    def log_message(self, format, *args):
        """Override để log với format tùy chỉnh"""
        log(f"HTTP: {args[0]}")

def main():
    log("=" * 50)
    log("Starting Webhook Server...")
    log(f"Port: {WEBHOOK_PORT}")
    log(f"QA Script: {DEPLOY_QA_SCRIPT}")
    log(f"Production Script: {DEPLOY_PRODUCTION_SCRIPT}")
    log("=" * 50)
    log("")
    log("Available endpoints:")
    log("  POST /webhook           - GitHub webhook (auto-detect)")
    log("  POST /deploy/qa         - Trigger QA deployment")
    log("  POST /deploy/production - Trigger Production deployment")
    log("  GET  /health            - Health check")
    log("")
    
    server = HTTPServer(('0.0.0.0', WEBHOOK_PORT), WebhookHandler)
    
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log("Server stopped")
        server.shutdown()

if __name__ == '__main__':
    main()
