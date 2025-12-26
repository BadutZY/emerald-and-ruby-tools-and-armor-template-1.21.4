package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class SwimmingHorseEffect extends StatusEffect {

    public SwimmingHorseEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0x0080FF // Bright blue untuk water/swimming
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/swimming_horse");
    }
}