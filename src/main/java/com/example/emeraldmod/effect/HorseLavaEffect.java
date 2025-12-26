package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class HorseLavaEffect extends StatusEffect{

    public HorseLavaEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0xF9682D
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/horse_lava");
    }
}
