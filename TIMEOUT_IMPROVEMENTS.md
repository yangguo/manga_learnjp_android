# Network Timeout Improvements

## Overview
This document outlines the improvements made to handle network timeout errors in the Manga Analysis app, specifically addressing the `java.net.SocketTimeoutException` errors that occur during image analysis.

## Problem
Users were experiencing timeout errors when analyzing manga images, particularly with:
- Large image files
- Slow network connections
- Custom API endpoints that take longer to process images
- API servers under heavy load

## Solutions Implemented

### 1. Extended Timeout Configuration
- **Standard Client**: 30s connect, 60s read/write timeouts
- **Extended Client**: 45s connect, 180s read, 120s write timeouts
- Extended client is used specifically for image analysis operations

### 2. Retry Logic with Exponential Backoff
- Automatic retry up to 2 additional attempts on timeout
- Exponential backoff delay: 1s, 2s, 4s between retries
- Specific handling for different timeout types (connection, read, network)

### 3. Enhanced Error Messages
- Clear distinction between timeout types
- Actionable troubleshooting suggestions
- User-friendly explanations in the UI

### 4. Improved User Interface
- Dedicated timeout error card with specific guidance
- Quick retry button for timeout errors
- Settings navigation for API configuration

## Technical Changes

### AIService.kt
1. **Added Extended Timeout Client**:
   ```kotlin
   private val extendedTimeoutClient = OkHttpClient.Builder()
       .connectTimeout(45, TimeUnit.SECONDS)
       .readTimeout(180, TimeUnit.SECONDS)  // 3 minutes for image analysis
       .writeTimeout(120, TimeUnit.SECONDS) // 2 minutes for upload
       .build()
   ```

2. **Implemented Retry Logic**:
   ```kotlin
   private fun executeWithRetry(request: Request, maxRetries: Int = 2): Response
   ```

3. **Updated All Image Analysis Methods**:
   - `analyzeWithOpenAI()` - Now uses extended timeout client
   - `analyzeWithGemini()` - Now uses extended timeout client
   - `analyzeWithCustomAPI()` - Now uses retry logic with extended timeouts

### MangaAnalysisScreen.kt
1. **Added Timeout-Specific Error Handling**:
   - Detects timeout-related error messages
   - Provides specific troubleshooting guidance
   - Offers quick retry and settings navigation

## User Benefits

### Improved Reliability
- 3x longer timeout for image processing (180s vs 60s)
- Automatic retry on temporary network issues
- Better handling of slow API responses

### Better User Experience
- Clear error messages explaining what went wrong
- Actionable suggestions for resolving issues
- No need to manually restart the analysis

### Reduced Support Issues
- Self-explanatory error messages
- Built-in troubleshooting guidance
- Automatic recovery from temporary failures

## Troubleshooting Guide

### For Users
If you still experience timeout errors:
1. **Check your network connection**
2. **Try reducing image size** (compress or resize before analysis)
3. **Switch to a different AI provider** (OpenAI/Gemini vs Custom API)
4. **Check API server status** (for custom APIs)
5. **Try again during off-peak hours**

### For Developers
To further customize timeout behavior:
1. Adjust timeout values in `extendedTimeoutClient`
2. Modify retry count in `executeWithRetry()` calls
3. Add provider-specific timeout configurations
4. Implement connection pooling for better performance

## Monitoring
The app now logs detailed timeout information:
- Timeout type (connection, read, network)
- Retry attempts and delays
- Final success/failure status
- Extended vs standard client usage

Check Android logs with tag `MangaLearnJP` for detailed timeout diagnostics.

## Future Improvements
- Adaptive timeout based on image size
- Provider-specific timeout configurations
- Background retry with user notification
- Offline analysis capability
- Image compression before upload