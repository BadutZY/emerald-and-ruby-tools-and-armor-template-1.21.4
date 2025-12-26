package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class SilentStepEffect extends StatusEffect {

    public SilentStepEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0x00FF88 // Emerald green untuk silent step
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/silent_step");
    }
}