package com.bluelotuscoding.eidolonunchained.mixins;

import com.bluelotuscoding.eidolonunchained.ai.AIDeityConfig;
import com.bluelotuscoding.eidolonunchained.ai.AIDeityManager;
import com.bluelotuscoding.eidolonunchained.capability.CapabilityHandler;
import com.bluelotuscoding.eidolonunchained.capability.IPatronData;
import com.bluelotuscoding.eidolonunchained.data.DatapackDeityManager;
import com.bluelotuscoding.eidolonunchained.deity.DatapackDeity;
import com.mojang.logging.LogUtils;
import elucent.eidolon.common.spell.StaticSpell;
import elucent.eidolon.common.spell.ThrallSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extends Eidolon's ThrallSpell canCast check with patron-aware per-player logic:
 *
 *   DENY  - target mob is in any opposing deity's followerMobIds for this player's patron.
 *   ALLOW - target mob is in the player's patron's followerMobIds AND the player's title
 *            meets stageRequiredForEntrall (if set).
 *   PASS  - neither case applies; Eidolon's default undead/tag check runs normally.
 *
 * Static tag wiring (Forge tag merge):
 *   data/eidolon/tags/entity_types/enthrall_whitelist.json  ->  #eidolonunchained:enthrall_whitelist
 *   data/eidolon/tags/entity_types/enthrall_blacklist.json  ->  #eidolonunchained:enthrall_blacklist
 *
 * This ensures Eidolon's EntityJoinLevelEvent goal injection (FollowOwnerGoal, ThrallTargetGoal)
 * covers all mobs added to the EU whitelist tag via datapacks, with no extra event handler needed.
 */
@Mixin(value = ThrallSpell.class, remap = false)
public class ThrallSpellMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "canCast", at = @At("HEAD"), cancellable = true)
    public void eu$patronCanCast(Level world, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        HitResult ray = StaticSpell.rayTrace(player, player.getBlockReach() + 3, 0, false);
        if (!(ray instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity living)) {
            return;
        }

        String targetMobId = getMobId(living);
        if (targetMobId == null) return;

        IPatronData patronData = player.level()
                .getCapability(CapabilityHandler.PATRON_DATA_CAPABILITY)
                .orElse(null);
        if (patronData == null) return;

        ResourceLocation patronDeityId = patronData.getPatron(serverPlayer);
        if (patronDeityId == null) return;

        try {
            AIDeityManager mgr = AIDeityManager.getInstance();
            if (mgr == null) return;

            AIDeityConfig patronAiConfig = mgr.getAIConfig(patronDeityId);
            if (patronAiConfig == null || patronAiConfig.patron_config == null) return;

            Set<String> currentDeityWhitelist = new HashSet<>(patronAiConfig.patron_config.followerMobIds);
            Set<String> opposingDeityBlacklist = getOpposingSupportedMobs(mgr, patronAiConfig);

            // 1. DENY: target mob is in opposing deity blacklist
            if (opposingDeityBlacklist.contains(targetMobId)) {
                LOGGER.debug("ThrallSpell denied for {}: {} is in opposing deity blacklist",
                        player.getName().getString(), targetMobId);
                serverPlayer.sendSystemMessage(Component.literal(
                        "\u00a7cYour patron forbids you from binding creatures of a rival faith."));
                cir.setReturnValue(false);
                return;
            }

            // 2. ALLOW: target mob is in current deity whitelist
            if (currentDeityWhitelist.contains(targetMobId)) {
                String requiredStage = patronAiConfig.patron_config.stageRequiredForEntrall;
                if (requiredStage != null && !requiredStage.isEmpty()) {
                    String playerTitle = patronData.getTitle(serverPlayer);
                    if (!titleMeetsRequirement(patronDeityId, playerTitle, requiredStage)) {
                        serverPlayer.sendSystemMessage(Component.literal(
                                "\u00a7cYour faith is not yet deep enough to bind these creatures. "
                                + "Required standing: \u00a7e" + requiredStage));
                        cir.setReturnValue(false);
                        return;
                    }
                }

                LOGGER.debug("ThrallSpell allowed for {}: {} is a sacred creature of patron {}",
                        player.getName().getString(), targetMobId, patronDeityId);
                cir.setReturnValue(true);
            }
            // 3. PASS: not in any patron's followerMobIds — Eidolon's default check runs

        } catch (Exception e) {
            LOGGER.error("ThrallSpellMixin error for player {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Returns true if playerTitle's reputation threshold is >= requiredTitle's threshold,
     * based on the deity's progression stages map.
     */
    private boolean titleMeetsRequirement(ResourceLocation deityId, String playerTitle, String requiredTitle) {
        if (playerTitle == null) return false;
        if (playerTitle.equals(requiredTitle)) return true;

        try {
            DatapackDeity deity = DatapackDeityManager.getDeity(deityId);
            if (deity == null) return false;

            Map<String, Object> stages = deity.getProgressionStages();
            if (stages == null || stages.isEmpty()) return false;

            double playerTitleRep = -1;
            double requiredTitleRep = -1;

            for (Map.Entry<String, Object> entry : stages.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> stageData)) continue;
                Object titleObj = stageData.get("title");
                Object repObj = stageData.get("reputationRequired");
                if (titleObj == null || repObj == null) continue;

                String stageTitle = titleObj.toString();
                double stageRep;
                try { stageRep = Double.parseDouble(repObj.toString()); }
                catch (NumberFormatException ignored) { continue; }

                if (stageTitle.equals(playerTitle)) playerTitleRep = stageRep;
                if (stageTitle.equals(requiredTitle)) requiredTitleRep = stageRep;
            }

            if (playerTitleRep >= 0 && requiredTitleRep >= 0) {
                return playerTitleRep >= requiredTitleRep;
            }
        } catch (Exception e) {
            LOGGER.warn("ThrallSpellMixin: could not compare title stages for deity {}: {}", deityId, e.getMessage());
        }

        return false; // deny if ordering can't be resolved
    }

    private String getMobId(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null ? key.toString() : null;
    }

    private Set<String> getOpposingSupportedMobs(AIDeityManager mgr, AIDeityConfig patronAiConfig) {
        Set<String> blacklist = new HashSet<>();
        if (patronAiConfig.patron_config.opposingDeities == null) return blacklist;

        for (String opposingIdStr : patronAiConfig.patron_config.opposingDeities) {
            try {
                ResourceLocation opposingId = ResourceLocation.tryParse(opposingIdStr);
                if (opposingId == null) continue;

                AIDeityConfig opposingConfig = mgr.getAIConfig(opposingId);
                if (opposingConfig != null && opposingConfig.patron_config != null) {
                    blacklist.addAll(opposingConfig.patron_config.followerMobIds);
                }
            } catch (Exception ignored) {
                // Ignore malformed deity IDs in config and continue processing valid entries.
            }
        }
        return blacklist;
    }
}
