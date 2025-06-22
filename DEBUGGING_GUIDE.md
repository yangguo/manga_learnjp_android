# Manga Learn JP - Debugging Guide

## Common Issues and Solutions

### 1. Analysis Button Does Nothing / No Response

**Symptoms:**
- Click "Quick Analysis" or "Analyze" button
- Nothing happens, no error message
- App appears frozen or unresponsive

**Root Cause Analysis:**
The most common cause is that **no AI providers are configured**. The app requires at least one API key to be set up before analysis can work.

**Debugging Steps:**

#### Step 1: Check Logs
If you have access to Android Studio or adb, check the logs for:
```
adb logcat -s MangaLearnJP
```

Look for these log messages:
- `UI: Quick AI Analysis button clicked` - Confirms button click is registered
- `ViewModel: quickAnalysis() called` - Confirms ViewModel method is called
- `ViewModel: validateAnalysisPrerequisites() called` - Confirms validation starts
- `ViewModel: Validation failed` - Shows what validation failed

#### Step 2: Most Common Issue - No API Keys Configured

**Problem:** No AI providers are configured
**Solution:** Configure at least one API provider:

1. **Go to Settings** (gear icon in top-right)
2. **Configure at least one provider:**

   **Option A: OpenAI (Recommended)**
   - Get API key from https://platform.openai.com/api-keys
   - Enter in "OpenAI API Key" field
   - Default models (gpt-4o for vision) should work

   **Option B: Google Gemini**
   - Get API key from https://aistudio.google.com/app/apikey
   - Enter in "Gemini API Key" field

   **Option C: Custom OpenAI-Compatible API**
   - Enter your custom endpoint URL
   - Enter your API key
   - Specify the model name

3. **Save settings** and try analysis again

#### Step 3: Verify Prerequisites
- ✅ **Image Selected**: Make sure you've uploaded an image
- ✅ **API Key Configured**: At least one AI provider must be set up
- ✅ **Internet Connection**: Check network connectivity
- ✅ **Valid API Key**: Ensure API key is correct and has permissions

#### Step 4: Test Network Connection
The app includes a network test. Check logs for:
```
ViewModel: Network test result: true/false
```

#### Step 5: Check API Key Validation
Look for these error patterns in logs:
- `OpenAI API key is not configured`
- `No AI providers configured`
- `All configured providers failed`

### 2. Analysis Fails with Error Message

**Symptoms:**
- Error message appears in red box
- Analysis stops immediately

**Common Error Messages:**

#### "❌ No image selected. Please upload an image first."
**Solution:** Upload an image before clicking analyze

#### "❌ No AI providers configured. Please set up at least one API key in Settings"
**Solution:** Follow Step 2 above to configure API keys

#### "Analysis failed: [API Error]"
**Possible causes:**
- Invalid API key
- API quota exceeded
- Network connectivity issues
- API service temporarily down

**Solutions:**
1. Verify API key is correct
2. Check API usage/billing status
3. Try different provider (enable fallback in settings)
4. Check internet connection

### 3. Image Upload Issues

**Symptoms:**
- Can't select image
- Image doesn't appear after selection
- "Failed to load image" error

**Solutions:**
1. **Check file format**: Supports PNG, JPG, WebP
2. **Check file size**: Very large images may cause issues
3. **Check permissions**: App needs storage access
4. **Try different image**: Test with a smaller, simpler image

### 4. Performance Issues

**Symptoms:**
- Analysis takes very long time
- App becomes unresponsive
- Out of memory errors

**Solutions:**
1. **Reduce image size**: Use smaller images (< 2MB recommended)
2. **Close other apps**: Free up device memory
3. **Check network**: Slow connection affects API calls
4. **Try different provider**: Some APIs are faster than others

### 5. Panel Segmentation Issues

**Symptoms:**
- Panels not detected correctly
- Wrong reading order
- Missing panels

**Solutions:**
1. **Use high-contrast images**: Clear panel borders work better
2. **Try different manga styles**: Some layouts work better than others
3. **Use manual mode**: If auto-detection fails, use simple analysis

### 6. API-Specific Issues

#### OpenAI Issues:
- **Rate limits**: Wait and retry
- **Model availability**: Ensure gpt-4o is available in your region
- **Billing**: Check account has credits

#### Gemini Issues:
- **Regional availability**: Gemini not available in all regions
- **Content policies**: Some manga content may be filtered
- **API limits**: Check quota usage

#### Custom API Issues:
- **Endpoint format**: Must be OpenAI-compatible
- **Authentication**: Check API key format
- **Model support**: Ensure vision models are supported

### 7. Getting Help

#### Log Examples to Look For:

**Successful Analysis:**
```
UI: Quick AI Analysis button clicked
ViewModel: quickAnalysis() called
ViewModel: validateAnalysisPrerequisites() called
ViewModel: All validations passed
AIService: Starting analyzeImage
AIService: Analysis successful with provider: OPENAI
```

**Failed Analysis (No API Key):**
```
UI: Quick AI Analysis button clicked
ViewModel: quickAnalysis() called
ViewModel: validateAnalysisPrerequisites() called
ViewModel: Validation failed - ❌ No AI providers configured
```

**Failed Analysis (API Error):**
```
UI: Quick AI Analysis button clicked
ViewModel: quickAnalysis() called
AIService: Starting analyzeImage
AIService: Analysis failed with provider: OPENAI - Invalid API key
```

### 8. Quick Troubleshooting Checklist

- [ ] Image is selected and visible in the app
- [ ] At least one API key is configured in Settings
- [ ] Internet connection is working
- [ ] API key is valid and has credits/quota
- [ ] App has necessary permissions
- [ ] Device has sufficient memory
- [ ] No firewall blocking API requests

### 9. Advanced Debugging

If issues persist:

1. **Enable verbose logging** in the app
2. **Check device logs** with Android Studio
3. **Test with minimal image** (small, simple manga panel)
4. **Try different API provider** to isolate the issue
5. **Check API status pages** for service outages

### 10. Recent Updates (Debugging Improvements)

The app now includes enhanced logging to help identify issues:
- Button click tracking
- Validation step logging
- API call tracing
- Error message improvements

These logs will help identify exactly where the analysis process fails.

---

## Contact Support

If you continue experiencing issues after following this guide:
1. Include relevant log messages
2. Describe your device and Android version
3. Specify which API provider you're using
4. Include steps to reproduce the issue