package com.example.emeraldmod.mixin;

import com.example.emeraldmod.item.ModItems;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public class HorseEntityPowderSnowMixin {

    /**
     * Allow horses with Emerald, Ruby, or Netherite armor to walk on powder snow
     */
    @Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void allowModHorseArmorWalkingOnPowderSnow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof HorseEntity horse) {
            ItemStack armorStack = horse.getBodyArmor();

            // ✅ CHECK: Emerald, Ruby, OR Netherite Horse Armor
            if (!armorStack.isEmpty() &&
                    (armorStack.getItem() == ModItems.EMERALD_HORSE_ARMOR ||
                            armorStack.getItem() == ModItems.RUBY_HORSE_ARMOR)) {
                cir.setReturnValue(true);
            }
        }
    }
}