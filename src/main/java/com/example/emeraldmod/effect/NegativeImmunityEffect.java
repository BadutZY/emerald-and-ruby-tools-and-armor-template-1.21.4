package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class NegativeImmunityEffect extends StatusEffect {

    public NegativeImmunityEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0x7F3DBD
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/negative_immun");
    }
}