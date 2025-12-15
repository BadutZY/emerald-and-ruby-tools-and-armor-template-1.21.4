package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class AutoSmeltEffect extends StatusEffect {

    public AutoSmeltEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0xFF8C00 // Dark orange untuk fire/smelt
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/auto_smelt");
    }
}