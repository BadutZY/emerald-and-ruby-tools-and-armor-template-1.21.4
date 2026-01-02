package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class MoreHarvestEffect extends StatusEffect {

    public MoreHarvestEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0x274E13
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/more_harvest");
    }
}