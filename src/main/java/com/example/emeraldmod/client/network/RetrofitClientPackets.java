package com.example.emeraldmod.client.network;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.client.ClientWorldTracker;
import com.example.emeraldmod.client.RetrofitLoadingScreen;
import com.example.emeraldmod.network.RetrofitPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * Client-side packet handlers untuk retrofit loading screen
 * FIXED: Check world name sebelum show UI
 */
@Environment(EnvType.CLIENT)
public class RetrofitClientPackets {

    private static boolean handlersRegistered = false;

    /**
     * Register client-side packet handlers dengan world name checking
     */
    public static void registerClient() {
        if (handlersRegistered) {
            EmeraldMod.LOGGER.warn("Client packet handlers already registered, skipping...");
            return;
        }

        try {
            // Handle show loading packet - WITH WORLD NAME CHECK
            ClientPlayNetworking.registerGlobalReceiver(
                    RetrofitPackets.ShowLoadingPayload.ID,
                    (payload, context) -> {
                        try {
                            String worldName = payload.worldName();
                            EmeraldMod.LOGGER.info("[Client] ✅ Received show loading packet for world '{}'", worldName);

                            // Execute on main thread dengan safety check
                            context.client().execute(() -> {
                                try {
                                    MinecraftClient client = context.client();

                                    if (client == null) {
                                        EmeraldMod.LOGGER.error("[Client] MinecraftClient is null!");
                                        return;
                                    }

                                    if (client.player == null) {
                                        EmeraldMod.LOGGER.warn("[Client] Player not ready yet, delaying...");
                                        // Retry setelah 1 detik
                                        new Thread(() -> {
                                            try {
                                                Thread.sleep(1000);
                                                client.execute(() -> {
                                                    EmeraldMod.LOGGER.info("[Client] Retry showing loading screen for '{}'", worldName);
                                                    ClientWorldTracker.setRetrofitWorld(worldName);
                                                    RetrofitLoadingScreen.show();
                                                });
                                            } catch (InterruptedException e) {
                                                // Ignore
                                            }
                                        }).start();
                                        return;
                                    }

                                    // ✨ KEY FIX: Set retrofit world dan check sebelum show
                                    ClientWorldTracker.setRetrofitWorld(worldName);

                                    if (ClientWorldTracker.shouldShowRetrofitUI()) {
                                        EmeraldMod.LOGGER.info("[Client] Showing loading screen for current world '{}'", worldName);
                                        RetrofitLoadingScreen.show();
                                    } else {
                                        EmeraldMod.LOGGER.warn("[Client] Not showing loading - world mismatch (current: '{}', retrofit: '{}')",
                                                ClientWorldTracker.getCurrentWorldName(), worldName);
                                    }
                                } catch (Exception e) {
                                    EmeraldMod.LOGGER.error("[Client] Error showing loading screen: {}", e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        } catch (Exception e) {
                            EmeraldMod.LOGGER.error("[Client] Error processing show loading packet: {}", e.getMessage());
                            e.printStackTrace();
                        }
                    }
            );

            // Handle update progress packet - WITH WORLD NAME CHECK
            ClientPlayNetworking.registerGlobalReceiver(
                    RetrofitPackets.UpdateProgressPayload.ID,
                    (payload, context) -> {
                        try {
                            String worldName = payload.worldName();

                            context.client().execute(() -> {
                                try {
                                    // ✨ KEY FIX: Only update jika world match
                                    if (ClientWorldTracker.shouldShowRetrofitUI() &&
                                            worldName.equals(ClientWorldTracker.getRetrofitWorldName())) {

                                        RetrofitLoadingScreen.updateProgress(
                                                payload.processed(),
                                                payload.total(),
                                                payload.dimension()
                                        );
                                    } else {
                                        // Silent ignore - bukan world kita
                                    }
                                } catch (Exception e) {
                                    // Silent fail untuk progress updates
                                }
                            });
                        } catch (Exception e) {
                            // Silent fail
                        }
                    }
            );

            // Handle complete packet - WITH WORLD NAME CHECK
            ClientPlayNetworking.registerGlobalReceiver(
                    RetrofitPackets.CompletePayload.ID,
                    (payload, context) -> {
                        try {
                            String worldName = payload.worldName();
                            EmeraldMod.LOGGER.info("[Client] ✅ Received complete packet for world '{}'", worldName);

                            context.client().execute(() -> {
                                try {
                                    // ✨ KEY FIX: Only complete jika world match
                                    if (worldName.equals(ClientWorldTracker.getRetrofitWorldName())) {
                                        EmeraldMod.LOGGER.info("[Client] Setting retrofit complete for current world '{}'", worldName);
                                        RetrofitLoadingScreen.setComplete();

                                        // Clear retrofit world setelah complete
                                        ClientWorldTracker.clearRetrofitWorld();
                                    } else {
                                        EmeraldMod.LOGGER.warn("[Client] Ignoring complete for different world (current: '{}', completed: '{}')",
                                                ClientWorldTracker.getCurrentWorldName(), worldName);
                                    }
                                } catch (Exception e) {
                                    EmeraldMod.LOGGER.error("[Client] Error setting complete: {}", e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        } catch (Exception e) {
                            EmeraldMod.LOGGER.error("[Client] Error processing complete packet: {}", e.getMessage());
                            e.printStackTrace();
                        }
                    }
            );

            handlersRegistered = true;
            EmeraldMod.LOGGER.info("✅ Registered Retrofit Client Packet Handlers (with world checking)");
            EmeraldMod.LOGGER.info("  → Show Loading: {}", RetrofitPackets.SHOW_LOADING_ID);
            EmeraldMod.LOGGER.info("  → Update Progress: {}", RetrofitPackets.UPDATE_PROGRESS_ID);
            EmeraldMod.LOGGER.info("  → Complete: {}", RetrofitPackets.COMPLETE_ID);

        } catch (Exception e) {
            EmeraldMod.LOGGER.error("❌ Failed to register client packet handlers: {}", e.getMessage());
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