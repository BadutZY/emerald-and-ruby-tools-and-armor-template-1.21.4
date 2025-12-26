package com.example.emeraldmod.network;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Packet untuk toggle effect tools atau armor
 */
public record ToggleEffectPacket(EffectType effectType, boolean enabled) implements CustomPayload {

    public static final CustomPayload.Id<ToggleEffectPacket> ID =
            new CustomPayload.Id<>(Identifier.of(EmeraldMod.MOD_ID, "toggle_effect"));

    public static final PacketCodec<RegistryByteBuf, ToggleEffectPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.xmap(EffectType::valueOf, EffectType::name),
            ToggleEffectPacket::effectType,
            PacketCodecs.BOOLEAN,
            ToggleEffectPacket::enabled,
            ToggleEffectPacket::new
    );

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public enum EffectType {
        TOOLS,
        ARMOR
    }
}