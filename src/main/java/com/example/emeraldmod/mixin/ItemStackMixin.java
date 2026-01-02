package com.example.emeraldmod.mixin;

import com.example.emeraldmod.item.RubyArmorItem;
import com.example.emeraldmod.item.RubyHorseArmorItem;
import com.example.emeraldmod.item.RubyToolItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    /**
     * Check if this is a Ruby item
     */
    private boolean isRubyItem() {
        ItemStack stack = (ItemStack) (Object) this;
        return stack.getItem() instanceof RubyToolItem.RubySwordItem ||
                stack.getItem() instanceof RubyToolItem.RubyPickaxeItem ||
                stack.getItem() instanceof RubyToolItem.RubyAxeItem ||
                stack.getItem() instanceof RubyToolItem.RubyShovelItem ||
                stack.getItem() instanceof RubyToolItem.RubyHoeItem ||
                stack.getItem() instanceof RubyArmorItem ||
                stack.getItem() instanceof RubyHorseArmorItem;
    }

    /**
     * Prevent Ruby items from taking any damage by intercepting setDamage
     */
    @Inject(method = "setDamage", at = @At("HEAD"), cancellable = true)
    private void preventRubyDamageOnSet(int damage, CallbackInfo ci) {
        if (isRubyItem()) {
            // Cancel setDamage - Ruby items are unbreakable
            ci.cancel();
        }
    }

    /**
     * Ensure Ruby items always report 0 damage
     */
    @Inject(method = "getDamage", at = @At("RETURN"), cancellable = true)
    private void forceRubyZeroDamage(CallbackInfoReturnable<Integer> cir) {
        if (isRubyItem()) {
            // Always return 0 damage for Ruby items
            cir.setReturnValue(0);
        }
    }

    /**
     * Make Ruby items report 0 max damage (hide durability bar)
     */
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void forceRubyZeroMaxDamage(CallbackInfoReturnable<Integer> cir) {
        if (isRubyItem()) {
            // Always return 0 max damage to hide durability bar
            cir.setReturnValue(0);
        }
    }

    /**
     * Make Ruby items always damageable = false
     */
    @Inject(method = "isDamageable", at = @At("RETURN"), cancellable = true)
    private void makeRubyNotDamageable(CallbackInfoReturnable<Boolean> cir) {
        if (isRubyItem()) {
            // Ruby items are not damageable
            cir.setReturnValue(false);
        }
    }
}