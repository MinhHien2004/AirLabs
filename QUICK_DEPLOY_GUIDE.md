# 🚀 Quick Deployment Guide

## TL;DR

### Trước (Cũ)
```bash
# GitHub Actions push: v0.0.123, qa-latest
# Deploy: docker run qa-latest  ❌ Không biết version chính xác
```

### Sau (Mới)
```bash
# GitHub Actions push: v0.0.123 (only)
# Deploy: ./deploy-helper.sh qa v0.0.123  ✅ Version rõ ràng
```

---

## 📋 Commands Thường Dùng

### Xem Các Tags Available
```bash
./deploy-helper.sh list-qa        # List QA tags
./deploy-helper.sh list-prod      # List Production tags
```

### Deploy
```bash
# Deploy QA với tag cụ thể
./deploy-helper.sh qa v0.0.125

# Deploy Production với tag cụ thể
./deploy-helper.sh production prod-v0.0.125
```

### Kiểm Tra Status
```bash
# Test toàn bộ workflow
./test-deployment.sh

# Xem containers đang chạy
docker ps --filter "name=airlabs-app"

# Health check
curl http://localhost:8081/actuator/health  # QA
curl http://localhost:8080/actuator/health  # Production
```

### Logs
```bash
# Container logs
docker logs -f airlabs-app-qa
docker logs -f airlabs-app-production

# Webhook logs
tail -f /opt/airlabs/webhook.log
```

---

## 🔄 Workflow Tự Động

### 1. Deploy QA
```
main → Create PR → QA → Merge
  ↓
GitHub Actions Build v0.0.123
  ↓
Push to Docker Hub: v0.0.123
  ↓
Webhook triggers: deploy-qa.sh v0.0.123
  ↓
QA running on port 8081
```

### 2. Deploy Production
```
QA → Create PR → Production → Merge
  ↓
GitHub Actions query latest QA tag → v0.0.123
  ↓
Re-tag as prod-v0.0.123
  ↓
Webhook triggers: deploy-production.sh prod-v0.0.123
  ↓
Production running on port 8080
```

---

## 📁 Files Changed

| File | Changes |
|------|---------|
| `.github/workflows/build.yaml` | ✅ Removed `qa-latest` tag<br>✅ Added Docker Hub API query for latest QA tag |
| `deploy-qa.sh` | ✅ Accept tag as parameter: `$1` |
| `deploy-production.sh` | ✅ Accept tag as parameter: `$1` |
| `webhook/webhook-server.py` | ✅ Parse `tag` from webhook payload<br>✅ Pass tag to deploy scripts |
| `deploy-helper.sh` | ✨ NEW: Helper to list & deploy |
| `test-deployment.sh` | ✨ NEW: Test workflow |
| `DEPLOYMENT_UPDATE_GUIDE.md` | 📝 Full documentation |

---

## 🔧 Setup on Server

```bash
# 1. SSH vào server
ssh -i "Hien-Key.pem" ubuntu@3.88.65.140

# 2. Pull code mới
cd /opt/airlabs
git pull origin main

# 3. Copy scripts
cp deploy-*.sh /opt/airlabs/
cp webhook/webhook-server.py /opt/airlabs/webhook/
chmod +x /opt/airlabs/deploy-*.sh
chmod +x /opt/airlabs/deploy-helper.sh
chmod +x /opt/airlabs/test-deployment.sh

# 4. Restart webhook
sudo systemctl restart webhook

# 5. Test
./test-deployment.sh
```

---

## ✅ Checklist

- [x] Cập nhật GitHub Actions workflow
- [x] Cập nhật deployment scripts
- [x] Cập nhật webhook server
- [x] Tạo helper scripts
- [ ] Upload scripts lên server
- [ ] Restart webhook service
- [ ] Test deployment
- [ ] Commit & push to GitHub

---

## 🆘 Troubleshooting

### Webhook không hoạt động
```bash
# Check status
sudo systemctl status webhook

# Restart
sudo systemctl restart webhook

# View logs
sudo journalctl -u webhook -f
```

### Container không start
```bash
# Check logs
docker logs airlabs-app-qa

# Check image exists
docker pull hienminh1332004/airlabs-realtime-flight:v0.0.123

# Rollback
./deploy-helper.sh qa v0.0.120
```

### Tag không tìm thấy
```bash
# List all tags manually
curl -s "https://hub.docker.com/v2/repositories/hienminh1332004/airlabs-realtime-flight/tags/" | jq

# Check GitHub Actions logs
# https://github.com/your-repo/actions
```

---

## 📚 Full Documentation

- [DEPLOYMENT_UPDATE_GUIDE.md](./DEPLOYMENT_UPDATE_GUIDE.md) - Chi tiết đầy đủ
- [CI-CD-COMPLETE-GUIDE.md](./CI-CD-COMPLETE-GUIDE.md) - CI/CD toàn diện
- [SERVER_DEPLOYMENT.md](./SERVER_DEPLOYMENT.md) - Server setup

---

## 🎯 Benefits

✅ **Version Control**: Biết chính xác version nào đang chạy  
✅ **Easy Rollback**: `./deploy-helper.sh qa v0.0.120`  
✅ **Audit Trail**: Logs ghi rõ tag  
✅ **Consistency**: Production = exact QA version tested  
✅ **No Confusion**: Không còn "latest" nào cả
