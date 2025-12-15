package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class TreeChoppingEffect extends StatusEffect {

    public TreeChoppingEffect() {
        super(
                StatusEffectCategory.BENEFICIAL,
                0x8B4513 // Brown untuk wood/tree
        );
    }

    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/tree_chopping");
    }
}