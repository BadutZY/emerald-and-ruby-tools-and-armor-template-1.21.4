package com.example.emeraldmod.client.network;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.client.ClientWorldTracker;
import com.example.emeraldmod.client.RubyOreScanningScreen;
import com.example.emeraldmod.network.ScanningPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side packet handlers untuk ruby ore scanning
 */
@Environment(EnvType.CLIENT)
public class ScanningClientPackets {

    private static boolean handlersRegistered = false;

    /**
     * Register client-side packet handlers
     */
    public static void registerClient() {
        if (handlersRegistered) {
            EmeraldMod.LOGGER.warn("Scanning client packet handlers already registered, skipping...");
            return;
        }

        try {
            // Handle start scan packet
            ClientPlayNetworking.registerGlobalReceiver(
                    ScanningPackets.StartScanPayload.ID,
                    (payload, context) -> {
                        try {
                            String worldName = payload.worldName();
                            EmeraldMod.LOGGER.info("[Client] ✅ Received start scan packet for world '{}'", worldName);

                            context.client().execute(() -> {
                                try {
                                    // Show scanning screen
                                    RubyOreScanningScreen.show();
                                    EmeraldMod.LOGGER.info("[Client] Showing scanning screen for world '{}'", worldName);
                                } catch (Exception e) {
                                    EmeraldMod.LOGGER.error("[Client] Error showing scanning screen: {}", e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        } catch (Exception e) {
                            EmeraldMod.LOGGER.error("[Client] Error processing start scan packet: {}", e.getMessage());
                            e.printStackTrace();
                        }
                    }
            );

            // Handle scan status update packet
            ClientPlayNetworking.registerGlobalReceiver(
                    ScanningPackets.ScanStatusPayload.ID,
                    (payload, context) -> {
                        try {
                            String worldName = payload.worldName();
                            String message = payload.message();

                            context.client().execute(() -> {
                                try {
                                    // Update scanning screen status
                                    RubyOreScanningScreen.updateStatus(message);
                                    EmeraldMod.LOGGER.debug("[Client] Scan status: {}", message);
                                } catch (Exception e) {
                                    // Silent fail for status updates
                                }
                            });
                        } catch (Exception e) {
                            // Silent fail
                        }
                    }
            );

            // Handle scan complete packet
            ClientPlayNetworking.registerGlobalReceiver(
                    ScanningPackets.ScanCompletePayload.ID,
                    (payload, context) -> {
                        try {
                            String worldName = payload.worldName();
                            boolean hasOres = payload.hasOres();

                            EmeraldMod.LOGGER.info("[Client] ✅ Received scan complete for world '{}': hasOres={}",
                                    worldName, hasOres);

                            context.client().execute(() -> {
                                try {
                                    // Update scanning screen with result
                                    RubyOreScanningScreen.setScanComplete(hasOres);

                                    if (hasOres) {
                                        EmeraldMod.LOGGER.info("[Client] World has ores - will auto-close");
                                    } else {
                                        EmeraldMod.LOGGER.info("[Client] World needs retrofit - will show confirmation");
                                    }
                                } catch (Exception e) {
                                    EmeraldMod.LOGGER.error("[Client] Error setting scan complete: {}", e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        } catch (Exception e) {
                            EmeraldMod.LOGGER.error("[Client] Error processing scan complete packet: {}", e.getMessage());
                            e.printStackTrace();
                        }
                    }
            );

            handlersRegistered = true;
            EmeraldMod.LOGGER.info("✅ Registered Scanning Client Packet Handlers");
            EmeraldMod.LOGGER.info("  → Start Scan: {}", ScanningPackets.START_SCAN_ID);
            EmeraldMod.LOGGER.info("  → Scan Status: {}", ScanningPackets.SCAN_STATUS_ID);
            EmeraldMod.LOGGER.info("  → Scan Complete: {}", ScanningPackets.SCAN_COMPLETE_ID);

        } catch (Exception e) {
            EmeraldMod.LOGGER.error("❌ Failed to register scanning client packet handlers: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if handlers are registered
     */
    public static boolean isRegistered() {
        return handlersRegistered;
    }
}