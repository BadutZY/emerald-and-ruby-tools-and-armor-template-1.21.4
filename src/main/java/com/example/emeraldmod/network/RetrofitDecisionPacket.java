package com.example.emeraldmod.network;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.world.gen.InstantRetrofitSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Packet untuk mengirim keputusan player (YES/NO) ke server
 */
public class RetrofitDecisionPacket {

    public static final Identifier DECISION_ID = Identifier.of(EmeraldMod.MOD_ID, "retrofit_decision");
    public static final Identifier ASK_CONFIRMATION_ID = Identifier.of(EmeraldMod.MOD_ID, "ask_retrofit_confirmation");

    /**
     * Payload untuk decision (YES/NO)
     */
    public record DecisionPayload(boolean accepted) implements CustomPayload {
        public static final CustomPayload.Id<DecisionPayload> ID = new CustomPayload.Id<>(DECISION_ID);
        public static final PacketCodec<RegistryByteBuf, DecisionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BOOLEAN, DecisionPayload::accepted,
                DecisionPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Payload untuk meminta konfirmasi (server -> client)
     */
    public record AskConfirmationPayload() implements CustomPayload {
        public static final CustomPayload.Id<AskConfirmationPayload> ID = new CustomPayload.Id<>(ASK_CONFIRMATION_ID);
        public static final PacketCodec<RegistryByteBuf, AskConfirmationPayload> CODEC = PacketCodec.unit(new AskConfirmationPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Register server-side
     */
    public static void registerServer() {
        try {
            // Register payload types
            PayloadTypeRegistry.playC2S().register(DecisionPayload.ID, DecisionPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(AskConfirmationPayload.ID, AskConfirmationPayload.CODEC);

            // Handle decision from client
            ServerPlayNetworking.registerGlobalReceiver(DecisionPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    boolean accepted = payload.accepted();

                    EmeraldMod.LOGGER.info("[RetrofitDecision] Player {} chose: {}",
                            player.getName().getString(), accepted ? "YES" : "NO");

                    if (accepted) {
                        // Start retrofit
                        EmeraldMod.LOGGER.info("[RetrofitDecision] Starting retrofit for player request");
                        InstantRetrofitSystem.runInitialRetrofit(context.server());
                    } else {
                        EmeraldMod.LOGGER.info("[RetrofitDecision] Player declined, will show reminder widget");
                        // Client will show reminder widget automatically
                    }
                });
            });

            EmeraldMod.LOGGER.info("✓ Registered Retrofit Decision Packets (Server)");
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("✗ Failed to register retrofit decision packets: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Register client-side
     */
    public static void registerClient() {
        try {
            // Handle request for confirmation from server
            ClientPlayNetworking.registerGlobalReceiver(AskConfirmationPayload.ID, (payload, context) -> {
                context.client().execute(() -> {
                    EmeraldMod.LOGGER.info("[RetrofitDecision] Received request to show confirmation");
                    com.example.emeraldmod.client.RetrofitConfirmationScreen.show();
                });
            });

            EmeraldMod.LOGGER.info("✓ Registered Retrofit Decision Packets (Client)");
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("✗ Failed to register retrofit decision packets (client): {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send decision to server (from client)
     */
    public static void sendDecision(boolean accepted) {
        try {
            ClientPlayNetworking.send(new DecisionPayload(accepted));
            EmeraldMod.LOGGER.info("[RetrofitDecision] Sent decision to server: {}", accepted ? "YES" : "NO");
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("[RetrofitDecision] Failed to send decision: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ask player for confirmation (from server to client)
     */
    public static void askConfirmation(ServerPlayerEntity player) {
        try {
            ServerPlayNetworking.send(player, new AskConfirmationPayload());
            EmeraldMod.LOGGER.info("[RetrofitDecision] Asked player {} for confirmation",
                    player.getName().getString());
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("[RetrofitDecision] Failed to ask confirmation: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}