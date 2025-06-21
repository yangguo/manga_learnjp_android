# Manga Learn JP - Debugging Guide

## Common Issues and Solutions

### 1. Analysis Button Does Nothing / No Response

**Symptoms:**
- Click "Quick Analysis" or "Analyze" button
- Nothing happens, no error message
- App appears frozen or unresponsive

**Debugging Steps:**

#### Step 1: Check Error Messages
- Look for red error cards in the app UI
- Enhanced error messages now show specific issues
- Error cards include detailed troubleshooting steps

#### Step 2: Check Android Logs (Logcat)
```bash
# Filter logs for this app
adb logcat -s MangaLearnJP

# Or use Android Studio Logcat with filter: "MangaLearnJP"
```

#### Step 3: Verify Prerequisites
- ✅ **Image Selected**: Make sure you've uploaded an image
- ✅ **API Key Configured**: At least one AI provider must be set up
- ✅ **Internet Connection**: Check network connectivity
- ✅ **Valid API Key**: Ensure API key is correct and has permissions

### 2. API Configuration Issues

#### OpenAI Setup
1. **Get API Key**: Visit https://platform.openai.com/api-keys
2. **Required Access**: Your account needs GPT-4 Vision access
3. **Billing**: Ensure your OpenAI account has billing set up
4. **Key Format**: Should start with `sk-` and be ~51 characters long

#### Google Gemini Setup
1. **Get API Key**: Visit https://makersuite.google.com/app/apikey
2. **Enable API**: Make sure Gemini API is enabled in Google Cloud
3. **Quota**: Check your API quota and usage limits

#### Custom API Setup
1. **Endpoint URL**: Must be a valid OpenAI-compatible API endpoint
2. **Model Name**: Specify the correct model name for your API
3. **Authentication**: Ensure your API key works with the endpoint

### 3. Network and Connection Issues

#### Common Error Messages:
- **"Request Timeout"**: API call took too long
- **"Connection Failed"**: Cannot reach API servers
- **"Network Error"**: DNS or routing issues
- **"Authentication Failed (401)"**: Invalid API key
- **"Access Forbidden (403)"**: API key lacks permissions
- **"Rate Limit Exceeded (429)"**: Too many requests

#### Solutions:
1. **Check Internet**: Verify you have a stable internet connection
2. **Try Different Network**: Switch between WiFi and mobile data
3. **Firewall/VPN**: Disable VPN or check firewall settings
4. **Image Size**: Try with a smaller image (< 2MB recommended)
5. **Wait and Retry**: For rate limits, wait a few minutes

### 4. Image Processing Issues

#### Supported Formats:
- ✅ JPEG (.jpg, .jpeg)
- ✅ PNG (.png)
- ✅ WebP (.webp)

#### Recommendations:
- **Size**: Keep images under 5MB for best performance
- **Resolution**: 1024x1024 or smaller works well
- **Quality**: Clear, high-contrast text works best
- **Language**: Japanese text should be clearly visible

### 5. Detailed Logging

The app now includes comprehensive logging. To view logs:

#### Using Android Studio:
1. Connect your device
2. Open Logcat
3. Filter by "MangaLearnJP"
4. Look for entries with categories:
   - `[UI]` - User interface events
   - `[VM]` - ViewModel actions
   - `[AI]` - AI service calls
   - `[NET]` - Network requests
   - `[IMG]` - Image processing
   - `[CFG]` - Configuration changes
   - `[ERR]` - Errors
   - `[DBG]` - Debug information

#### Using ADB:
```bash
# Real-time logs
adb logcat -s MangaLearnJP

# Save logs to file
adb logcat -s MangaLearnJP > manga_app_logs.txt

# Clear logs and start fresh
adb logcat -c && adb logcat -s MangaLearnJP
```

### 6. Performance Issues

#### If the app is slow:
1. **Image Size**: Reduce image resolution
2. **Memory**: Close other apps to free up RAM
3. **Storage**: Ensure device has sufficient storage
4. **Network**: Use a faster internet connection

### 7. Getting Help

When reporting issues, please include:

1. **Error Message**: Full text from the error card
2. **Logs**: Relevant entries from Logcat
3. **Steps**: What you were doing when the issue occurred
4. **Device Info**: Android version, device model
5. **Image Info**: Size, format, content type
6. **API Provider**: Which AI service you're using

#### Log Examples to Look For:

**Successful Analysis:**
```
MangaLearnJP: [VM] Action: Starting full image analysis
MangaLearnJP: [AI] Provider: OPENAI, Action: analyzeImage
MangaLearnJP: [NET] Making API request to OpenAI
MangaLearnJP: [NET] Response received, status: 200
MangaLearnJP: [AI] Analysis successful
```

**Failed Analysis:**
```
MangaLearnJP: [ERR] Error in analyzeWithOpenAI: API call failed with status 401
MangaLearnJP: [ERR] Error body: {"error": {"message": "Invalid API key"}}
```

### 8. Quick Troubleshooting Checklist

- [ ] Image is selected and visible in the app
- [ ] At least one API key is configured in Settings
- [ ] Internet connection is working
- [ ] API key is valid and has proper permissions
- [ ] No firewall blocking the connection
- [ ] Sufficient device storage and memory
- [ ] App has necessary permissions
- [ ] Latest version of the app is installed

### 9. Advanced Debugging

For developers or advanced users:

#### Enable Verbose Logging:
The app automatically logs detailed information. Check these log categories:

- **Function Entry/Exit**: Track method calls
- **State Changes**: Monitor UI state transitions
- **Network Requests**: Full request/response details
- **Performance Metrics**: Timing information
- **Configuration Changes**: Settings modifications

#### Common Code Paths:
1. `quickAnalysis()` → `validateAnalysisPrerequisites()` → `analyzeFullImage()`
2. `analyzeFullImage()` → `AIService.analyzeImage()` → `analyzeWithOpenAI()`
3. Error handling → UI state update → Error card display

This enhanced debugging system should help identify exactly where issues occur in the analysis pipeline.