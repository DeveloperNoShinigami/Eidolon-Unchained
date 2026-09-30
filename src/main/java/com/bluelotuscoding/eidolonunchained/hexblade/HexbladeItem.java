package com.bluelotuscoding.eidolonunchained.hexblade;

import com.bluelotuscoding.eidolonunchained.EidolonUnchained;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import elucent.eidolon.capability.IReputation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A scripted hexblade: Hexblades Renewed's weapon system (awaken on right-click, devotion-scaled power, durability as
 * the energy pool that drains while awakened and recharges while dormant, no invulnerability frames, on-hit effects,
 * dialogue) bound to <em>any</em> Eidolon deity instead of only Hexblades' own. Made by KubeJS:
 * {@code StartupEvents.registry('item', e => e.create('mypack:bone_blade', 'eidolonunchained:hexblade').deity(...)…)}.
 * State lives in {@code eidolonunchained:{v:1, hexblade:{awakened, damage, speed, elemental}}} (rule C6).
 */
public class HexbladeItem extends SwordItem {
    public interface DevotionFn {
        double apply(double devotion);
    }

    public interface HitFn {
        void on(ItemStack stack, LivingEntity target, LivingEntity attacker, boolean awakened);
    }

    public interface HeldFn {
        void on(Player player, ItemStack stack, boolean awakened);
    }

    /** Everything the builder decided. */
    public static final class Settings {
        public ResourceLocation deity;
        public int rechargeTicks = 5;
        public int drainPerTick = 2;
        public int hitEnergy = 10;
        public double elementalRatio = 10;
        public DevotionFn awakenedDamage = d -> d / 10.0;
        public DevotionFn awakenedSpeed = d -> 0;
        public @Nullable HitFn onHit;
        public @Nullable HeldFn whileHeld;
        public List<String> dialogue = List.of();
        public List<Component> flavor = List.of();
        public List<Component> awakenedTooltip = List.of();
        public int textColor = 0xFFAA00;
        public boolean noInvulnerabilityFrames = true;
        public boolean talkOnAwaken = true;
    }

    public static final String ROOT = EidolonUnchained.MOD_ID;
    private final Settings s;
    private final double baseAttack;
    private final double baseSpeed;

    public HexbladeItem(Settings settings, Tier tier, int attackBaseline, float speedBaseline, Properties props) {
        super(tier, attackBaseline, speedBaseline, props);
        this.s = settings;
        this.baseAttack = attackBaseline + tier.getAttackDamageBonus();
        this.baseSpeed = speedBaseline;
    }

    public Settings settings() {
        return s;
    }

    public @Nullable ResourceLocation deity() {
        return s.deity;
    }

    // ---- state ----

    private static CompoundTag state(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getCompound(ROOT).getCompound("hexblade") : new CompoundTag();
    }

    private static void write(ItemStack stack, CompoundTag hex) {
        var root = stack.getOrCreateTag().getCompound(ROOT);
        root.putInt("v", 1);
        root.put("hexblade", hex);
        stack.getOrCreateTag().put(ROOT, root);
    }

    public static boolean isAwakened(ItemStack stack) {
        return !stack.isEmpty() && state(stack).getBoolean("awakened");
    }

    public static double awakenedDamage(ItemStack stack) {
        return state(stack).getDouble("damage");
    }

    public static double awakenedSpeed(ItemStack stack) {
        return state(stack).getDouble("speed");
    }

    public static float elementalPower(ItemStack stack) {
        return state(stack).getFloat("elemental");
    }

    public int energyLeft(ItemStack stack) {
        return getMaxDamage(stack) - stack.getDamageValue();
    }

    public double devotion(Player player) {
        if (s.deity == null || !(player instanceof ServerPlayer sp)) return 0;
        return sp.server.overworld().getCapability(IReputation.INSTANCE).resolve().map(r -> r.getReputation(sp, s.deity)).orElse(0.0);
    }

    /** Awakens (only when fully charged, as Hexblades does) or puts the blade to sleep; recomputes the powers. */
    public boolean setAwakened(ItemStack stack, Player player, boolean awaken) {
        if (awaken && stack.getDamageValue() != 0) return false;
        double devotion = devotion(player);
        var hex = state(stack);
        hex.putBoolean("awakened", awaken);
        hex.putDouble("damage", awaken ? safe(s.awakenedDamage, devotion) : 0);
        hex.putDouble("speed", awaken ? safe(s.awakenedSpeed, devotion) : 0);
        hex.putFloat("elemental", awaken && s.elementalRatio > 0 ? (float) (devotion / s.elementalRatio) : 0f);
        write(stack, hex);
        if (awaken && s.talkOnAwaken) talk(player);
        return true;
    }

    private static double safe(DevotionFn fn, double devotion) {
        try {
            return fn.apply(devotion);
        } catch (RuntimeException e) {
            EidolonUnchained.LOGGER.error("hexblade devotion function threw: {}", e.toString());
            return 0;
        }
    }

    public void toggle(ItemStack stack, Player player) {
        setAwakened(stack, player, !isAwakened(stack));
    }

    public void talk(Player player) {
        if (s.dialogue.isEmpty()) return;
        var line = s.dialogue.get(player.getRandom().nextInt(s.dialogue.size()));
        player.sendSystemMessage(Component.literal(line).setStyle(Style.EMPTY.withItalic(true).withColor(TextColor.fromRgb(s.textColor))));
    }

    // ---- behaviour ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide() && !(player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ShieldItem)) {
            toggle(stack, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        boolean awakened = isAwakened(stack);
        if (s.whileHeld != null && (selected || player.getOffhandItem() == stack)) {
            try {
                s.whileHeld.on(player, stack, awakened);
            } catch (RuntimeException e) {
                EidolonUnchained.LOGGER.error("hexblade whileHeld threw: {}", e.toString());
            }
        }
        if (awakened && !player.isCreative()) {
            if (energyLeft(stack) > s.rechargeTicks + 1) {
                stack.hurtAndBreak(s.drainPerTick, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            } else {
                setAwakened(stack, player, false);                 // spent: the blade sleeps
            }
        } else if (stack.getDamageValue() > 0) {
            stack.setDamageValue(Math.max(stack.getDamageValue() - s.rechargeTicks, 0));
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean awakened = isAwakened(stack);
        if (s.noInvulnerabilityFrames && target.invulnerableTime > 0) target.invulnerableTime = 0;
        if (s.onHit != null) {
            try {
                s.onHit.on(stack, target, attacker, awakened);
            } catch (RuntimeException e) {
                EidolonUnchained.LOGGER.error("hexblade onHit threw: {}", e.toString());
            }
        }
        stack.setDamageValue(Math.max(stack.getDamageValue() - s.hitEnergy, 0));   // a hit feeds the blade
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        boolean awakened = isAwakened(stack);
        map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", baseAttack + (awakened ? awakenedDamage(stack) : 0), AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", baseSpeed + (awakened ? awakenedSpeed(stack) : 0), AttributeModifier.Operation.ADDITION));
        return map;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        for (var line : s.flavor) tooltip.add(line.copy().setStyle(Style.EMPTY.withItalic(true).withColor(TextColor.fromRgb(s.textColor))));
        if (s.deity != null) tooltip.add(Component.translatable("eidolonunchained.hexblade.bound", prettify(s.deity)).withStyle(ChatFormatting.GOLD));
        boolean awakened = isAwakened(stack);
        tooltip.add(Component.translatable(awakened ? "eidolonunchained.hexblade.awakened" : "eidolonunchained.hexblade.dormant").withStyle(awakened ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("eidolonunchained.hexblade.awakened_damage", String.format("%.1f", awakenedDamage(stack))).withStyle(ChatFormatting.BLUE));
            if (s.elementalRatio > 0) tooltip.add(Component.translatable("eidolonunchained.hexblade.elemental", String.format("%.1f", elementalPower(stack))).withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("eidolonunchained.hexblade.energy", energyLeft(stack), getMaxDamage(stack)).withStyle(ChatFormatting.BLUE));
            for (var line : s.awakenedTooltip) tooltip.add(line.copy().withStyle(ChatFormatting.BLUE));
        } else {
            tooltip.add(Component.translatable("eidolonunchained.hexblade.shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String prettify(ResourceLocation id) {
        var p = id.getPath().replace('_', ' ');
        return p.isEmpty() ? id.toString() : Character.toUpperCase(p.charAt(0)) + p.substring(1);
    }
}
