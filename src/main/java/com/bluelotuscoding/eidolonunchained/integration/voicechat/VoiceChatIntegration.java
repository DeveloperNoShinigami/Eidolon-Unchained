package com.bluelotuscoding.eidolonunchained.integration.voicechat;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Enhanced integration with Simple Voice Chat for spatial TTS audio.
 * Provides 3D positioned audio playback for deity conversations.
 */
public class VoiceChatIntegration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile boolean available = false;
    private static volatile Object api; // Common VoiceChatApi (for builders, etc.)
    private static volatile Object serverApi; // VoicechatServerApi for audio channels

    public static void setAvailable(boolean flag) {
        available = flag;
    }

    public static boolean isAvailable() {
        return available;
    }

    public static void setApi(Object voicechatApi) {
        api = voicechatApi;
    }

    public static Object getApi() {
        return api;
    }

    public static void setServerApi(Object voicechatServerApi) {
        serverApi = voicechatServerApi;
    }

    public static Object getServerApi() {
        return serverApi;
    }

    /**
     * Try spatial playback via Simple Voice Chat if present.
     * Uses the typed SVC API directly (no reflection for channel/encoder/player creation).
     */
    public static boolean tryPlaySpatial(net.minecraft.server.level.ServerPlayer player,
                                         String audioUrl,
                                         byte[] audioData,
                                         float volume,
                                         float speed) {
        LOGGER.info("tryPlaySpatial called - available: {}, serverApi: {}, audioUrl: {}, audioData length: {}",
                   isAvailable(), serverApi != null, audioUrl != null, audioData != null ? audioData.length : 0);

        if (!isAvailable() || serverApi == null) {
            LOGGER.warn("Simple Voice Chat not available (available: {}) or serverApi is null ({})",
                       isAvailable(), serverApi != null);
            return false;
        }

        if (!(serverApi instanceof VoicechatServerApi)) {
            LOGGER.warn("SVC spatial: serverApi is not a VoicechatServerApi (actual: {})", serverApi.getClass().getName());
            return false;
        }

        VoicechatServerApi svcApi = (VoicechatServerApi) serverApi;

        try {
            // If we have URL but no data, try to download it first
            if (audioUrl != null && (audioData == null || audioData.length == 0)) {
                LOGGER.info("Attempting to download audio from URL for spatial playback: {}", audioUrl);
                audioData = downloadAudioForSpatialPlayback(audioUrl);
                if (audioData == null) {
                    LOGGER.warn("Failed to download audio data from URL, falling back to client packet");
                    return false;
                }
                LOGGER.info("Successfully downloaded {} bytes of audio data from URL", audioData.length);
            }

            if (audioData == null || audioData.length == 0) {
                LOGGER.debug("No audio data available for spatial playback");
                return false;
            }

            // Decode audio to 48kHz mono PCM (required by SVC)
            short[] pcm = extractPcmFromAudioData(audioData);
            if (pcm == null) {
                LOGGER.warn("SVC spatial: could not extract PCM data from audio (format unsupported)");
                return false;
            }

            final double x = player.getX();
            final double y = player.getY();
            final double z = player.getZ();

            LOGGER.debug("Creating SVC locational audio channel at ({}, {}, {}) for player: {}",
                        x, y, z, player.getGameProfile().getName());

            // Wrap Minecraft ServerLevel → SVC ServerLevel
            ServerLevel svcLevel = svcApi.fromServerLevel(player.level());
            if (svcLevel == null) {
                LOGGER.warn("SVC spatial: fromServerLevel returned null");
                return false;
            }

            // Build SVC Position
            Position pos = svcApi.createPosition(x, y, z);

            // Create locational channel
            java.util.UUID channelId = java.util.UUID.randomUUID();
            LocationalAudioChannel channel = svcApi.createLocationalAudioChannel(channelId, svcLevel, pos);
            if (channel == null) {
                LOGGER.warn("SVC spatial: createLocationalAudioChannel returned null");
                return false;
            }

            // Set hearing distance (default 16, scale with volume up to 128)
            float effectiveDistance = Math.max(16.0f, Math.min(128.0f, volume * 64.0f));
            channel.setDistance(effectiveDistance);

            // Apply volume scaling to PCM
            if (volume != 1.0f) {
                pcm = applyVolumeScaling(pcm, volume);
            }

            // Chunk PCM into 960-sample frames (48kHz, 20ms) as required by Opus
            final short[] pcmFinal = padToFrameBoundary(pcm, 960);
            final int frameSize = 960;

            // Create encoder
            OpusEncoder encoder = svcApi.createEncoder();
            if (encoder == null) {
                LOGGER.warn("SVC spatial: createEncoder returned null");
                return false;
            }

            // Use Supplier<short[]> overload — feeds one 960-sample frame at a time
            final int[] frameIndex = {0};
            final int totalFrames = pcmFinal.length / frameSize;
            AudioPlayer audioPlayer = svcApi.createAudioPlayer(channel, encoder, () -> {
                int idx = frameIndex[0]++;
                if (idx >= totalFrames) return null; // signals end of audio
                short[] frame = new short[frameSize];
                System.arraycopy(pcmFinal, idx * frameSize, frame, 0, frameSize);
                return frame;
            });

            // Clean up encoder when playback stops
            audioPlayer.setOnStopped(() -> {
                encoder.close();
                LOGGER.debug("SVC spatial: encoder closed after playback for {}", player.getGameProfile().getName());
            });

            audioPlayer.startPlaying();

            LOGGER.info("Started SVC spatial TTS for {} ({} samples / {} frames, distance: {})",
                       player.getGameProfile().getName(), pcmFinal.length, totalFrames, effectiveDistance);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error during Simple Voice Chat spatial playback: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Pad PCM array to a multiple of frameSize so the supplier never gets a short frame.
     */
    private static short[] padToFrameBoundary(short[] pcm, int frameSize) {
        int remainder = pcm.length % frameSize;
        if (remainder == 0) return pcm;
        short[] padded = new short[pcm.length + (frameSize - remainder)];
        System.arraycopy(pcm, 0, padded, 0, pcm.length);
        return padded;
    }

    /**
     * Download audio data from URL for spatial playback
     */
    private static byte[] downloadAudioForSpatialPlayback(String url) {
        try {
            java.net.URL audioUrl = new java.net.URL(url);
            java.net.URLConnection connection = audioUrl.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "EidolonUnchained/1.0");
            
            try (java.io.InputStream is = connection.getInputStream();
                 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                
                byte[] data = baos.toByteArray();
                LOGGER.debug("Downloaded {} bytes from audio URL", data.length);
                return data;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to download audio from URL {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Enhanced PCM extraction supporting multiple audio formats
     */
    private static short[] extractPcmFromAudioData(byte[] data) {
        // First, check if this might be raw LINEAR16 PCM data from Gemini TTS
        // Gemini returns raw PCM at 24kHz with no headers when requested as LINEAR16
        if (!hasAudioHeaders(data) && data.length > 1000 && data.length % 2 == 0) {
            LOGGER.info("Detected raw LINEAR16 PCM data (no headers), assuming 24kHz from Gemini TTS");
            // Convert bytes to short array (16-bit samples, little endian)
            short[] samples = new short[data.length / 2];
            for (int i = 0; i < samples.length; i++) {
                int byteIndex = i * 2;
                // Little endian: low byte first, high byte second
                samples[i] = (short) ((data[byteIndex] & 0xFF) | ((data[byteIndex + 1] & 0xFF) << 8));
            }
            
            // Resample from 24kHz to 48kHz for Simple Voice Chat
            LOGGER.info("Resampling raw LINEAR16 from 24kHz to 48kHz: {} samples", samples.length);
            short[] resampled = resampleAudio(samples, 24000.0f, 48000.0f, 1); // Assume mono
            LOGGER.info("Successfully processed raw LINEAR16: {} -> {} samples", samples.length, resampled.length);
            return resampled;
        }
        
        // First, analyze the actual WAV header to understand what we're dealing with
        WavInfo info = analyzeWavHeader(data);
        if (info != null) {
            LOGGER.info("WAV audio format detected: {}Hz, {} channels, {} bits",
                       info.sampleRate, info.channels, info.bitsPerSample);

            short[] pcm = extractPcm16FromWav(data);
            if (pcm != null) {
                // Handle different sample rates
                if (info.sampleRate == 48000) {
                    LOGGER.info("Perfect match: 48kHz WAV, no resampling needed");
                    return pcm;
                } else if (info.sampleRate == 44100) {
                    LOGGER.info("Resampling from 44.1kHz to 48kHz");
                    return resample44to48kHz(pcm);
                } else if (info.sampleRate == 22050) {
                    LOGGER.info("Resampling from 22.05kHz to 48kHz");
                    return resample22to48kHz(pcm);
                } else if (info.sampleRate == 24000) {
                    LOGGER.info("Resampling from 24kHz to 48kHz");
                    return resample24to48kHz(pcm);
                } else {
                    LOGGER.warn("Unsupported sample rate: {}Hz, will try generic resampling", info.sampleRate);
                    return resampleGeneric(pcm, info.sampleRate, 48000);
                }
            }
        }

        // Legacy code for backward compatibility
        // Try WAV format first (most common)
        if (isWavPcm16Stereo48k(data) || isWavPcm16Mono48k(data)) {
            return extractPcm16FromWav(data);
        }

        // Try other WAV formats with different sample rates
        if (isWavHeader(data, 1, 44100, 16) || isWavHeader(data, 2, 44100, 16)) {
            short[] pcm = extractPcm16FromWav(data);
            if (pcm != null) {
                // Convert from 44.1kHz to 48kHz if needed
                return resample44to48kHz(pcm);
            }
        }

        // Try MP3 format detection and conversion - Enhanced detection
        if (isMp3Data(data)) {
            LOGGER.info("MP3 format detected, attempting conversion to PCM");
            short[] pcm = convertMp3ToPcm(data);
            if (pcm != null) {
                LOGGER.info("Successfully converted MP3 to PCM: {} samples", pcm.length);
                return pcm;
            } else {
                LOGGER.warn("Failed to convert MP3 to PCM");
            }
        }

        // Try OGG format detection
        if (data.length > 4 && data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S') {
            LOGGER.debug("OGG format detected but conversion not implemented");
            // OGG decoding would require external library
            return null;
        }

        // Fallback: Try to treat unknown data as MP3 (common case for Gemini API)
        if (data.length > 100) { // Only try if we have substantial data
            LOGGER.info("Unknown audio format, attempting MP3 conversion as fallback");
            short[] pcm = convertMp3ToPcm(data);
            if (pcm != null) {
                LOGGER.info("Fallback MP3 conversion successful: {} samples", pcm.length);
                return pcm;
            }
        }

        // Last resort: try to interpret as raw PCM
        return tryInterpretAsLittleEndianPcm(data);
    }

    /**
     * Apply volume scaling to PCM data
     */
    private static short[] applyVolumeScaling(short[] pcm, float volume) {
        if (volume == 1.0f || pcm == null) {
            return pcm;
        }
        
        short[] scaled = new short[pcm.length];
        float clampedVolume = Math.max(0.0f, Math.min(2.0f, volume));
        
        for (int i = 0; i < pcm.length; i++) {
            int scaled_sample = Math.round(pcm[i] * clampedVolume);
            scaled[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaled_sample));
        }
        
        return scaled;
    }

    /**
     * Simple resampling from 44.1kHz to 48kHz using linear interpolation
     */
    private static short[] resample44to48kHz(short[] input) {
        if (input == null) return null;
        
        // Simple linear interpolation resampling
        double ratio = 48000.0 / 44100.0;
        int outputLength = (int) (input.length * ratio);
        short[] output = new short[outputLength];
        
        for (int i = 0; i < outputLength; i++) {
            double sourceIndex = i / ratio;
            int index1 = (int) sourceIndex;
            int index2 = Math.min(index1 + 1, input.length - 1);
            double fraction = sourceIndex - index1;
            
            if (index1 < input.length) {
                double interpolated = input[index1] * (1.0 - fraction) + input[index2] * fraction;
                output[i] = (short) Math.round(interpolated);
            }
        }
        
        return output;
    }

    /**
     * Enhanced MP3 format detection
     * Handles various MP3 formats including those with metadata or ID3 tags
     */
    private static boolean isMp3Data(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }

        // Check for MP3 frame header (standard detection)
        if (data[0] == (byte) 0xFF && (data[1] & 0xE0) == 0xE0) {
            return true;
        }

        // Check for ID3v2 tag followed by MP3 data
        if (data.length >= 10 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            // ID3v2 header found, look for MP3 frame after tag
            try {
                // ID3v2 size is in bytes 6-9 (syncsafe integer)
                int tagSize = ((data[6] & 0x7F) << 21) | ((data[7] & 0x7F) << 14) | 
                             ((data[8] & 0x7F) << 7) | (data[9] & 0x7F);
                int frameStart = 10 + tagSize; // Header (10 bytes) + tag size
                
                if (frameStart < data.length - 1 && 
                    data[frameStart] == (byte) 0xFF && (data[frameStart + 1] & 0xE0) == 0xE0) {
                    return true;
                }
            } catch (Exception e) {
                // If ID3 parsing fails, fall through to other checks
            }
        }

        // Try Java Sound API to definitively identify MP3
        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
            javax.sound.sampled.AudioInputStream stream = javax.sound.sampled.AudioSystem.getAudioInputStream(bais);
            if (stream != null) {
                javax.sound.sampled.AudioFormat format = stream.getFormat();
                stream.close();
                // Check if format encoding suggests MP3/MPEG
                String encoding = format.getEncoding().toString().toLowerCase();
                return encoding.contains("mp3") || encoding.contains("mpeg");
            }
        } catch (Exception e) {
            // Not a valid audio format that Java can recognize
        }

        return false;
    }

    /**
     * Convert MP3 data to PCM using Simple Voice Chat's native MP3 decoder
     * This is much more reliable than Java Sound API for MP3 files
     */
    private static short[] convertMp3ToPcm(byte[] mp3Data) {
        if (serverApi == null) {
            LOGGER.warn("Simple Voice Chat API not available for MP3 decoding");
            return null;
        }

        try (java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(mp3Data)) {
            // Use Simple Voice Chat's native MP3 decoder
            Object mp3Decoder = reflectInvoke(serverApi, "createMp3Decoder", 
                                            new Class[]{java.io.InputStream.class}, 
                                            new Object[]{inputStream});
            
            if (mp3Decoder == null) {
                LOGGER.warn("Failed to create MP3 decoder from Simple Voice Chat API");
                return null;
            }

            // Decode MP3 to PCM samples
            short[] pcmSamples = (short[]) reflectInvoke(mp3Decoder, "decode", new Class[]{}, new Object[]{});
            
            if (pcmSamples != null) {
                // Get audio format to check sample rate
                Object audioFormat = reflectInvoke(mp3Decoder, "getAudioFormat", new Class[]{}, new Object[]{});
                if (audioFormat != null) {
                    float sampleRate = (float) reflectInvoke(audioFormat, "getSampleRate", new Class[]{}, new Object[]{});
                    int channels = (int) reflectInvoke(audioFormat, "getChannels", new Class[]{}, new Object[]{});
                    LOGGER.debug("MP3 decoded: {} samples, {}Hz, {} channels", pcmSamples.length, sampleRate, channels);
                    
                    // If sample rate is not 48kHz, resample it
                    if (Math.abs(sampleRate - 48000.0f) > 1.0f) {
                        LOGGER.debug("Resampling from {}Hz to 48kHz", sampleRate);
                        pcmSamples = resampleAudio(pcmSamples, sampleRate, 48000.0f, channels);
                    }
                }
                
                LOGGER.info("Successfully decoded MP3 using Simple Voice Chat: {} samples", pcmSamples.length);
                return pcmSamples;
            } else {
                LOGGER.warn("MP3 decoder returned null PCM samples");
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to decode MP3 using Simple Voice Chat decoder: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Fallback method to try treating data as raw PCM
     */
    private static short[] tryRawAudioConversion(byte[] audioData) {
        LOGGER.debug("Attempting raw audio conversion as fallback");
        try {
            // Assume 16-bit samples, convert to shorts
            short[] samples = new short[audioData.length / 2];
            for (int i = 0; i < samples.length; i++) {
                int byteIndex = i * 2;
                if (byteIndex + 1 < audioData.length) {
                    // Little endian conversion
                    samples[i] = (short) ((audioData[byteIndex] & 0xFF) | (audioData[byteIndex + 1] << 8));
                }
            }
            LOGGER.debug("Raw audio conversion produced {} samples", samples.length);
            return samples;
        } catch (Exception e) {
            LOGGER.warn("Raw audio conversion failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * WAV file information holder
     */
    private static class WavInfo {
        final int sampleRate;
        final int channels;
        final int bitsPerSample;

        WavInfo(int sampleRate, int channels, int bitsPerSample) {
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.bitsPerSample = bitsPerSample;
        }
    }

    /**
     * Analyze WAV header to extract format information
     */
    private static WavInfo analyzeWavHeader(byte[] data) {
        if (data == null || data.length < 44) return null;

        // Check RIFF/WAVE header
        if (data[0] != 'R' || data[1] != 'I' || data[2] != 'F' || data[3] != 'F') return null;
        if (data[8] != 'W' || data[9] != 'A' || data[10] != 'V' || data[11] != 'E') return null;

        // Find 'fmt ' chunk
        int idx = 12;
        while (idx + 8 <= data.length) {
            if (data[idx] == 'f' && data[idx + 1] == 'm' && data[idx + 2] == 't' && data[idx + 3] == ' ') {
                int fmtLen = toIntLE(data, idx + 4);
                if (idx + 8 + fmtLen > data.length) return null;

                int audioFormat = toShortLE(data, idx + 8) & 0xFFFF;
                int channels = toShortLE(data, idx + 10) & 0xFFFF;
                int sampleRate = toIntLE(data, idx + 12);
                int bitsPerSample = toShortLE(data, idx + 22) & 0xFFFF;

                if (audioFormat == 1) { // PCM format
                    return new WavInfo(sampleRate, channels, bitsPerSample);
                }
                return null;
            }
            idx += 1;
        }
        return null;
    }

    /**
     * Resample from 22.05kHz to 48kHz using linear interpolation
     */
    private static short[] resample22to48kHz(short[] input) {
        if (input == null) return null;

        double ratio = 48000.0 / 22050.0; // approximately 2.176
        int outputLength = (int) (input.length * ratio);
        short[] output = new short[outputLength];

        for (int i = 0; i < outputLength; i++) {
            double sourceIndex = i / ratio;
            int index1 = (int) sourceIndex;
            int index2 = Math.min(index1 + 1, input.length - 1);

            if (index1 < input.length) {
                double fraction = sourceIndex - index1;
                double sample = input[index1] + fraction * (input[index2] - input[index1]);
                output[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
            }
        }

        LOGGER.debug("Resampled {} samples (22.05kHz) to {} samples (48kHz)", input.length, output.length);
        return output;
    }

    /**
     * Resample from 24kHz to 48kHz (exact 2x upsampling)
     */
    private static short[] resample24to48kHz(short[] input) {
        if (input == null) return null;

        // 24kHz to 48kHz is exactly 2x, so we can use simple interpolation
        short[] output = new short[input.length * 2];

        for (int i = 0; i < input.length - 1; i++) {
            output[i * 2] = input[i];
            // Linear interpolation between samples
            output[i * 2 + 1] = (short) ((input[i] + input[i + 1]) / 2);
        }

        // Handle the last sample
        if (input.length > 0) {
            output[output.length - 2] = input[input.length - 1];
            output[output.length - 1] = input[input.length - 1];
        }

        LOGGER.debug("Resampled {} samples (24kHz) to {} samples (48kHz)", input.length, output.length);
        return output;
    }

    /**
     * Generic resampling using linear interpolation
     */
    private static short[] resampleGeneric(short[] input, int fromRate, int toRate) {
        if (input == null || fromRate <= 0 || toRate <= 0) return null;

        double ratio = (double) toRate / fromRate;
        int outputLength = (int) (input.length * ratio);
        short[] output = new short[outputLength];

        for (int i = 0; i < outputLength; i++) {
            double sourceIndex = i / ratio;
            int index1 = (int) sourceIndex;
            int index2 = Math.min(index1 + 1, input.length - 1);

            if (index1 < input.length) {
                double fraction = sourceIndex - index1;
                double sample = input[index1] + fraction * (input[index2] - input[index1]);
                output[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
            }
        }

        LOGGER.debug("Resampled {} samples ({}Hz) to {} samples ({}Hz)",
                    input.length, fromRate, output.length, toRate);
        return output;
    }

    // --- Helpers: reflection and minimal WAV parsing ---

    private static Object reflectInvoke(Object target, String method, Class<?>[] types, Object[] args) {
        try {
            java.lang.reflect.Method m = findMethod(target.getClass(), method, types);
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable t) {
            LOGGER.debug("VoiceChat reflectInvoke {} failed: {}", method, t.getMessage());
            return null;
        }
    }

    private static boolean reflectTryInvoke(Object target, String method, Class<?>[] types, Object[] args) {
        try {
            java.lang.reflect.Method m = findMethod(target.getClass(), method, types);
            if (m == null) return false;
            m.setAccessible(true);
            m.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static java.lang.reflect.Method findMethod(Class<?> cls, String name, Class<?>[] types) {
        try {
            return cls.getMethod(name, types);
        } catch (NoSuchMethodException e) {
            // Try to find compatible method by name and arg count
            for (java.lang.reflect.Method m : cls.getMethods()) {
                if (m.getName().equals(name) && (types == null || m.getParameterCount() == types.length)) {
                    return m;
                }
            }
            return null;
        }
    }

    private static boolean isWavPcm16Stereo48k(byte[] data) {
        return isWavHeader(data, 2, 48000, 16);
    }

    private static boolean isWavPcm16Mono48k(byte[] data) {
        return isWavHeader(data, 1, 48000, 16);
    }

    private static boolean isWavHeader(byte[] data, int channels, int sampleRate, int bitsPerSample) {
        if (data == null || data.length < 44) return false;
        // Check RIFF/WAVE
        if (data[0] != 'R' || data[1] != 'I' || data[2] != 'F' || data[3] != 'F') return false;
        if (data[8] != 'W' || data[9] != 'A' || data[10] != 'V' || data[11] != 'E') return false;
        // fmt chunk usually at 12
        // Find 'fmt ' chunk
        int idx = 12;
        while (idx + 8 <= data.length) {
            if (data[idx] == 'f' && data[idx + 1] == 'm' && data[idx + 2] == 't' && data[idx + 3] == ' ') {
                int fmtLen = toIntLE(data, idx + 4);
                if (idx + 8 + fmtLen > data.length) return false;
                int audioFormat = toShortLE(data, idx + 8) & 0xFFFF;
                int ch = toShortLE(data, idx + 10) & 0xFFFF;
                int sr = toIntLE(data, idx + 12);
                int bps = toShortLE(data, idx + 22) & 0xFFFF;
                return audioFormat == 1 && ch == channels && sr == sampleRate && bps == bitsPerSample;
            }
            idx += 1;
        }
        return false;
    }

    private static short[] extractPcm16FromWav(byte[] data) {
        // Locate data chunk
        int idx = 12;
        int dataOffset = -1;
        int dataSize = -1;
        while (idx + 8 <= data.length) {
            int chunkId = toIntBE(data, idx);
            int size = toIntLE(data, idx + 4);
            if (chunkId == 0x64617461) { // 'data'
                dataOffset = idx + 8;
                dataSize = Math.min(size, data.length - dataOffset);
                break;
            }
            idx += 8 + size;
        }
        if (dataOffset < 0 || dataSize <= 0) return null;
        int samples = dataSize / 2;
        short[] pcm = new short[samples];
        int p = dataOffset;
        for (int i = 0; i < samples && p + 1 < data.length; i++, p += 2) {
            pcm[i] = (short) ((data[p] & 0xFF) | (data[p + 1] << 8));
        }
        return pcm;
    }

    private static short[] tryInterpretAsLittleEndianPcm(byte[] data) {
        if (data.length % 2 != 0) return null;
        short[] pcm = new short[data.length / 2];
        int p = 0;
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = (short) ((data[p] & 0xFF) | (data[p + 1] << 8));
            p += 2;
        }
        return pcm;
    }

    private static int toIntLE(byte[] b, int off) {
        if (off + 3 >= b.length) return 0;
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static int toIntBE(byte[] b, int off) {
        if (off + 3 >= b.length) return 0;
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16) | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    private static short toShortLE(byte[] b, int off) {
        if (off + 1 >= b.length) return 0;
        return (short) ((b[off] & 0xFF) | (b[off + 1] << 8));
    }

    /**
     * Check if audio data has recognizable format headers (WAV, MP3, OGG, etc.)
     * Returns false for raw PCM data with no headers
     */
    private static boolean hasAudioHeaders(byte[] data) {
        if (data.length < 4) return false;
        
        // Check for WAV header
        if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') {
            return true;
        }
        
        // Check for MP3 header (various forms)
        if (isMp3Data(data)) {
            return true;
        }
        
        // Check for OGG header
        if (data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S') {
            return true;
        }
        
        // Check for FLAC header
        if (data.length > 4 && data[0] == 'f' && data[1] == 'L' && data[2] == 'a' && data[3] == 'C') {
            return true;
        }
        
        return false;
    }

    /**
     * Simple linear interpolation resampling for audio data
     * Converts from source sample rate to target sample rate
     */
    private static short[] resampleAudio(short[] samples, float sourceSampleRate, float targetSampleRate, int channels) {
        if (Math.abs(sourceSampleRate - targetSampleRate) < 1.0f) {
            return samples; // No resampling needed
        }

        double ratio = sourceSampleRate / targetSampleRate;
        int targetLength = (int) (samples.length / ratio);
        short[] resampled = new short[targetLength];

        for (int i = 0; i < targetLength; i++) {
            double sourceIndex = i * ratio;
            int index = (int) sourceIndex;
            
            if (index + 1 < samples.length) {
                // Linear interpolation
                double fraction = sourceIndex - index;
                double sample1 = samples[index];
                double sample2 = samples[index + 1];
                resampled[i] = (short) (sample1 + fraction * (sample2 - sample1));
            } else if (index < samples.length) {
                resampled[i] = samples[index];
            }
        }

        LOGGER.debug("Resampled audio from {}Hz to {}Hz: {} -> {} samples", 
                    sourceSampleRate, targetSampleRate, samples.length, resampled.length);
        return resampled;
    }
}
