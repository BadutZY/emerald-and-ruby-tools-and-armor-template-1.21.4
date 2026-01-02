package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class VeinMiningEffect extends StatusEffect {

    public VeinMiningEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0xB86602
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/vein_mining");
    }
}