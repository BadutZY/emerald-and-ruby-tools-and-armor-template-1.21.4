package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class HorseFireEffect extends StatusEffect{

    public HorseFireEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0xE78F05
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/horse_fire");
    }
}
