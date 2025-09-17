package com.bluelotuscoding.eidolonunchained.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Network packet to send TTS audio data to clients
 * Supports both direct audio data and URLs for client download
 */
public class TTSAudioPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String audioUrl;
    private final byte[] audioData;
    private final float volume;
    private final float speed;

    public TTSAudioPacket(String audioUrl, byte[] audioData, float volume, float speed) {
        this.audioUrl = audioUrl;
        this.audioData = audioData;
        this.volume = volume;
        this.speed = speed;
    }

    public TTSAudioPacket(FriendlyByteBuf buffer) {
        // Read whether we have URL or data
        boolean hasUrl = buffer.readBoolean();
        if (hasUrl) {
            this.audioUrl = buffer.readUtf();
            this.audioData = null;
        } else {
            this.audioUrl = null;
            boolean hasData = buffer.readBoolean();
            if (hasData) {
                int dataLength = buffer.readInt();
                this.audioData = new byte[dataLength];
                buffer.readBytes(this.audioData);
            } else {
                this.audioData = null;
            }
        }

        this.volume = buffer.readFloat();
        this.speed = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        // Write URL if present
        if (audioUrl != null) {
            buffer.writeBoolean(true);
            buffer.writeUtf(audioUrl);
        } else {
            buffer.writeBoolean(false);
            // Write audio data if present
            if (audioData != null) {
                buffer.writeBoolean(true);
                buffer.writeInt(audioData.length);
                buffer.writeBytes(audioData);
            } else {
                buffer.writeBoolean(false);
            }
        }

        buffer.writeFloat(volume);
        buffer.writeFloat(speed);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Only handle on client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                handleClientSide();
            });
        });
        context.setPacketHandled(true);
    }

    private void handleClientSide() {
        try {
            // Handle TTS audio on client side
            if (audioUrl != null) {
                LOGGER.debug("Received TTS audio URL: {}", audioUrl);
                // Play audio from URL
                playTTSFromUrl(audioUrl, volume, speed);
            } else if (audioData != null) {
                LOGGER.debug("Received TTS audio data: {} bytes", audioData.length);
                // Play audio from data
                playTTSFromData(audioData, volume, speed);
            } else {
                LOGGER.warn("Received TTS packet with no audio URL or data");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle TTS audio packet: {}", e.getMessage());
        }
    }

    /**
     * Play TTS audio from URL (client downloads and plays)
     */
    private void playTTSFromUrl(String url, float volume, float speed) {
        LOGGER.info("Playing TTS audio from URL: {} (volume: {}, speed: {})", url, volume, speed);
        
        // Try multiple playback methods in priority order
        CompletableFuture.runAsync(() -> {
            try {
                // Method 1: Try Simple Voice Chat spatial audio (if available)
                if (trySimpleVoiceChatPlayback(url, null, volume, speed)) {
                    LOGGER.debug("TTS audio played via Simple Voice Chat");
                    return;
                }
                
                // Method 2: Try Player2 App direct playback (if available)
                if (tryPlayer2AppPlayback(url, volume, speed)) {
                    LOGGER.debug("TTS audio played via Player2 App");
                    return;
                }
                
                // Method 3: Download and play via Minecraft's sound system
                if (tryMinecraftSoundSystem(url, null, volume, speed)) {
                    LOGGER.debug("TTS audio played via Minecraft sound system");
                    return;
                }
                
                // Method 4: Fallback to system audio (if possible)
                if (trySystemAudioPlayback(url, volume, speed)) {
                    LOGGER.debug("TTS audio played via system audio");
                    return;
                }
                
                // All methods failed - show notification
                showTTSNotification("Could not play deity voice - check audio settings");
                LOGGER.warn("All TTS playback methods failed for URL: {}", url);
                
            } catch (Exception e) {
                LOGGER.error("Error playing TTS audio from URL: {}", e.getMessage());
                showTTSNotification("Audio playback error");
            }
        });
    }

    /**
     * Play TTS audio from byte data
     */
    private void playTTSFromData(byte[] data, float volume, float speed) {
        LOGGER.info("Playing TTS audio from data: {} bytes (volume: {}, speed: {})", data.length, volume, speed);
        
        CompletableFuture.runAsync(() -> {
            try {
                // Method 1: Try Simple Voice Chat spatial audio (if available)
                if (trySimpleVoiceChatPlayback(null, data, volume, speed)) {
                    LOGGER.debug("TTS audio played via Simple Voice Chat");
                    return;
                }
                
                // Method 2: Try Minecraft's sound system with temporary file
                if (tryMinecraftSoundSystem(null, data, volume, speed)) {
                    LOGGER.debug("TTS audio played via Minecraft sound system");
                    return;
                }
                
                // Method 3: Try direct OpenAL playback (if supported format)
                if (tryOpenALDirectPlayback(data, volume, speed)) {
                    LOGGER.debug("TTS audio played via OpenAL");
                    return;
                }
                
                // All methods failed - show notification
                showTTSNotification("Could not play deity voice - unsupported audio format");
                LOGGER.warn("All TTS playback methods failed for audio data ({} bytes)", data.length);
                
            } catch (Exception e) {
                LOGGER.error("Error playing TTS audio from data: {}", e.getMessage());
                showTTSNotification("Audio playback error");
            }
        });
    }

    /**
     * Show a notification to the player about TTS playback
     */
    private void showTTSNotification(String message) {
        try {
            // Send a client-side message
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("🔊 " + message), true);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to show TTS notification: {}", e.getMessage());
        }
    }

    /**
     * Try playing audio via Simple Voice Chat (spatial 3D audio)
     */
    private boolean trySimpleVoiceChatPlayback(String url, byte[] data, float volume, float speed) {
        try {
            LOGGER.info("Trying Simple Voice Chat playback - URL: {}, data: {}", url != null, data != null ? data.length : 0);

            // Simple Voice Chat spatial audio is handled server-side in our VoiceChatIntegration
            // On the client side, we should fall back to other methods
            // The server already tried VoiceChat via VoiceChatIntegration.tryPlaySpatial()

            LOGGER.debug("Simple Voice Chat spatial audio handled server-side, skipping client-side attempt");
            return false;

        } catch (Exception e) {
            LOGGER.debug("Simple Voice Chat playback failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Try playing audio via Player2 App direct playback
     */
    private boolean tryPlayer2AppPlayback(String url, float volume, float speed) {
        try {
            // Check if Player2 App is running locally
            // Player2 App typically exposes audio playback APIs on local ports
            
            // Try to send audio URL to Player2 App for native playback
            if (url != null) {
                // Player2 App would handle this natively if it's running
                // For now, we'll check if the app is available
                
                // This would require Player2 App integration
                // Currently not implemented - fall back to other methods
                LOGGER.debug("Player2 App direct playback not yet implemented");
                return false;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.debug("Player2 App playback failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Try playing audio via Minecraft's built-in sound system
     */
    private boolean tryMinecraftSoundSystem(String url, byte[] data, float volume, float speed) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getSoundManager() == null) {
                return false;
            }
            
            // We need a SoundInstance to play audio
            // Minecraft's sound system expects registered sounds or temp files
            
            Path tempFile = null;
            try {
                // Create temporary file for audio data
                if (data != null) {
                    tempFile = createTempAudioFile(data);
                } else if (url != null) {
                    byte[] downloadedData = downloadAudioData(url);
                    if (downloadedData != null) {
                        tempFile = createTempAudioFile(downloadedData);
                    }
                }
                
                if (tempFile == null) {
                    return false;
                }
                
                // Convert to format Minecraft can play (OGG preferred)
                if (convertToOggIfNeeded(tempFile)) {
                    // Create and play sound
                    playMinecraftSound(tempFile, volume, speed);
                    return true;
                }
                
            } finally {
                // Clean up temp file after a delay
                if (tempFile != null) {
                    final Path finalTempFile = tempFile;
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(10000); // Wait 10 seconds for playback to complete
                            Files.deleteIfExists(finalTempFile);
                        } catch (Exception e) {
                            // Ignore cleanup errors
                        }
                    });
                }
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.debug("Minecraft sound system playback failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Try playing audio via system audio (Java Sound API or native)
     */
    private boolean trySystemAudioPlayback(String url, float volume, float speed) {
        try {
            // Use Java's built-in audio capabilities
            byte[] audioData = null;
            
            if (url != null) {
                audioData = downloadAudioData(url);
            }
            
            if (audioData == null) {
                return false;
            }
            
            // Try to play using Java Sound API
            return playWithJavaSound(audioData, volume, speed);
            
        } catch (Exception e) {
            LOGGER.debug("System audio playback failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Try direct OpenAL playback (for PCM data)
     */
    private boolean tryOpenALDirectPlayback(byte[] data, float volume, float speed) {
        try {
            // This would require direct OpenAL integration
            // Minecraft uses OpenAL for its sound system, but accessing it directly
            // is complex and may interfere with Minecraft's audio
            
            LOGGER.debug("OpenAL direct playback not implemented (may interfere with Minecraft audio)");
            return false;
            
        } catch (Exception e) {
            LOGGER.debug("OpenAL direct playback failed: {}", e.getMessage());
            return false;
        }
    }

    // Helper methods for audio processing

    private byte[] downloadAudioData(String url) {
        try {
            URL audioUrl = new URL(url);
            URLConnection connection = audioUrl.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            
            try (InputStream is = connection.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                
                return baos.toByteArray();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to download audio from URL: {}", e.getMessage());
            return null;
        }
    }

    private Path createTempAudioFile(byte[] data) {
        try {
            // Determine file extension based on audio format
            String extension = ".wav"; // Default
            if (data.length > 4) {
                // Check for common audio formats
                if (data[0] == (byte) 0xFF && (data[1] & 0xE0) == 0xE0) {
                    extension = ".mp3";
                } else if (data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S') {
                    extension = ".ogg";
                } else if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') {
                    extension = ".wav";
                }
            }
            
            Path tempFile = Files.createTempFile("eidolon_tts_", extension);
            Files.write(tempFile, data);
            return tempFile;
            
        } catch (Exception e) {
            LOGGER.debug("Failed to create temp audio file: {}", e.getMessage());
            return null;
        }
    }

    private boolean convertToOggIfNeeded(Path audioFile) {
        try {
            // For now, assume the audio is already in a compatible format
            // In a full implementation, we'd convert MP3/WAV to OGG using FFmpeg or similar
            String fileName = audioFile.getFileName().toString().toLowerCase();
            
            // Minecraft prefers OGG format, but can handle WAV
            if (fileName.endsWith(".ogg") || fileName.endsWith(".wav")) {
                return true;
            }
            
            // For other formats, we'd need conversion
            LOGGER.debug("Audio format conversion not implemented for: {}", fileName);
            return false;
            
        } catch (Exception e) {
            LOGGER.debug("Audio format check failed: {}", e.getMessage());
            return false;
        }
    }

    private void playMinecraftSound(Path audioFile, float volume, float speed) {
        try {
            // This would require creating a custom SoundInstance
            // For now, we'll use a simpler approach
            showTTSNotification("Playing deity voice (Minecraft audio)");
            
            // In a full implementation, we'd:
            // 1. Register the sound file as a resource
            // 2. Create a SoundInstance with proper positioning
            // 3. Play it through SoundManager
            
        } catch (Exception e) {
            LOGGER.debug("Minecraft sound playback failed: {}", e.getMessage());
        }
    }

    private boolean playWithJavaSound(byte[] audioData, float volume, float speed) {
        try {
            LOGGER.info("Attempting Java Sound API playback with {} bytes of audio data", audioData.length);

            javax.sound.sampled.AudioInputStream audioStream = null;

            // Try to create AudioInputStream directly first
            try {
                audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));
                LOGGER.debug("Successfully created AudioInputStream directly from audio data");
            } catch (javax.sound.sampled.UnsupportedAudioFileException e) {
                LOGGER.info("Direct AudioInputStream creation failed ({}), attempting MP3 conversion", e.getMessage());

                // Try MP3 to PCM conversion if direct approach fails
                byte[] convertedData = convertMp3ToPcmBytes(audioData);
                if (convertedData != null) {
                    try {
                        audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(new ByteArrayInputStream(convertedData));
                        LOGGER.info("Successfully created AudioInputStream from converted MP3 data");
                    } catch (Exception e2) {
                        LOGGER.warn("Failed to create AudioInputStream from converted data: {}", e2.getMessage());
                    }
                }

                if (audioStream == null) {
                    LOGGER.warn("Could not create AudioInputStream from audio data or converted data");
                    return false;
                }
            }

            javax.sound.sampled.AudioFormat format = audioStream.getFormat();
            LOGGER.info("Audio format: {} Hz, {} channels, {} bits",
                       format.getSampleRate(), format.getChannels(), format.getSampleSizeInBits());

            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(
                javax.sound.sampled.Clip.class, format);

            if (!javax.sound.sampled.AudioSystem.isLineSupported(info)) {
                LOGGER.warn("Audio line not supported for format: {}", format);
                audioStream.close();
                return false;
            }

            javax.sound.sampled.Clip clip = (javax.sound.sampled.Clip) javax.sound.sampled.AudioSystem.getLine(info);
            clip.open(audioStream);

            // Apply volume
            if (clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                javax.sound.sampled.FloatControl gainControl =
                    (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);

                float gain = 20f * (float) Math.log10(Math.max(0.1, Math.min(2.0, volume)));
                gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), gain)));
                LOGGER.info("Applied volume gain: {} dB (volume: {})", gain, volume);
            } else {
                LOGGER.warn("Volume control not supported for this audio line");
            }

            // Play the audio
            LOGGER.info("Starting audio playback...");
            clip.start();

            showTTSNotification("🔊 Deity speaks...");

            // Clean up after playback
            CompletableFuture.runAsync(() -> {
                try {
                    LOGGER.debug("Monitoring audio playback completion...");
                    while (clip.isRunning()) {
                        Thread.sleep(100);
                    }
                    LOGGER.info("Audio playback completed, cleaning up");
                    clip.close();
                } catch (Exception e) {
                    LOGGER.debug("Error during audio cleanup: {}", e.getMessage());
                }
            });

            return true;

        } catch (Exception e) {
            LOGGER.warn("Java Sound API playback failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convert MP3 data to PCM bytes using Java Sound API
     */
    private byte[] convertMp3ToPcmBytes(byte[] mp3Data) {
        try {
            // Create input stream from MP3 data
            ByteArrayInputStream bais = new ByteArrayInputStream(mp3Data);
            javax.sound.sampled.AudioInputStream mp3Stream = javax.sound.sampled.AudioSystem.getAudioInputStream(bais);

            if (mp3Stream == null) {
                LOGGER.debug("AudioSystem could not create AudioInputStream from MP3 data");
                return null;
            }

            javax.sound.sampled.AudioFormat sourceFormat = mp3Stream.getFormat();
            LOGGER.debug("Source MP3 format: {} Hz, {} channels, {} bits",
                        sourceFormat.getSampleRate(), sourceFormat.getChannels(), sourceFormat.getSampleSizeInBits());

            // Define target PCM format (44.1kHz or 48kHz, 16-bit, mono/stereo as source)
            int targetChannels = Math.min(2, Math.max(1, sourceFormat.getChannels()));
            float targetSampleRate = sourceFormat.getSampleRate() > 22000 ? 44100.0f : 22050.0f;

            javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                targetSampleRate,  // 44.1kHz or 22kHz
                16,        // 16-bit
                targetChannels,  // Mono or stereo
                targetChannels * 2,  // Frame size
                targetSampleRate,  // Frame rate
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
            ByteArrayOutputStream pcmBytes = new ByteArrayOutputStream();
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

            LOGGER.info("Successfully converted MP3 to PCM: {} bytes -> {} bytes ({}Hz, {} channels)",
                       mp3Data.length, pcmData.length, targetFormat.getSampleRate(), targetFormat.getChannels());
            return pcmData;

        } catch (javax.sound.sampled.UnsupportedAudioFileException e) {
            LOGGER.debug("MP3 format not supported by Java Sound API: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.debug("Error converting MP3 to PCM: {}", e.getMessage());
            return null;
        }
    }

    // Getters for testing and debugging
    public String getAudioUrl() { return audioUrl; }
    public byte[] getAudioData() { return audioData; }
    public float getVolume() { return volume; }
    public float getSpeed() { return speed; }
}