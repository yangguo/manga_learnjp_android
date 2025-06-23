# Custom API Debugging and Validation Improvements

## Issues Identified and Fixed

### 1. **Enhanced Request Validation**
- Added validation for empty model parameter with fallback to `gpt-4-vision-preview`
- Added validation for empty API key with clear error message
- Enhanced endpoint URL validation
- Added comprehensive logging of request structure

### 2. **Improved Request Body Construction**
- Verified OpenAI-compatible JSON structure for vision API calls
- Added detailed logging of request body (sanitized for security)
- Confirmed proper base64 image encoding and formatting
- Added bitmap validation before processing

### 3. **Enhanced Response Handling**
- Added response preview logging (first 200 characters)
- Improved error messages with specific HTTP status code guidance
- Enhanced JSON parsing with better fallback handling
- Added comprehensive logging throughout the response chain

### 4. **Key Validation Checks Added**

#### Request Validation:
```kotlin
// Model validation with fallback
val modelToUse = if (config.model.trim().isEmpty()) {
    "gpt-4-vision-preview" // Fallback model for vision tasks
} else {
    config.model.trim()
}

// API key validation
if (config.apiKey.trim().isEmpty()) {
    return Result.failure(IllegalArgumentException("Custom API key is empty"))
}

// Endpoint validation
if (!config.endpoint.startsWith("http://") && !config.endpoint.startsWith("https://")) {
    return Result.failure(IllegalArgumentException("Invalid endpoint URL format"))
}
```

#### Bitmap Processing:
```kotlin
println("AIService: Bitmap info - Size: ${bitmap.width}x${bitmap.height}")
val base64Image = bitmapToBase64(bitmap)
if (base64Image.isEmpty()) {
    return Result.failure(IllegalArgumentException("Failed to convert bitmap to base64"))
}
```

### 5. **Request Structure Verification**
The request now properly constructs an OpenAI-compatible vision API call:
```json
{
  "model": "your-model-name",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "Analyze this manga image..."
        },
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/jpeg;base64,{base64_image}"
          }
        }
      ]
    }
  ],
  "max_tokens": 4000
}
```

### 6. **Common 404 Error Causes and Solutions**

#### Possible Causes of 404 Error:
1. **Incorrect Endpoint URL**
   - Wrong path for the specific API provider
   - Wrong base URL
   - Typo in URL

2. **Server Configuration Issues**
   - API server not running
   - Wrong port number
   - Firewall blocking access

3. **API Provider Differences**
   - Different providers use different endpoint paths
   - Some use `/chat/completions`, others use `/api/v1/generate`, etc.
   - Model name not supported by server

#### Solutions Implemented:
1. **Enhanced Error Messages**: Specific guidance for each error type
2. **Request Logging**: Full visibility into what's being sent
3. **Fallback Model**: Automatic fallback if model not specified
4. **Validation**: Pre-request validation of all parameters

### 7. **Testing Checklist**
Before making requests, the system now validates:
- ✅ API key is not empty
- ✅ Endpoint URL is properly formatted
- ✅ Model name is specified (with fallback)
- ✅ Bitmap is valid and can be converted to base64
- ✅ Request body is properly structured
- ✅ Authorization header is added

### 8. **Debugging Information Available**
The enhanced logging now provides:
- Request URL and headers
- Request body structure (sanitized)
- Base64 image length
- Response status and preview
- Detailed error messages with troubleshooting steps

## Usage Instructions

1. **Verify Custom API Configuration**:
   - Endpoint should match your API provider's requirements
   - API key must be provided
   - Model name should be specified (or will use fallback)
   - Check your provider's documentation for correct endpoint path

2. **Check Logs**:
   - Look for "MangaLearnJP" tag in Android Logcat
   - Console output shows detailed request/response flow
   - Error messages now include specific troubleshooting steps

3. **Test Sequence**:
   - Upload an image
   - Try analysis
   - Check logs for request details
   - Verify server receives proper OpenAI-format request

This should resolve the 404 error by ensuring the request is properly formatted and sent to the correct endpoint with all required parameters.
