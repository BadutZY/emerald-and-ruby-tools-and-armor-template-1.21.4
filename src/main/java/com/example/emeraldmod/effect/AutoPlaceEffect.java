package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class AutoPlaceEffect extends StatusEffect {

    public AutoPlaceEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0x5B2E00
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/auto_place");
    }
}