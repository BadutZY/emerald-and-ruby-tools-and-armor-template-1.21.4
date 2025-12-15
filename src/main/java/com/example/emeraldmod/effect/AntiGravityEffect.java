package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class AntiGravityEffect extends StatusEffect {

    public AntiGravityEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0x87CEEB // Sky blue untuk anti-gravity
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/anti_gravity");
    }
}