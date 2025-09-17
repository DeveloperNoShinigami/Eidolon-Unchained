package com.bluelotuscoding.eidolonunchained.integration.voicechat;

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
    private static volatile Object serverApi; // ServerVoicechatApi for audio channels

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
     * Enhanced implementation with better audio format support and error handling.
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

            // Enhanced audio format detection and conversion
            short[] pcm = extractPcmFromAudioData(audioData);
            if (pcm == null) {
                LOGGER.debug("Could not extract PCM data from audio, format may be unsupported");
                return false;
            }

            // Create spatial audio channel at the player's position
            final Object lvl = player.level();
            final double x = player.getX();
            final double y = player.getY();
            final double z = player.getZ();

            LOGGER.debug("Creating spatial audio channel at position: {}, {}, {} for player: {}", 
                        x, y, z, player.getGameProfile().getName());

            Object serverLevel = reflectInvoke(serverApi, "fromServerLevel", new Class[]{Object.class}, new Object[]{lvl});
            if (serverLevel == null) {
                LOGGER.debug("Failed to get server level for spatial audio");
                return false;
            }

            Object pos = reflectInvoke(serverApi, "createPosition", new Class[]{double.class, double.class, double.class}, new Object[]{x, y, z});
            if (pos == null) {
                LOGGER.debug("Failed to create position for spatial audio");
                return false;
            }

            java.util.UUID channelId = java.util.UUID.randomUUID();
            Object channel = reflectInvoke(serverApi, "createLocationalAudioChannel",
                    new Class[]{java.util.UUID.class, serverLevel.getClass(), pos.getClass()},
                    new Object[]{channelId, serverLevel, pos});
            if (channel == null) {
                LOGGER.debug("Failed to create locational audio channel");
                return false;
            }

            // Set audio distance with enhanced fallback handling
            float effectiveDistance = Math.max(16.0f, Math.min(128.0f, volume * 64.0f));
            boolean distanceSet = reflectTryInvoke(channel, "setDistance", new Class[]{double.class}, new Object[]{(double)effectiveDistance})
                    || reflectTryInvoke(channel, "setDistance", new Class[]{int.class}, new Object[]{(int)effectiveDistance})
                    || reflectTryInvoke(channel, "setDistance", new Class[]{float.class}, new Object[]{effectiveDistance});
            
            if (!distanceSet) {
                LOGGER.debug("Could not set audio distance, using default");
            }

            // Apply volume scaling to PCM data if needed
            if (volume != 1.0f) {
                pcm = applyVolumeScaling(pcm, volume);
            }

            // Create encoder and audio player
            Object encoder = reflectInvoke(serverApi, "createEncoder", new Class[]{}, new Object[]{});
            if (encoder == null) {
                LOGGER.debug("Failed to create audio encoder");
                return false;
            }

            Object audioPlayer = reflectInvoke(serverApi, "createAudioPlayer",
                    new Class[]{channel.getClass(), encoder.getClass(), short[].class},
                    new Object[]{channel, encoder, pcm});
            if (audioPlayer == null) {
                LOGGER.debug("Failed to create audio player");
                return false;
            }

            // Start playing asynchronously
            boolean started = reflectTryInvoke(audioPlayer, "startPlaying", new Class[]{}, new Object[]{});
            if (started) {
                LOGGER.info("Started spatial TTS playback via Simple Voice Chat for {} ({} samples, volume: {}, distance: {})", 
                           player.getGameProfile().getName(), pcm.length, volume, effectiveDistance);
                
                // Schedule cleanup after estimated playback duration
                scheduleChannelCleanup(channelId, pcm.length, serverApi);
                return true;
            } else {
                LOGGER.debug("Failed to start audio playback");
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("Error during Simple Voice Chat spatial playback: {}", e.getMessage());
            return false;
        }
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

        // Try MP3 format detection and conversion
        if (data.length > 4 && data[0] == (byte) 0xFF && (data[1] & 0xE0) == 0xE0) {
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
     * Convert MP3 data to PCM using Java Sound API
     */
    private static short[] convertMp3ToPcm(byte[] mp3Data) {
        try {
            // Create input stream from MP3 data
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(mp3Data);
            javax.sound.sampled.AudioInputStream mp3Stream = javax.sound.sampled.AudioSystem.getAudioInputStream(bais);

            if (mp3Stream == null) {
                LOGGER.debug("AudioSystem could not create AudioInputStream from MP3 data");
                return null;
            }

            javax.sound.sampled.AudioFormat sourceFormat = mp3Stream.getFormat();
            LOGGER.debug("Source MP3 format: {} Hz, {} channels, {} bits",
                        sourceFormat.getSampleRate(), sourceFormat.getChannels(), sourceFormat.getSampleSizeInBits());

            // Define target PCM format (48kHz, 16-bit, mono/stereo as source)
            int targetChannels = Math.min(2, Math.max(1, sourceFormat.getChannels()));
            javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                48000.0f,  // 48kHz for VoiceChat
                16,        // 16-bit
                targetChannels,  // Mono or stereo
                targetChannels * 2,  // Frame size
                48000.0f,  // Frame rate
                false      // Little endian
            );

            // Convert to target format
            javax.sound.sampled.AudioInputStream pcmStream = javax.sound.sampled.AudioSystem.getAudioInputStream(targetFormat, mp3Stream);
            if (pcmStream == null) {
                LOGGER.debug("AudioSystem could not convert MP3 to target PCM format");
                mp3Stream.close();
                return null;
            }

            // Read PCM data
            java.io.ByteArrayOutputStream pcmBytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = pcmStream.read(buffer)) != -1) {
                pcmBytes.write(buffer, 0, bytesRead);
            }

            pcmStream.close();
            mp3Stream.close();

            byte[] pcmData = pcmBytes.toByteArray();
            if (pcmData.length == 0) {
                LOGGER.debug("No PCM data extracted from MP3");
                return null;
            }

            // Convert byte array to short array (16-bit samples)
            short[] samples = new short[pcmData.length / 2];
            for (int i = 0; i < samples.length; i++) {
                int byteIndex = i * 2;
                if (byteIndex + 1 < pcmData.length) {
                    // Little endian conversion
                    samples[i] = (short) ((pcmData[byteIndex] & 0xFF) | (pcmData[byteIndex + 1] << 8));
                }
            }

            LOGGER.info("Successfully converted MP3 to PCM: {} bytes -> {} samples ({}Hz, {} channels)",
                       mp3Data.length, samples.length, targetFormat.getSampleRate(), targetFormat.getChannels());
            return samples;

        } catch (javax.sound.sampled.UnsupportedAudioFileException e) {
            LOGGER.debug("MP3 format not supported by Java Sound API: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.debug("Error converting MP3 to PCM: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Schedule cleanup of audio channel after playback
     */
    private static void scheduleChannelCleanup(java.util.UUID channelId, int sampleCount, Object serverApi) {
        // Estimate playback duration (assuming 48kHz sample rate)
        long durationMs = (sampleCount * 1000L) / 48000L;
        long cleanupDelayMs = durationMs + 2000; // Add 2 seconds buffer
        
        // Schedule cleanup
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(cleanupDelayMs);
                // Try to remove/cleanup the channel
                reflectTryInvoke(serverApi, "removeAudioChannel", new Class[]{java.util.UUID.class}, new Object[]{channelId});
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        });
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
}
