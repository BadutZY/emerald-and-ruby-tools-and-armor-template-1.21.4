package com.example.emeraldmod;

import com.example.emeraldmod.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Client-side initialization for Emerald & Ruby Mod
 * FIXED: Proper cleanup ketika disconnect dan world change
 */
public class EmeraldModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("Initializing Emerald Mod Client");
        EmeraldMod.LOGGER.info("========================================");

        // PHASE 1: NETWORK HANDLERS
        registerNetworkHandlers();

        // PHASE 2: INPUT HANDLERS
        registerInputHandlers();

        // PHASE 3: UI COMPONENTS
        registerUIComponents();

        // PHASE 4: RENDERING
        registerRendering();

        // PHASE 5: WORLD TRACKING
        registerWorldTracking();

        // PHASE 6: FINALIZATION
        logClientInitComplete();
    }

    /**
     * PHASE 1: Register network packet handlers (client-side)
     */
    private void registerNetworkHandlers() {
        EmeraldMod.LOGGER.info("--- Phase 1: Network Handlers ---");

        // Scanning packets
        com.example.emeraldmod.client.network.ScanningClientPackets.registerClient();
        EmeraldMod.LOGGER.info("✅ Scanning Client Packets");

        // Retrofit loading screen packets
        com.example.emeraldmod.client.network.RetrofitClientPackets.registerClient();
        EmeraldMod.LOGGER.info("✅ Retrofit Client Packets");

        // Retrofit decision packets (client-side)
        com.example.emeraldmod.network.RetrofitDecisionPacket.registerClient();
        EmeraldMod.LOGGER.info("✅ Retrofit Decision Packets");
    }

    /**
     * PHASE 2: Register input handlers (keybinds)
     */
    private void registerInputHandlers() {
        EmeraldMod.LOGGER.info("--- Phase 2: Input Handlers ---");

        // Toggle keybinds (V for tools, B for armor)
        ModKeybinds.register();
        EmeraldMod.LOGGER.info("✅ Toggle Keybinds (V, B)");

        // Retrofit keybinds (M for maximize, N for generate now)
        RetrofitKeybind.register();
        EmeraldMod.LOGGER.info("✅ Retrofit Keybinds (M, N)");
    }

    /**
     * PHASE 3: Register UI components
     */
    private void registerUIComponents() {
        EmeraldMod.LOGGER.info("--- Phase 3: UI Components ---");

        // Scanning screen
        RubyOreScanningScreen.getInstance();
        EmeraldMod.LOGGER.info("✅ Ruby Ore Scanning Screen");

        // Retrofit confirmation screen
        RetrofitConfirmationScreen.getInstance();
        EmeraldMod.LOGGER.info("✅ Retrofit Confirmation Screen");

        // Retrofit loading screen
        RetrofitLoadingScreen.getInstance();
        EmeraldMod.LOGGER.info("✅ Retrofit Loading Screen");

        // Retrofit overlay (minimized state)
        RetrofitOverlayRenderer.register();
        EmeraldMod.LOGGER.info("✅ Retrofit Overlay Renderer");

        // Retrofit reminder widget (top-right corner)
        RetrofitReminderWidget.register();
        EmeraldMod.LOGGER.info("✅ Retrofit Reminder Widget");
    }

    /**
     * PHASE 4: Register rendering components
     */
    private void registerRendering() {
        EmeraldMod.LOGGER.info("--- Phase 4: Rendering ---");

        // Tooltip handler
        TooltipHandler.register();
        EmeraldMod.LOGGER.info("✅ Tooltip Handler");

        // Effect sprite loader
        EffectSpriteLoader.register();
        EmeraldMod.LOGGER.info("✅ Effect Sprite Loader");
    }

    /**
     * PHASE 5: Register world tracking
     * 🔧 FIXED: Proper cleanup ketika disconnect dan world change
     */
    private void registerWorldTracking() {
        EmeraldMod.LOGGER.info("--- Phase 5: World Tracking ---");

        // Track ketika join server/world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.getServer() != null) {
                String worldName = client.getServer().getSaveProperties().getLevelName();

                EmeraldMod.LOGGER.info("[Client] ========================================");
                EmeraldMod.LOGGER.info("[Client] Joining world: {}", worldName);
                EmeraldMod.LOGGER.info("[Client] ========================================");

                // 🔧 FIX: Reset SEMUA UI sebelum update world
                hideAllUI();

                // Update current world
                ClientWorldTracker.updateCurrentWorld(worldName);
            }
        });

        // 🔧 FIX: Track ketika disconnect - CLEANUP SEMUA
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            String previousWorld = ClientWorldTracker.getCurrentWorldName();

            EmeraldMod.LOGGER.info("[Client] ========================================");
            EmeraldMod.LOGGER.info("[Client] Disconnecting from: {}", previousWorld);
            EmeraldMod.LOGGER.info("[Client] ========================================");

            // Hide semua UI
            hideAllUI();

            // Reset world tracker
            ClientWorldTracker.reset();

            EmeraldMod.LOGGER.info("[Client] ✅ All UI hidden and state reset");
        });

        // 🔧 FIX: Track setiap tick untuk detect world changes
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getServer() != null) {
                String currentWorld = client.getServer().getSaveProperties().getLevelName();
                String trackedWorld = ClientWorldTracker.getCurrentWorldName();

                // Detect world change
                if (trackedWorld != null && !trackedWorld.equals(currentWorld)) {
                    EmeraldMod.LOGGER.info("[Client] ========================================");
                    EmeraldMod.LOGGER.info("[Client] World changed: {} -> {}", trackedWorld, currentWorld);
                    EmeraldMod.LOGGER.info("[Client] ========================================");

                    // 🔧 FIX: Hide SEMUA UI ketika world berubah
                    hideAllUI();

                    // Update current world
                    ClientWorldTracker.updateCurrentWorld(currentWorld);

                    EmeraldMod.LOGGER.info("[Client] ✅ UI reset for new world");
                }
            }
        });

        EmeraldMod.LOGGER.info("✅ World Tracking System");
    }

    /**
     * 🔧 NEW: Helper method untuk hide semua UI
     */
    private void hideAllUI() {
        try {
            RubyOreScanningScreen.hide();
            RetrofitLoadingScreen.hide();
            RetrofitConfirmationScreen.hide();
            RetrofitOverlayRenderer.deactivate();
            RetrofitReminderWidget.hide();

            EmeraldMod.LOGGER.info("[Client] All UI components hidden");
        } catch (Exception e) {
            EmeraldMod.LOGGER.error("[Client] Error hiding UI: {}", e.getMessage());
        }
    }

    /**
     * PHASE 6: Log client initialization complete
     */
    private void logClientInitComplete() {
        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("Emerald Mod Client Initialized!");
        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("");

        // Keybind info
        EmeraldMod.LOGGER.info("⌨️ CLIENT KEYBINDS:");
        EmeraldMod.LOGGER.info("  - [V] Toggle Tool Effects");
        EmeraldMod.LOGGER.info("  - [B] Toggle Armor Effects");
        EmeraldMod.LOGGER.info("  - [M] Maximize Retrofit Screen");
        EmeraldMod.LOGGER.info("  - [N] Generate Now (from widget)");
        EmeraldMod.LOGGER.info("  - Customizable in Controls menu");
        EmeraldMod.LOGGER.info("");

        // UI info
        EmeraldMod.LOGGER.info("🖥️ CLIENT UI:");
        EmeraldMod.LOGGER.info("  - Ruby Ore Scanning Screen 🔍");
        EmeraldMod.LOGGER.info("  - Retrofit Confirmation Dialog");
        EmeraldMod.LOGGER.info("  - Retrofit Loading Screen");
        EmeraldMod.LOGGER.info("  - Retrofit Reminder Widget");
        EmeraldMod.LOGGER.info("  - Status Tooltips");
        EmeraldMod.LOGGER.info("  - Per-World UI Tracking ✨");
        EmeraldMod.LOGGER.info("");

        EmeraldMod.LOGGER.info("========================================");
        EmeraldMod.LOGGER.info("Client ready! 🎮");
        EmeraldMod.LOGGER.info("========================================");
    }
}