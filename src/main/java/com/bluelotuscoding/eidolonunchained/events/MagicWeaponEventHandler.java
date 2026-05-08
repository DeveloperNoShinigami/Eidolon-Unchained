package com.bluelotuscoding.eidolonunchained.events;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.bluelotuscoding.eidolonunchained.network.DeityDamageNumberPacket;
import com.bluelotuscoding.eidolonunchained.network.EidolonUnchainedNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles the "magic weapon" system.
 *
 * When a LivingEntity hits with a weapon tagged {@code eu_magic_weapon: 1},
 * the melee hit is cancelled and re-fired as indirectMagic damage so that
 * Eidolon's own LivingHurtEvent scales it by the attacker's magic_power attribute.
 *
 * The chant effect "magic_weapon" applies the tag (with an optional expiry tick).
 * The tag is removed automatically when the expiry passes (checked each server tick).
 *
 * JSON fields for the chant effect:
 *   "duration"  — ticks the weapon stays magical (omit for permanent until death/relog)
 *   "slot"      — "mainhand" (default) or "offhand"
 */
@Mod.EventBusSubscriber(modid = EidolonUnchained.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MagicWeaponEventHandler {

    public static final String TAG_MAGIC_WEAPON = "eu_magic_weapon";
    public static final String TAG_EXPIRY        = "eu_magic_weapon_expiry";
    public static final String TAG_DAMAGE_TYPE   = "eu_magic_weapon_damage_type";
    public static final String TAG_HIT_COLOR     = "eu_magic_weapon_hit_color";

    /** Re-entry guard so we don't recursively intercept the magic hit we just fired. */
    private static final ThreadLocal<Boolean> REPROCESSING = ThreadLocal.withInitial(() -> false);

    // -------------------------------------------------------------------------
    // Melee interception
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (REPROCESSING.get()) return;

        // Skip if the hit is already some form of magic damage to avoid re-entry
        String msgId = event.getSource().getMsgId();
        if (msgId.equals("magic") || msgId.equals("indirectMagic")) return;

        LivingEntity attacker;
        ItemStack weaponUsed;

        if (event.getSource().getEntity() instanceof LivingEntity direct) {
            // ── Melee: source entity is the attacker directly ──
            attacker    = direct;
            weaponUsed  = attacker.getMainHandItem();
        } else if (event.getSource().getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow
                   && arrow.getOwner() instanceof LivingEntity shooter) {
            // ── Ranged: source entity is the arrow, owner is the shooter ──
            // Tag lives on the bow/crossbow in the shooter's hand
            attacker   = shooter;
            weaponUsed = shooter.getMainHandItem();
            if (!isMagicWeapon(weaponUsed)) {
                weaponUsed = shooter.getOffhandItem();
            }
        } else {
            return;
        }

        if (!isMagicWeapon(weaponUsed)) return;
        CompoundTag weaponTag = weaponUsed.getTag();
        String configuredDamageType = weaponTag != null && weaponTag.contains(TAG_DAMAGE_TYPE, Tag.TAG_STRING)
            ? weaponTag.getString(TAG_DAMAGE_TYPE)
            : "magic";
        Integer hitColor = weaponTag != null && weaponTag.contains(TAG_HIT_COLOR, Tag.TAG_INT)
            ? weaponTag.getInt(TAG_HIT_COLOR)
            : null;

        float amount = event.getAmount();
        float adjustedAmount = DivineResistanceResolver.applyDivineResistance(event.getEntity(), amount, configuredDamageType);
        event.setCanceled(true);

        REPROCESSING.set(true);
        try {
            Entity directSource = event.getSource().getDirectEntity();
            DamageSource convertedSource = resolveConfiguredDamageSource(
                event.getEntity(),
                directSource,
                attacker,
                configuredDamageType
            );
            if (event.getEntity().hurt(convertedSource, adjustedAmount) && hitColor != null && shouldEmitCustomDamageNumber(event.getEntity())) {
                sendCustomDamageNumber(event.getEntity(), adjustedAmount, hitColor);
            }
        } finally {
            REPROCESSING.set(false);
        }
    }

    private static boolean shouldEmitCustomDamageNumber(LivingEntity target) {
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return typeId == null || !"dummmmmmy".equals(typeId.getNamespace());
    }

    private static void sendCustomDamageNumber(LivingEntity target, float amount, int rgb) {
        if (!(target.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }

        EidolonUnchainedNetworking.sendToPlayersAround(
            level,
            target.blockPosition(),
            48.0d,
            new DeityDamageNumberPacket(target.getId(), amount, rgb)
        );
    }

    private static DamageSource resolveConfiguredDamageSource(
        LivingEntity target,
        Entity directSource,
        LivingEntity attacker,
        String configuredType
    ) {
        if (configuredType == null || configuredType.isBlank() || "magic".equalsIgnoreCase(configuredType)) {
            return target.damageSources().indirectMagic(
                directSource != null ? directSource : attacker,
                attacker
            );
        }

        if ("indirect_magic".equalsIgnoreCase(configuredType) || "indirectmagic".equalsIgnoreCase(configuredType)) {
            return target.damageSources().indirectMagic(
                directSource != null ? directSource : attacker,
                attacker
            );
        }

        ResourceLocation damageTypeId = ResourceLocation.tryParse(configuredType);
        if (damageTypeId == null) {
            return target.damageSources().indirectMagic(
                directSource != null ? directSource : attacker,
                attacker
            );
        }

        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId);
        Holder<DamageType> holder = target.level().registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolder(key)
            .orElse(null);
        if (holder == null) {
            return target.damageSources().indirectMagic(
                directSource != null ? directSource : attacker,
                attacker
            );
        }

        Entity actualDirectSource = directSource != null ? directSource : attacker;
        return new DamageSource(holder, actualDirectSource, attacker);
    }

    // -------------------------------------------------------------------------
    // Duration expiry (server tick)
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (net.minecraft.server.level.ServerLevel level : event.getServer().getAllLevels()) {
            long gameTime = level.getGameTime();
            // Use getAllEntities() to iterate all tracked entities without an AABB
            level.getAllEntities().forEach(e -> {
                if (!(e instanceof LivingEntity living)) return;
                checkAndExpireSlot(living, living.getMainHandItem(), gameTime);
                checkAndExpireSlot(living, living.getOffhandItem(), gameTime);
            });
        }
    }

    private static void checkAndExpireSlot(LivingEntity entity, ItemStack stack, long gameTime) {
        if (!isMagicWeapon(stack)) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_EXPIRY)) return;

        long expiry = tag.getLong(TAG_EXPIRY);
        if (gameTime >= expiry) {
            tag.remove(TAG_MAGIC_WEAPON);
            tag.remove(TAG_EXPIRY);
            tag.remove(TAG_DAMAGE_TYPE);
            tag.remove(TAG_HIT_COLOR);
            // Clean up empty tag
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!isMagicWeapon(event.getItemStack())) return;
        event.getToolTip().add(
            Component.translatable("tooltip.eidolonunchained.magic_weapon")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public static boolean isMagicWeapon(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_MAGIC_WEAPON);
    }

    /**
     * Applies the magic weapon tag to {@code stack}.
     * @param stack      the ItemStack to tag
     * @param durationTicks  duration in ticks, or -1 for permanent (until manually removed)
     * @param currentGameTime  current level game time (used to compute expiry)
     */
    public static void applyMagicWeaponTag(ItemStack stack, int durationTicks, long currentGameTime) {
        applyMagicWeaponTag(stack, durationTicks, currentGameTime, null, null);
    }

    /**
     * Applies the magic weapon tag to {@code stack}.
     * @param stack      the ItemStack to tag
     * @param durationTicks  duration in ticks, or -1 for permanent (until manually removed)
     * @param currentGameTime  current level game time (used to compute expiry)
     * @param damageType optional damage type id (for example, "mymod:dark_magic")
     */
    public static void applyMagicWeaponTag(ItemStack stack, int durationTicks, long currentGameTime, String damageType) {
        applyMagicWeaponTag(stack, durationTicks, currentGameTime, damageType, null);
    }

    public static void applyMagicWeaponTag(ItemStack stack, int durationTicks, long currentGameTime, String damageType, Integer hitColor) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_MAGIC_WEAPON, true);

        if (damageType != null && !damageType.isBlank() && ResourceLocation.tryParse(damageType) != null) {
            tag.putString(TAG_DAMAGE_TYPE, damageType);
        } else {
            tag.remove(TAG_DAMAGE_TYPE);
        }

        if (hitColor != null) {
            tag.putInt(TAG_HIT_COLOR, hitColor);
        } else {
            tag.remove(TAG_HIT_COLOR);
        }

        if (durationTicks > 0) {
            tag.putLong(TAG_EXPIRY, currentGameTime + durationTicks);
        } else {
            // Permanent — remove expiry if re-applying
            tag.remove(TAG_EXPIRY);
        }
    }
}
