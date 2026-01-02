package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.item.RubyArmorItem;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;

public class DamagePreventionHandler {

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                // ✅ CHECK: Armor effect enabled
                EffectStateManager stateManager = EffectStateManager.getServerState(player.getServer());

                if (!stateManager.isArmorEnabled(player.getUuid())) {
                    return true; // Allow damage if effect disabled
                }

                // ✅ CHECK: Has Emerald OR Ruby Armor
                if (hasAnyModArmor(player)) {
                    if (isFireDamage(source)) {
                        EmeraldMod.LOGGER.debug("Cancelled fire damage for player {} (armor effect enabled)",
                                player.getName().getString());
                        return false; // Cancel fire damage
                    }
                }
            }

            return true;
        });

        EmeraldMod.LOGGER.info("✓ Registered Fire Damage Prevention (Emerald + Ruby Armor - Toggleable)");
    }

    private static boolean isFireDamage(DamageSource source) {
        return source.isIn(DamageTypeTags.IS_FIRE) ||
                source.isOf(DamageTypes.IN_FIRE) ||
                source.isOf(DamageTypes.ON_FIRE) ||
                source.isOf(DamageTypes.LAVA) ||
                source.isOf(DamageTypes.HOT_FLOOR);
    }

    private static boolean hasAnyModArmor(PlayerEntity player) {
        for (ItemStack armorStack : player.getArmorItems()) {
            // Check Emerald Armor
            if (armorStack.getItem() instanceof EmeraldArmorItem) {
                return true;
            }
            // Check Ruby Armor
            if (armorStack.getItem() instanceof RubyArmorItem) {
                return true;
            }
        }
        return false;
    }
}