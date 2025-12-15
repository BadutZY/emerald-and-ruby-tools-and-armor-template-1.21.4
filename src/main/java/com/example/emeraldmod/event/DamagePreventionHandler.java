package com.example.emeraldmod.event;

import com.example.emeraldmod.item.EmeraldArmorItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;

public class DamagePreventionHandler {

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // Cek jika entity adalah player
            if (entity instanceof PlayerEntity player) {
                // Cek apakah player pakai armor emerald
                if (hasAnyEmeraldArmor(player)) {
                    // Cek apakah damage dari fire/lava
                    if (isFireDamage(source)) {
                        // Cancel damage dari fire/lava
                        return false;
                    }
                }
            }

            return true;
        });
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