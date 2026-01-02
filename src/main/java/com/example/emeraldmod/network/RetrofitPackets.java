package com.example.emeraldmod.network;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Network packets untuk retrofit loading screen
 * FIXED: Include world name untuk prevent cross-world UI bugs
 */
public class RetrofitPackets {

    // Packet IDs
    public static final Identifier SHOW_LOADING_ID = Identifier.of(EmeraldMod.MOD_ID, "show_retrofit_loading");
    public static final Identifier UPDATE_PROGRESS_ID = Identifier.of(EmeraldMod.MOD_ID, "update_retrofit_progress");
    public static final Identifier COMPLETE_ID = Identifier.of(EmeraldMod.MOD_ID, "complete_retrofit");

    /**
     * Packet to show loading screen - NOW WITH WORLD NAME
     */
    public record ShowLoadingPayload(String worldName) implements CustomPayload {
        public static final CustomPayload.Id<ShowLoadingPayload> ID = new CustomPayload.Id<>(SHOW_LOADING_ID);
        public static final PacketCodec<RegistryByteBuf, ShowLoadingPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ShowLoadingPayload::worldName,
                ShowLoadingPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Packet to update progress - NOW WITH WORLD NAME
     */
    public record UpdateProgressPayload(String worldName, int processed, int total, String dimension) implements CustomPayload {
        public static final CustomPayload.Id<UpdateProgressPayload> ID = new CustomPayload.Id<>(UPDATE_PROGRESS_ID);
        public static final PacketCodec<RegistryByteBuf, UpdateProgressPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, UpdateProgressPayload::worldName,
                PacketCodecs.INTEGER, UpdateProgressPayload::processed,
                PacketCodecs.INTEGER, UpdateProgressPayload::total,
                PacketCodecs.STRING, UpdateProgressPayload::dimension,
                UpdateProgressPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Packet to mark as complete - NOW WITH WORLD NAME
     */
    public record CompletePayload(String worldName) implements CustomPayload {
        public static final CustomPayload.Id<CompletePayload> ID = new CustomPayload.Id<>(COMPLETE_ID);
        public static final PacketCodec<RegistryByteBuf, CompletePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, CompletePayload::worldName,
                CompletePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Register server-side packet sending
     */
    public static void registerServer() {
        try {
            PayloadTypeRegistry.playS2C().register(ShowLoadingPayload.ID, ShowLoadingPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(UpdateProgressPayload.ID, UpdateProgressPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(CompletePayload.ID, CompletePayload.CODEC);

            EmeraldMod.LOGGER.info("✅ Registered Retrofit Server Packets (with world name)");
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("❌ Failed to register retrofit packets: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send show loading packet to player dengan world name
     */
    public static void sendShowLoading(ServerPlayerEntity player, String worldName) {
        try {
            if (player == null || !player.networkHandler.isConnectionOpen()) {
                EmeraldMod.LOGGER.warn("Cannot send show loading - player not ready: {}",
                        player != null ? player.getName().getString() : "null");
                return;
            }

            ServerPlayNetworking.send(player, new ShowLoadingPayload(worldName));
            EmeraldMod.LOGGER.info("✅ Sent show loading packet to {} for world '{}'",
                    player.getName().getString(), worldName);
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("❌ Failed to send show loading to {}: {}",
                    player != null ? player.getName().getString() : "null", e.getMessage());
        }
    }

    /**
     * Send update progress packet to player dengan world name
     */
    public static void sendUpdateProgress(ServerPlayerEntity player, String worldName,
                                          int processed, int total, String dimension) {
        try {
            if (player == null || !player.networkHandler.isConnectionOpen()) {
                return; // Silent fail untuk progress updates
            }

            ServerPlayNetworking.send(player, new UpdateProgressPayload(worldName, processed, total, dimension));
        } catch (Exception e) {
            // Silent fail untuk progress updates
        }
    }

    /**
     * Send complete packet to player dengan world name
     */
    public static void sendComplete(ServerPlayerEntity player, String worldName) {
        try {
            if (player == null || !player.networkHandler.isConnectionOpen()) {
                EmeraldMod.LOGGER.warn("Cannot send complete - player not ready: {}",
                        player != null ? player.getName().getString() : "null");
                return;
            }

            ServerPlayNetworking.send(player, new CompletePayload(worldName));
            EmeraldMod.LOGGER.info("✅ Sent complete packet to {} for world '{}'",
                    player.getName().getString(), worldName);
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("❌ Failed to send complete to {}: {}",
                    player != null ? player.getName().getString() : "null", e.getMessage());
        }
    }

    /**
     * Broadcast to all players dengan world name check
     */
    public static void broadcastShowLoading(Iterable<ServerPlayerEntity> players, String worldName) {
        int successCount = 0;
        int failCount = 0;

        for (ServerPlayerEntity player : players) {
            try {
                sendShowLoading(player, worldName);
                successCount++;
            } catch (Exception e) {
                failCount++;
                EmeraldMod.LOGGER.error("Failed to broadcast show loading to {}: {}",
                        player.getName().getString(), e.getMessage());
            }
        }

        EmeraldMod.LOGGER.info("Broadcast show loading for '{}': {} success, {} failed",
                worldName, successCount, failCount);
    }

    public static void broadcastUpdateProgress(Iterable<ServerPlayerEntity> players, String worldName,
                                               int processed, int total, String dimension) {
        for (ServerPlayerEntity player : players) {
            try {
                sendUpdateProgress(player, worldName, processed, total, dimension);
            } catch (Exception e) {
                // Silent fail
            }
        }
    }

    public static void broadcastComplete(Iterable<ServerPlayerEntity> players, String worldName) {
        int successCount = 0;
        int failCount = 0;

        for (ServerPlayerEntity player : players) {
            try {
                sendComplete(player, worldName);
                successCount++;
            } catch (Exception e) {
                failCount++;
                EmeraldMod.LOGGER.error("Failed to broadcast complete to {}: {}",
                        player.getName().getString(), e.getMessage());
            }
        }

        EmeraldMod.LOGGER.info("Broadcast complete for '{}': {} success, {} failed",
                worldName, successCount, failCount);
    }
}