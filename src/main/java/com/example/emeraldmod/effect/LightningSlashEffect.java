package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class LightningSlashEffect extends StatusEffect {

    public LightningSlashEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0x3D85C6
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/lightning_slash");
    }
}