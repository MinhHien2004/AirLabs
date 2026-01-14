# Security Configuration Guide

## ⚠️ IMPORTANT: Never Commit Sensitive Data!

This guide explains how to properly configure sensitive information for this application.

## 🔐 Environment Variables Setup

### For Local Development:

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your actual credentials:**
   ```properties
   SPRING_DATASOURCE_URL=jdbc:postgresql://your-host:5432/your-database
   SPRING_DATASOURCE_USERNAME=your_username
   SPRING_DATASOURCE_PASSWORD=your_actual_password
   AIRLABS_API_KEY=your_actual_api_key
   SPRING_REDIS_PASSWORD=your_redis_password
   ```

3. **Load environment variables (Windows PowerShell):**
   ```powershell
   # Option 1: Set for current session
   $env:SPRING_DATASOURCE_PASSWORD="your_password"
   $env:AIRLABS_API_KEY="your_api_key"
   
   # Option 2: Set permanently (System Environment Variables)
   [System.Environment]::SetEnvironmentVariable('SPRING_DATASOURCE_PASSWORD', 'your_password', 'User')
   ```

4. **Load environment variables (Linux/Mac):**
   ```bash
   export SPRING_DATASOURCE_PASSWORD="your_password"
   export AIRLABS_API_KEY="your_api_key"
   ```

### For Production (Render/Heroku/Cloud):

1. **Set environment variables in your hosting platform dashboard:**
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `AIRLABS_API_KEY`
   - `SPRING_REDIS_HOST`
   - `SPRING_REDIS_PORT`
   - `SPRING_REDIS_PASSWORD`

2. **Example for Render.com:**
   - Go to your service → Environment
   - Add each variable as a key-value pair
   - Render will automatically inject them

## 🛡️ Security Best Practices

### ✅ DO:
- Use environment variables for all sensitive data
- Keep `.env` file in `.gitignore`
- Use different credentials for development and production
- Rotate API keys regularly
- Use HTTPS in production
- Enable CORS only for trusted domains
- Review and update `.gitignore` regularly

### ❌ DON'T:
- Never commit `.env` files
- Never hardcode passwords or API keys in source code
- Never expose credentials in logs
- Don't use default passwords in production
- Don't commit `application-prod.yaml` with real credentials

## 🔍 What Has Been Secured

1. **Database Credentials** - Moved to environment variables
2. **AirLabs API Key** - Moved to environment variables  
3. **Redis Password** - Moved to environment variables
4. **Frontend URLs** - Using relative paths instead of hardcoded URLs

## 📋 Files Changed

- ✅ `application.yaml` - Removed hardcoded credentials
- ✅ `ScheduleService.java` - Inject config instead of hardcoded API key
- ✅ `FlightService.java` - Inject config instead of hardcoded API key
- ✅ `Scheduled.html` - Using relative paths
- ✅ `.gitignore` - Added environment files
- ✅ `.env.example` - Template for credentials

## 🚀 Running the Application

```bash
# Make sure environment variables are set first!
mvn spring-boot:run
```

## 🔧 Troubleshooting

**Issue:** Application fails to start with "Cannot determine embedded database driver class"
- **Solution:** Make sure `SPRING_DATASOURCE_PASSWORD` is set as environment variable

**Issue:** API calls return 401/403 errors
- **Solution:** Check that `AIRLABS_API_KEY` is correctly set and valid

**Issue:** Redis connection fails
- **Solution:** Verify `SPRING_REDIS_PASSWORD` is set correctly, or disable Redis if not using cache

## 📞 Support

If you have security concerns, DO NOT post credentials in public issues. Contact the development team privately.
