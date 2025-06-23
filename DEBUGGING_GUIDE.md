# Manga Learn JP - Debugging Guide

## Common Issues and Solutions

### 1. Analysis Button Does Nothing / No Response

**Symptoms:**
- Click "Quick Analysis" or "Analyze" button
- Nothing happens, no error message
- App appears frozen or unresponsive

**Root Cause Analysis:**

**UPDATED FIX (2025-01-22):**
The issue has been identified and fixed. The problem was that API keys were not being properly trimmed of whitespace when saved or loaded, causing validation to fail even when keys were entered. The following changes have been made:

1. **Enhanced API Key Validation**: Added proper trimming in PreferencesRepository
2. **Improved Debug Logging**: Added comprehensive logging throughout the data flow
3. **Fixed UI Callbacks**: API keys are now trimmed when entered in settings

**To verify the fix:**
1. Clear app data or reinstall the app
2. Go to Settings and enter your OpenAI API key
3. Check logcat for debug messages:
   ```
   adb logcat | grep MangaLearnJP
   ```
4. You should see messages like:
   ```
   AISettingsScreen: OpenAI API key updated - length: XX
   ViewModel: updateAIConfig called - OpenAI key length: XX
   PreferencesRepository: saveAIConfig called - OpenAI key length: XX
   PreferencesRepository: Loaded AI config - OpenAI key length: XX
   ```

**If the issue persists:**
- Check that you're entering a valid OpenAI API key (starts with 'sk-' and is ~51 characters)
- Ensure you have internet connectivity
- Verify the API key works in other applications

**Original Root Cause Analysis:**

**FIXED ISSUE: API Key Validation with Whitespace**
- **Problem**: API keys with leading/trailing whitespace were not being recognized as valid
- **Solution**: Added `.trim()` to all API key validation and storage operations
- **Files Modified**: 
  - `AIProvider.kt`: Enhanced `isProviderConfigured()` method
  - `PreferencesRepository.kt`: Added trimming when saving API keys
  - `MangaAnalysisViewModel.kt`: Enhanced debug logging

**Additional Troubleshooting Steps:**
1. **Re-enter API Key**: Clear the API key field completely and re-enter it
2. **Check Debug Logs**: Look for "API key status" logs showing actual key lengths
3. **Verify Trimming**: The app now automatically trims whitespace from API keys
4. **Enhanced Error Messages**: Error messages now include debug info about key lengths
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
**Enhanced Error Message:** The app now provides detailed troubleshooting information including:
- Current status of each provider (OpenAI, Gemini, Custom)
- Detection of whitespace issues in API keys
- Step-by-step solutions

**Solutions:**
1. Go to Settings (⚙️ icon)
2. Enter a valid API key for at least one provider
3. **Important:** Make sure there are no extra spaces before/after the API key
4. Try the 'Refresh Config' button in Settings
5. If issues persist, try 'Clear All Preferences' and reconfigure

#### "❌ OpenAI is set as primary provider but API key is missing"
**Solution:** Either add your OpenAI API key in Settings or switch to a different primary provider

#### "❌ OpenAI API key appears to be invalid (too short: X characters)"
**Solution:** Check your OpenAI API key - it should start with 'sk-' and be much longer

#### "❌ OpenAI API key format appears incorrect (should start with 'sk-')"
**Solution:** Verify you copied the correct API key from OpenAI platform

#### "Analysis failed: [API Error]"
**Possible causes:**
- Invalid API key
- API quota exceeded
- Network connectivity issues
- API service temporarily down
- Whitespace in API keys

**Solutions:**
1. Verify API key is correct and has no extra spaces
2. Check API usage/billing status
3. Try different provider (enable fallback in settings)
4. Check internet connection
5. Use the 'Debug Test' button in Settings to diagnose issues

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