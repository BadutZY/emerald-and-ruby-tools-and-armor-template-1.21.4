package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class FastDiggingEffect extends StatusEffect {

    public FastDiggingEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0xBA036F
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/fast_digging");
    }
}