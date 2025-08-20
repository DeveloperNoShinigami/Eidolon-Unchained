package com.bluelotuscoding.eidolonunchained.research.tasks;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.data.ResearchDataManager;
import com.bluelotuscoding.eidolonunchained.research.ResearchEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles progress tracking for {@link KillEntityWithNbtTask} instances.
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID)
public class KillEntityWithNbtTaskHandler {
    private static final String DATA_KEY = "eu_kill_entity_nbt";

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        Entity killed = event.getEntity();
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(killed.getType());
        if (type == null) return;
        CompoundTag data = killed.saveWithoutId(new CompoundTag());

        for (ResearchEntry entry : ResearchDataManager.getLoadedResearchEntries().values()) {
            for (var tasks : entry.getTasks().values()) {
                for (ResearchTask task : tasks) {
                    if (task instanceof KillEntityWithNbtTask killTask) {
                        if (!type.equals(killTask.getEntity())) continue;
                        CompoundTag filter = killTask.getFilter();
                        if (filter != null && !NbtUtils.compareNbt(filter, data, true)) continue;
                        increment(player, killTask);
                    }
                }
            }
        }
    }

    private static void increment(ServerPlayer player, KillEntityWithNbtTask task) {
        CompoundTag root = player.getPersistentData();
        CompoundTag kills = root.getCompound(DATA_KEY);
        String key = task.getKey();
        kills.putInt(key, kills.getInt(key) + 1);
        root.put(DATA_KEY, kills);
    }

    static int getKillCount(Player player, KillEntityWithNbtTask task) {
        if (!(player instanceof ServerPlayer sp)) return 0;
        CompoundTag root = sp.getPersistentData();
        CompoundTag kills = root.getCompound(DATA_KEY);
        return kills.getInt(task.getKey());
    }
}
