package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class ShockwaveEffect extends StatusEffect {

    public ShockwaveEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0xFF4500 // Orange-red untuk shockwave
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/shockwave");
    }
}