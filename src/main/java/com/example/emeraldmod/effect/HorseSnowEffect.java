package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class HorseSnowEffect extends StatusEffect{

    public HorseSnowEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0xD1E2EC
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/horse_snow");
    }
}
