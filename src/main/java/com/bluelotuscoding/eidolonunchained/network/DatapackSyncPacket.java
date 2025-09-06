package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.codex.CodexEntry;
import com.bluelotuscoding.eidolonunchained.research.ResearchChapter;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * COMPLETE datapack synchronization packet  
 * Syncs ALL datapack content: deities, chants, codex entries, research data, and AI configs
 * Professional implementation with full functionality
 */
public class DatapackSyncPacket {
    private static final Gson GSON = new GsonBuilder().create();
    
    private final Map<ResourceLocation, String> deityData;
    private final Map<ResourceLocation, String> chantData;
    private final Map<ResourceLocation, String> codexData;
    private final Map<ResourceLocation, String> aiDeityData;
    private final Map<ResourceLocation, String> researchChapterData;
    private final Map<ResourceLocation, String> researchEntryData;
    
    /**
     * Create packet with current server data
     */
    public DatapackSyncPacket() {
        this.deityData = new HashMap<>();
        this.chantData = new HashMap<>(); 
        this.codexData = new HashMap<>();
        this.aiDeityData = new HashMap<>();
        this.researchChapterData = new HashMap<>();
        this.researchEntryData = new HashMap<>();
        
        // Gather ALL server-side datapack content
        collectServerData();
    }
    
    /**
     * Constructor for packet decoding
     */
    public DatapackSyncPacket(Map<ResourceLocation, String> deityData, 
                             Map<ResourceLocation, String> chantData,
                             Map<ResourceLocation, String> codexData,
                             Map<ResourceLocation, String> aiDeityData,
                             Map<ResourceLocation, String> researchChapterData,
                             Map<ResourceLocation, String> researchEntryData) {
        this.deityData = deityData;
        this.chantData = chantData;
        this.codexData = codexData;
        this.aiDeityData = aiDeityData;
        this.researchChapterData = researchChapterData;
        this.researchEntryData = researchEntryData;
    }
    
    private void collectServerData() {
        try {
            System.out.println("DatapackSyncPacket: Collecting server data...");
            
            // Collect deity data
            Map<ResourceLocation, DatapackDeity> deities = DatapackDeityManager.getAllDeities();
            if (deities != null) {
                System.out.println("DatapackSyncPacket: Found " + deities.size() + " deities to sync");
                deities.forEach((id, deity) -> {
                    try {
                        deityData.put(id, GSON.toJson(deity.toJson()));
                        System.out.println("DatapackSyncPacket: Serialized deity " + id);
                    } catch (Exception e) {
                        System.err.println("Failed to serialize deity " + id + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                System.out.println("DatapackSyncPacket: No deities found on server");
            }
            
            // Collect chant data
            Map<ResourceLocation, DatapackChant> chants = DatapackChantManager.getAllChantsAsMap();
            if (chants != null) {
                System.out.println("DatapackSyncPacket: Found " + chants.size() + " chants to sync");
                chants.forEach((id, chant) -> {
                    try {
                        chantData.put(id, GSON.toJson(chant.toJson()));
                        System.out.println("DatapackSyncPacket: Serialized chant " + id);
                    } catch (Exception e) {
                        System.err.println("Failed to serialize chant " + id + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                System.out.println("DatapackSyncPacket: No chants found on server");
            }
            
            // Collect codex data  
            Map<ResourceLocation, CodexEntry> codexEntries = CodexDataManager.getAllEntries();
            if (codexEntries != null) {
                System.out.println("DatapackSyncPacket: Found " + codexEntries.size() + " codex entries to sync");
                codexEntries.forEach((id, entry) -> {
                    try {
                        codexData.put(id, GSON.toJson(entry.toJson()));
                        System.out.println("DatapackSyncPacket: Serialized codex entry " + id);
                    } catch (Exception e) {
                        System.err.println("Failed to serialize codex entry " + id + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                System.out.println("DatapackSyncPacket: No codex entries found on server");
            }
            
            // Collect AI deity data  
            AIDeityManager aiManager = AIDeityManager.getInstance();
            if (aiManager != null) {
                java.util.Collection<AIDeityConfig> aiConfigs = aiManager.getAllConfigs();
                if (aiConfigs != null && !aiConfigs.isEmpty()) {
                    System.out.println("DatapackSyncPacket: Found " + aiConfigs.size() + " AI deity configs to sync");
                    aiConfigs.forEach(config -> {
                        try {
                            if (config.deity_id != null) {
                                aiDeityData.put(config.deity_id, GSON.toJson(config.toJson()));
                                System.out.println("DatapackSyncPacket: Serialized AI config for deity " + config.deity_id);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to serialize AI config for deity " + config.deity_id + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                } else {
                    System.out.println("DatapackSyncPacket: No AI deity configs found on server");
                }
            } else {
                System.out.println("DatapackSyncPacket: AIDeityManager not available");
            }
            
            // Collect research chapter data
            Map<ResourceLocation, ResearchChapter> researchChapters = ResearchDataManager.getLoadedResearchChapters();
            if (researchChapters != null && !researchChapters.isEmpty()) {
                System.out.println("DatapackSyncPacket: Found " + researchChapters.size() + " research chapters to sync");
                researchChapters.forEach((id, chapter) -> {
                    try {
                        researchChapterData.put(id, GSON.toJson(chapter.toJson()));
                        System.out.println("DatapackSyncPacket: Serialized research chapter " + id);
                    } catch (Exception e) {
                        System.err.println("Failed to serialize research chapter " + id + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                System.out.println("DatapackSyncPacket: No research chapters found on server");
            }
            
            // Collect research entry data
            Map<ResourceLocation, ResearchEntry> researchEntries = ResearchDataManager.getLoadedResearchEntries();
            if (researchEntries != null && !researchEntries.isEmpty()) {
                System.out.println("DatapackSyncPacket: Found " + researchEntries.size() + " research entries to sync");
                researchEntries.forEach((id, entry) -> {
                    try {
                        researchEntryData.put(id, GSON.toJson(entry.toJson()));
                        System.out.println("DatapackSyncPacket: Serialized research entry " + id);
                    } catch (Exception e) {
                        System.err.println("Failed to serialize research entry " + id + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                System.out.println("DatapackSyncPacket: No research entries found on server");
            }
            
            System.out.println("DatapackSyncPacket: Collection complete - " + 
                             deityData.size() + " deities, " + 
                             chantData.size() + " chants, " + 
                             codexData.size() + " codex entries, " +
                             aiDeityData.size() + " AI configs, " + 
                             researchChapterData.size() + " research chapters, " + 
                             researchEntryData.size() + " research entries");
            
        } catch (Exception e) {
            System.err.println("Failed to collect server datapack data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void encode(DatapackSyncPacket packet, FriendlyByteBuf buffer) {
        try {
            System.out.println("DatapackSyncPacket: Starting encode with " + 
                             packet.deityData.size() + " deities, " + 
                             packet.chantData.size() + " chants, " + 
                             packet.codexData.size() + " codex entries, " +
                             packet.aiDeityData.size() + " AI configs, " +
                             packet.researchChapterData.size() + " research chapters, " +
                             packet.researchEntryData.size() + " research entries");
            
            // Write deity data
            buffer.writeInt(packet.deityData.size());
            packet.deityData.forEach((id, data) -> {
                buffer.writeResourceLocation(id);
                buffer.writeUtf(data);
            });
            
            // Write chant data
            buffer.writeInt(packet.chantData.size());
            packet.chantData.forEach((id, data) -> {
                buffer.writeResourceLocation(id);
                buffer.writeUtf(data);
            });
            
            // Write codex data
            buffer.writeInt(packet.codexData.size());
            packet.codexData.forEach((id, data) -> {
                buffer.writeResourceLocation(id);
                buffer.writeUtf(data);
            });
            
            // Write AI deity data
            buffer.writeInt(packet.aiDeityData.size());
            packet.aiDeityData.forEach((id, data) -> {
                buffer.writeResourceLocation(id);
                buffer.writeUtf(data);
            });
            
            // Write research chapter data
            buffer.writeInt(packet.researchChapterData.size());
            packet.researchChapterData.forEach((id, data) -> {
                buffer.writeResourceLocation(id);
                buffer.writeUtf(data);
            });
            
            // Write research entry data
            buffer.writeInt(packet.researchEntryData.size());
            packet.researchEntryData.forEach((id, data) -> {
                buffer.writeResourceLocation(id);
                buffer.writeUtf(data);
            });
            
            System.out.println("DatapackSyncPacket: Encode complete - wrote " + 
                             (packet.deityData.size() + packet.chantData.size() + 
                              packet.codexData.size() + packet.aiDeityData.size() +
                              packet.researchChapterData.size() + packet.researchEntryData.size()) + " total entries");
        } catch (Exception e) {
            System.err.println("Failed to encode datapack sync packet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static DatapackSyncPacket decode(FriendlyByteBuf buffer) {
        Map<ResourceLocation, String> deityData = new HashMap<>();
        Map<ResourceLocation, String> chantData = new HashMap<>();
        Map<ResourceLocation, String> codexData = new HashMap<>();
        Map<ResourceLocation, String> aiDeityData = new HashMap<>();
        Map<ResourceLocation, String> researchChapterData = new HashMap<>();
        Map<ResourceLocation, String> researchEntryData = new HashMap<>();
        
        try {
            System.out.println("DatapackSyncPacket: Starting decode...");
            
            // Read deity data
            int deityCount = buffer.readInt();
            System.out.println("DatapackSyncPacket: Reading " + deityCount + " deities");
            for (int i = 0; i < deityCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                deityData.put(id, data);
            }
            
            // Read chant data
            int chantCount = buffer.readInt();
            System.out.println("DatapackSyncPacket: Reading " + chantCount + " chants");
            for (int i = 0; i < chantCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                chantData.put(id, data);
            }
            
            // Read codex data
            int codexCount = buffer.readInt();
            System.out.println("DatapackSyncPacket: Reading " + codexCount + " codex entries");
            for (int i = 0; i < codexCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                codexData.put(id, data);
            }
            
            // Read AI deity data
            int aiDeityCount = buffer.readInt();
            System.out.println("DatapackSyncPacket: Reading " + aiDeityCount + " AI configs");
            for (int i = 0; i < aiDeityCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                aiDeityData.put(id, data);
            }
            
            // Read research chapter data
            int researchChapterCount = buffer.readInt();
            System.out.println("DatapackSyncPacket: Reading " + researchChapterCount + " research chapters");
            for (int i = 0; i < researchChapterCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                researchChapterData.put(id, data);
            }
            
            // Read research entry data
            int researchEntryCount = buffer.readInt();
            System.out.println("DatapackSyncPacket: Reading " + researchEntryCount + " research entries");
            for (int i = 0; i < researchEntryCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                researchEntryData.put(id, data);
            }
            
            System.out.println("DatapackSyncPacket: Decode complete - " + 
                             deityCount + " deities, " + chantCount + " chants, " + 
                             codexCount + " codex entries, " + aiDeityCount + " AI configs, " +
                             researchChapterCount + " research chapters, " + researchEntryCount + " research entries");
        } catch (Exception e) {
            System.err.println("Failed to decode datapack sync packet: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new DatapackSyncPacket(deityData, chantData, codexData, aiDeityData, researchChapterData, researchEntryData);
    }
    
    public static void handle(DatapackSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // CLIENT-SIDE ONLY - populate client data managers
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                handleClientSide(packet);
            });
        });
        context.setPacketHandled(true);
    }
    
    /**
     * Handle client-side data population
     */
    private static void handleClientSide(DatapackSyncPacket packet) {
        try {
            System.out.println("CLIENT: Received DatapackSyncPacket - processing...");
            System.out.println("CLIENT: Packet contains " + packet.deityData.size() + " deities, " + 
                             packet.chantData.size() + " chants, " + 
                             packet.codexData.size() + " codex entries, " +
                             packet.aiDeityData.size() + " AI configs, " +
                             packet.researchChapterData.size() + " research chapters, " +
                             packet.researchEntryData.size() + " research entries");
            
            // Clear existing client data first
            DatapackDeityManager.clearClientDeities();
            DatapackChantManager.clearClientChants();
            CodexDataManager.clearClientEntries();
            AIDeityManager.clearClientConfigs();
            ResearchDataManager.clearClientResearchData();
            
            // Populate client-side deity data
            packet.deityData.forEach((id, jsonData) -> {
                try {
                    com.google.gson.JsonObject deityJson = GSON.fromJson(jsonData, com.google.gson.JsonObject.class);
                    DatapackDeity deity = DatapackDeity.fromJson(deityJson);
                    if (deity != null) {
                        DatapackDeityManager.addClientDeity(id, deity);
                        System.out.println("CLIENT: Added deity " + id);
                    }
                } catch (Exception e) {
                    System.err.println("CLIENT: Failed to deserialize deity " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            // Populate client-side chant data
            packet.chantData.forEach((id, jsonData) -> {
                try {
                    com.google.gson.JsonObject chantJson = GSON.fromJson(jsonData, com.google.gson.JsonObject.class);
                    DatapackChant chant = DatapackChant.fromJson(id, chantJson);
                    if (chant != null) {
                        DatapackChantManager.addClientChant(id, chant);
                        System.out.println("CLIENT: Added chant " + id);
                    }
                } catch (Exception e) {
                    System.err.println("CLIENT: Failed to deserialize chant " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            // Populate client-side codex data
            packet.codexData.forEach((id, jsonData) -> {
                try {
                    com.google.gson.JsonObject codexJson = GSON.fromJson(jsonData, com.google.gson.JsonObject.class);
                    CodexEntry entry = CodexEntry.fromJson(codexJson);
                    if (entry != null) {
                        CodexDataManager.addClientEntry(id, entry);
                        System.out.println("CLIENT: Added codex entry " + id);
                    }
                } catch (Exception e) {
                    System.err.println("CLIENT: Failed to deserialize codex entry " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            // Populate client-side AI deity data
            packet.aiDeityData.forEach((id, jsonData) -> {
                try {
                    com.google.gson.JsonObject aiJson = GSON.fromJson(jsonData, com.google.gson.JsonObject.class);
                    AIDeityConfig config = AIDeityConfig.fromJson(aiJson);
                    if (config != null) {
                        AIDeityManager.addClientConfig(id, config);
                        System.out.println("CLIENT: Added AI deity config " + id);
                    }
                } catch (Exception e) {
                    System.err.println("CLIENT: Failed to deserialize AI deity config " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            // Populate client-side research chapter data
            packet.researchChapterData.forEach((id, jsonData) -> {
                try {
                    com.google.gson.JsonObject chapterJson = GSON.fromJson(jsonData, com.google.gson.JsonObject.class);
                    ResearchChapter chapter = ResearchChapter.fromJson(chapterJson);
                    if (chapter != null) {
                        ResearchDataManager.addClientResearchChapter(id, chapter);
                        System.out.println("CLIENT: Added research chapter " + id);
                    }
                } catch (Exception e) {
                    System.err.println("CLIENT: Failed to deserialize research chapter " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            // Populate client-side research entry data
            packet.researchEntryData.forEach((id, jsonData) -> {
                try {
                    com.google.gson.JsonObject entryJson = GSON.fromJson(jsonData, com.google.gson.JsonObject.class);
                    ResearchEntry entry = ResearchEntry.fromJson(entryJson);
                    if (entry != null) {
                        ResearchDataManager.addClientResearchEntry(id, entry);
                        System.out.println("CLIENT: Added research entry " + id);
                    }
                } catch (Exception e) {
                    System.err.println("CLIENT: Failed to deserialize research entry " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            System.out.println("CLIENT: Datapack synchronization complete!");
            System.out.println("CLIENT: Final counts - Deities: " + DatapackDeityManager.getAllDeities().size() + 
                             ", Chants: " + DatapackChantManager.getAllChantsCollection().size() + 
                             ", Codex: " + CodexDataManager.getAllEntries().size() +
                             ", AI Configs: " + AIDeityManager.getAllClientSafeConfigs().size() +
                             ", Research Chapters: " + ResearchDataManager.getLoadedResearchChapters().size() +
                             ", Research Entries: " + ResearchDataManager.getLoadedResearchEntries().size());
            
            // CRITICAL: Register synced data with Eidolon systems for actual functionality
            System.out.println("CLIENT: Registering synced data with Eidolon systems...");
            
            try {
                // Register chants with Eidolon spell system (essential for keybind execution)
                DatapackChantManager.registerClientChantsWithEidolon();
                System.out.println("CLIENT: Registered chants with Eidolon spell system");
                
                // Register research with Eidolon research system
                ResearchDataManager.registerClientResearchWithEidolon();
                System.out.println("CLIENT: Registered research with Eidolon research system");
                
                System.out.println("CLIENT: All Eidolon integrations complete!");
                
            } catch (Exception e) {
                System.err.println("CLIENT: Failed to register with Eidolon systems: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("CLIENT: Failed to handle datapack sync: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
