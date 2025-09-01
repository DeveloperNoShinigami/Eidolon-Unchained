# API Retry System Implementation

## Overview
Implemented a comprehensive automatic retry system for Google Gemini API requests to handle temporary service overloads (503 errors). This addresses the "API is overloaded" errors that users were experiencing during peak usage times.

## Features Implemented

### 1. Automatic Retry Logic
- **Smart Error Detection**: Specifically targets 503 Service Unavailable errors and overload messages
- **Exponential Backoff**: Uses configurable backoff multiplier to avoid overwhelming the API
- **Configurable Attempts**: Default 3 attempts, configurable from 1-10
- **Graceful Fallbacks**: Only retries on overload errors, immediately fails on auth/rate limit errors

### 2. Configuration System
Added new configuration options to `EidolonUnchainedConfig.java`:

```toml
[ai_deities]
# Enable automatic retry for failed API requests (especially 503 overload errors)
enable_api_retry = true

# Maximum number of retry attempts for failed API requests
# Set to 1 to disable retries, recommended range: 2-5
max_retry_attempts = 3

# Base delay in milliseconds before first retry attempt
# Actual delays use exponential backoff (e.g., 2s, 4s, 8s)
retry_base_delay_ms = 2000

# Exponential backoff multiplier for retry delays
# Each retry waits: base_delay * (backoff^attempt)
retry_backoff_multiplier = 2.0
```

### 3. Enhanced Error Handling
Improved error messages for different API failure scenarios:
- **503 Overload**: "The divine channels are overwhelmed. Please try again in a moment."
- **401 Auth**: "The divine connection is misconfigured. Please check your sacred keys."
- **429 Rate Limit**: "You have prayed too often. Please wait before seeking divine wisdom again."
- **Generic**: "I cannot hear your prayer clearly right now. Please try again later."

### 4. Management Commands
Added new commands under `/eidolon-unchained api retry`:

```
/eidolon-unchained api retry status       # Show current retry configuration
/eidolon-unchained api retry toggle      # Show toggle instruction
/eidolon-unchained api retry attempts 5  # Show how to set max attempts
/eidolon-unchained api retry delay 3000  # Show how to set base delay
```

## Technical Implementation

### Code Changes

#### 1. GeminiAPIClient.java
- **New Method**: `sendRequestWithRetry()` - Handles the retry logic with exponential backoff
- **Enhanced**: `generateResponse()` - Now uses retry wrapper and provides specific error messages
- **Enhanced**: `sendRequest()` - Better error categorization for different HTTP status codes

#### 2. EidolonUnchainedConfig.java
- **New Fields**: `enableApiRetry`, `maxRetryAttempts`, `retryBaseDelayMs`, `retryBackoffMultiplier`
- **Configuration**: Added to AI deities section with proper comments and validation

#### 3. UnifiedCommands.java
- **New Commands**: Added retry management commands for configuration inspection
- **Fixed**: Commented out reputation commands that weren't implemented to resolve compilation errors

### Retry Algorithm
```java
for (attempt = 1; attempt <= maxRetries; attempt++) {
    try {
        return sendRequest(requestBody);
    } catch (IOException e) {
        if (is503Error && attempt < maxRetries) {
            delay = baseDelay * (backoffMultiplier ^ (attempt-1))
            sleep(delay)
            continue
        } else {
            throw e  // Not retriable or exhausted attempts
        }
    }
}
```

### Example Retry Schedule
With default settings (base=2000ms, multiplier=2.0, max=3):
- **Attempt 1**: Immediate
- **Attempt 2**: Wait 2 seconds (2000ms)
- **Attempt 3**: Wait 4 seconds (4000ms)
- **Total time**: Up to 6 seconds for complete failure

## Benefits

### 1. Improved User Experience
- Automatic recovery from temporary API overloads
- No manual retry required from players
- Contextual error messages explaining what's happening

### 2. Server Stability
- Configurable retry limits prevent API abuse
- Exponential backoff reduces server load during outages
- Graceful degradation when APIs are unavailable

### 3. Debugging Support
- Comprehensive logging of retry attempts
- Status commands for administrators
- Configuration validation and reporting

## Usage Instructions

### For Server Administrators
1. **Configure Retry Settings**: Edit `config/eidolonunchained-common.toml`
2. **Monitor Retry Status**: Use `/eidolon-unchained api retry status`
3. **Adjust Settings**: Modify config file and restart server

### For Players
- **Transparent Operation**: Retries happen automatically
- **Better Feedback**: Clear messages about what's happening
- **Faster Recovery**: Most overload situations resolve within seconds

## Testing Scenarios

### 1. Normal Operation
- API calls succeed on first attempt
- No retry logic triggered
- Standard response times

### 2. Temporary Overload
- First attempt returns 503
- Automatic retry after 2 seconds
- Success on second attempt
- Player sees brief delay but gets response

### 3. Extended Outage
- Multiple 503 errors
- Retries with exponential backoff
- Graceful failure after max attempts
- Clear error message to player

### 4. Authentication Issues
- 401 error on first attempt
- No retry (not retriable)
- Immediate error to administrator
- Clear configuration message

## Future Enhancements

### Potential Improvements
1. **Rate Limit Handling**: Add retry logic for 429 errors with proper delay parsing
2. **Circuit Breaker**: Temporarily disable API calls after multiple failures
3. **Health Monitoring**: Track API success rates and response times
4. **Dynamic Configuration**: Allow runtime configuration changes
5. **Jitter**: Add randomization to retry delays to prevent thundering herd

### Configuration Expansion
- Per-deity retry settings
- Time-based retry windows
- Success rate thresholds
- Health check intervals

## Conclusion

The retry system implementation significantly improves the reliability of AI deity interactions by automatically handling temporary API overloads. The configurable nature allows server administrators to tune the behavior for their specific needs while providing a seamless experience for players.

The system is production-ready and follows best practices for API retry logic with exponential backoff, proper error categorization, and comprehensive logging.
