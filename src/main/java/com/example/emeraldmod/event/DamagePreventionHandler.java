package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.EmeraldArmorItem;
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
            // Cek jika entity adalah player
            if (entity instanceof ServerPlayerEntity player) {
                // ✅ CHECK: Apakah armor effect enabled?
                EffectStateManager stateManager = EffectStateManager.getServerState(player.getServer());

                if (!stateManager.isArmorEnabled(player.getUuid())) {
                    // Armor effect DISABLED, allow damage
                    return true;
                }

                // Cek apakah player pakai armor emerald
                if (hasAnyEmeraldArmor(player)) {
                    // Cek apakah damage dari fire/lava
                    if (isFireDamage(source)) {
                        // Cancel damage dari fire/lava
                        EmeraldMod.LOGGER.debug("Cancelled fire damage for player {} (armor effect enabled)",
                                player.getName().getString());
                        return false;
                    }
                }
            }

            return true;
        });

        EmeraldMod.LOGGER.info("✓ Registered Fire Damage Prevention Handler (Toggleable)");
    }

    private static boolean isFireDamage(DamageSource source) {
        // Cek apakah damage termasuk fire damage
        return source.isIn(DamageTypeTags.IS_FIRE) ||
                source.isOf(DamageTypes.IN_FIRE) ||
                source.isOf(DamageTypes.ON_FIRE) ||
                source.isOf(DamageTypes.LAVA) ||
                source.isOf(DamageTypes.HOT_FLOOR);
    }

    private static boolean hasAnyEmeraldArmor(PlayerEntity player) {
        for (ItemStack armorStack : player.getArmorItems()) {
            if (armorStack.getItem() instanceof EmeraldArmorItem) {
                return true;
            }
        }
        return false;
    }
}