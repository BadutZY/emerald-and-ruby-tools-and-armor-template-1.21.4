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
 * Network packets untuk ruby ore scanning
 */
public class ScanningPackets {

    // Packet IDs
    public static final Identifier START_SCAN_ID = Identifier.of(EmeraldMod.MOD_ID, "start_ore_scan");
    public static final Identifier SCAN_STATUS_ID = Identifier.of(EmeraldMod.MOD_ID, "scan_status");
    public static final Identifier SCAN_COMPLETE_ID = Identifier.of(EmeraldMod.MOD_ID, "scan_complete");

    /**
     * Packet to start scanning
     */
    public record StartScanPayload(String worldName) implements CustomPayload {
        public static final CustomPayload.Id<StartScanPayload> ID = new CustomPayload.Id<>(START_SCAN_ID);
        public static final PacketCodec<RegistryByteBuf, StartScanPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, StartScanPayload::worldName,
                StartScanPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Packet to update scan status
     */
    public record ScanStatusPayload(String worldName, String message) implements CustomPayload {
        public static final CustomPayload.Id<ScanStatusPayload> ID = new CustomPayload.Id<>(SCAN_STATUS_ID);
        public static final PacketCodec<RegistryByteBuf, ScanStatusPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ScanStatusPayload::worldName,
                PacketCodecs.STRING, ScanStatusPayload::message,
                ScanStatusPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Packet to mark scan as complete
     */
    public record ScanCompletePayload(String worldName, boolean hasOres) implements CustomPayload {
        public static final CustomPayload.Id<ScanCompletePayload> ID = new CustomPayload.Id<>(SCAN_COMPLETE_ID);
        public static final PacketCodec<RegistryByteBuf, ScanCompletePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ScanCompletePayload::worldName,
                PacketCodecs.BOOLEAN, ScanCompletePayload::hasOres,
                ScanCompletePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Register server-side packets
     */
    public static void registerServer() {
        try {
            PayloadTypeRegistry.playS2C().register(StartScanPayload.ID, StartScanPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(ScanStatusPayload.ID, ScanStatusPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(ScanCompletePayload.ID, ScanCompletePayload.CODEC);

            EmeraldMod.LOGGER.info("✅ Registered Scanning Server Packets");
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("❌ Failed to register scanning packets: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send start scan packet to player
     */
    public static void sendStartScan(ServerPlayerEntity player, String worldName) {
        try {
            if (player == null || !player.networkHandler.isConnectionOpen()) {
                EmeraldMod.LOGGER.warn("Cannot send start scan - player not ready");
                return;
            }

            ServerPlayNetworking.send(player, new StartScanPayload(worldName));
            EmeraldMod.LOGGER.info("✅ Sent start scan packet to {} for world '{}'",
                    player.getName().getString(), worldName);
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("❌ Failed to send start scan: {}", e.getMessage());
        }
    }

    /**
     * Send scan status update to player
     */
    public static void sendScanStatus(ServerPlayerEntity player, String worldName, String message) {
        try {
            if (player == null || !player.networkHandler.isConnectionOpen()) {
                return;
            }

            ServerPlayNetworking.send(player, new ScanStatusPayload(worldName, message));
        } catch (Exception e) {
            // Silent fail for status updates
        }
    }

    /**
     * Send scan complete to player
     */
    public static void sendScanComplete(ServerPlayerEntity player, String worldName, boolean hasOres) {
        try {
            if (player == null || !player.networkHandler.isConnectionOpen()) {
                EmeraldMod.LOGGER.warn("Cannot send scan complete - player not ready");
                return;
            }

            ServerPlayNetworking.send(player, new ScanCompletePayload(worldName, hasOres));
            EmeraldMod.LOGGER.info("✅ Sent scan complete to {} for world '{}': hasOres={}",
                    player.getName().getString(), worldName, hasOres);
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("❌ Failed to send scan complete: {}", e.getMessage());
        }
    }

    /**
     * Broadcast to all players
     */
    public static void broadcastStartScan(Iterable<ServerPlayerEntity> players, String worldName) {
        for (ServerPlayerEntity player : players) {
            sendStartScan(player, worldName);
        }
    }

    public static void broadcastScanStatus(Iterable<ServerPlayerEntity> players, String worldName, String message) {
        for (ServerPlayerEntity player : players) {
            sendScanStatus(player, worldName, message);
        }
    }

    public static void broadcastScanComplete(Iterable<ServerPlayerEntity> players, String worldName, boolean hasOres) {
        for (ServerPlayerEntity player : players) {
            sendScanComplete(player, worldName, hasOres);
        }
    }
}