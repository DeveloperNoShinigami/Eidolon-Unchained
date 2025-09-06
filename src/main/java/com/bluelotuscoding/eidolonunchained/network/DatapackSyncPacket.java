package com.bluelotuscoding.eidolonunchained.network;

import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChantManager;
import com.bluelotuscoding.eidolonunchained.data.CodexDataManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.bluelotuscoding.eidolonunchained.chant.DatapackChant;
import com.bluelotuscoding.eidolonunchained.codex.CodexEntry;
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
 * Comprehensive datapack synchronization packet
 * Sends ALL custom datapack content from server to clients on login
 * This fixes the core issue where clients never received datapack data
 */
public class DatapackSyncPacket {
    private static final Gson GSON = new GsonBuilder().create();
    
    private final Map<ResourceLocation, String> deityData;
    private final Map<ResourceLocation, String> chantData;
    private final Map<ResourceLocation, String> codexData;
    
    /**
     * Create packet with current server data
     */
    public DatapackSyncPacket() {
        this.deityData = new HashMap<>();
        this.chantData = new HashMap<>(); 
        this.codexData = new HashMap<>();
        
        // Gather all server-side datapack content
        collectServerData();
    }
    
    /**
     * Constructor for packet decoding
     */
    public DatapackSyncPacket(Map<ResourceLocation, String> deityData, 
                             Map<ResourceLocation, String> chantData,
                             Map<ResourceLocation, String> codexData) {
        this.deityData = deityData;
        this.chantData = chantData;
        this.codexData = codexData;
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
                    }
                });
            } else {
                System.out.println("DatapackSyncPacket: No codex entries found on server");
            }
            
            System.out.println("DatapackSyncPacket: Collection complete - " + 
                             deityData.size() + " deities, " + 
                             chantData.size() + " chants, " + 
                             codexData.size() + " codex entries");
            
        } catch (Exception e) {
            System.err.println("Failed to collect server datapack data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void encode(DatapackSyncPacket packet, FriendlyByteBuf buffer) {
        try {
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
            
        } catch (Exception e) {
            System.err.println("Failed to encode datapack sync packet: " + e.getMessage());
        }
    }
    
    public static DatapackSyncPacket decode(FriendlyByteBuf buffer) {
        Map<ResourceLocation, String> deityData = new HashMap<>();
        Map<ResourceLocation, String> chantData = new HashMap<>();
        Map<ResourceLocation, String> codexData = new HashMap<>();
        
        try {
            // Read deity data
            int deityCount = buffer.readInt();
            for (int i = 0; i < deityCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                deityData.put(id, data);
            }
            
            // Read chant data
            int chantCount = buffer.readInt();
            for (int i = 0; i < chantCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                chantData.put(id, data);
            }
            
            // Read codex data
            int codexCount = buffer.readInt();
            for (int i = 0; i < codexCount; i++) {
                ResourceLocation id = buffer.readResourceLocation();
                String data = buffer.readUtf();
                codexData.put(id, data);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to decode datapack sync packet: " + e.getMessage());
        }
        
        return new DatapackSyncPacket(deityData, chantData, codexData);
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
                             packet.codexData.size() + " codex entries");
            
            // Clear existing client data first
            DatapackDeityManager.clearClientDeities();
            DatapackChantManager.clearClientChants();
            CodexDataManager.clearClientEntries();
            
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
                }
            });
            
            System.out.println("CLIENT: Datapack synchronization complete!");
            System.out.println("CLIENT: Final counts - Deities: " + DatapackDeityManager.getAllDeities().size() + 
                             ", Chants: " + DatapackChantManager.getAllChantsCollection().size() + 
                             ", Codex: " + CodexDataManager.getAllEntries().size());
            
        } catch (Exception e) {
            System.err.println("CLIENT: Failed to handle datapack sync: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
