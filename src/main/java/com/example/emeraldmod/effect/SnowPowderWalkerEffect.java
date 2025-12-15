package com.example.emeraldmod.effect;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class SnowPowderWalkerEffect extends StatusEffect {

    public SnowPowderWalkerEffect() {
        super(
                StatusEffectCategory.BENEFICIAL, // Kategori: beneficial (biru)
                0xE0F2F7 // Warna: light cyan/blue untuk snow effect
        );
    }

    // Method untuk mendapatkan texture path
    public Identifier getTextureId() {
        return Identifier.of(EmeraldMod.MOD_ID, "mob_effect/snow_powder_walker");
    }
}