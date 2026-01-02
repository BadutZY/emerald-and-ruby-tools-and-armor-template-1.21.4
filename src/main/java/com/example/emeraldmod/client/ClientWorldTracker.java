package com.example.emeraldmod.client;

import com.example.emeraldmod.EmeraldMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Tracks current world name on client side
 * 🔧 FIXED: Proper state management untuk prevent cross-world UI bugs
 */
@Environment(EnvType.CLIENT)
public class ClientWorldTracker {

    private static String currentWorldName = null;
    private static String retrofitWorldName = null; // World yang sedang retrofit
    private static long lastWorldChangeTime = 0;

    /**
     * 🔧 FIXED: Update current world dengan proper logging
     */
    public static void updateCurrentWorld(String worldName) {
        String previous = currentWorldName;
        currentWorldName = worldName;
        lastWorldChangeTime = System.currentTimeMillis();

        if (previous == null) {
            EmeraldMod.LOGGER.info("[ClientWorldTracker] ✅ Initial world set: '{}'", worldName);
        } else if (!previous.equals(worldName)) {
            EmeraldMod.LOGGER.info("[ClientWorldTracker] 🔄 World changed: '{}' -> '{}'", previous, worldName);

            // 🔧 FIX: Clear retrofit world jika bukan world yang sama
            if (retrofitWorldName != null && !retrofitWorldName.equals(worldName)) {
                EmeraldMod.LOGGER.warn("[ClientWorldTracker] ⚠️ Clearing retrofit world '{}' (now in '{}')",
                        retrofitWorldName, worldName);
                retrofitWorldName = null;
            }
        }
    }

    /**
     * 🔧 FIXED: Set world yang sedang retrofit dengan validation
     */
    public static void setRetrofitWorld(String worldName) {
        if (worldName == null) {
            EmeraldMod.LOGGER.warn("[ClientWorldTracker] Attempted to set null retrofit world");
            return;
        }

        // 🔧 FIX: Only set jika current world sama dengan world yang mau di-retrofit
        if (currentWorldName != null && !currentWorldName.equals(worldName)) {
            EmeraldMod.LOGGER.warn("[ClientWorldTracker] ⚠️ Not setting retrofit world '{}' - current world is '{}'",
                    worldName, currentWorldName);
            return;
        }

        retrofitWorldName = worldName;
        EmeraldMod.LOGGER.info("[ClientWorldTracker] ✅ Retrofit world set to: '{}'", worldName);
    }

    /**
     * Clear retrofit world (ketika complete atau cancelled)
     */
    public static void clearRetrofitWorld() {
        if (retrofitWorldName != null) {
            EmeraldMod.LOGGER.info("[ClientWorldTracker] 🧹 Clearing retrofit world: '{}'", retrofitWorldName);
            retrofitWorldName = null;
        }
    }

    /**
     * 🔧 FIXED: Check if currently in the world yang sedang retrofit
     */
    public static boolean isInRetrofitWorld() {
        if (currentWorldName == null || retrofitWorldName == null) {
            return false;
        }

        boolean inRetrofitWorld = currentWorldName.equals(retrofitWorldName);

        if (!inRetrofitWorld && retrofitWorldName != null) {
            EmeraldMod.LOGGER.debug("[ClientWorldTracker] Not in retrofit world - current: '{}', retrofit: '{}'",
                    currentWorldName, retrofitWorldName);
        }

        return inRetrofitWorld;
    }

    /**
     * Get current world name
     */
    public static String getCurrentWorldName() {
        return currentWorldName;
    }

    /**
     * Get retrofit world name
     */
    public static String getRetrofitWorldName() {
        return retrofitWorldName;
    }

    /**
     * 🔧 FIXED: Reset dengan proper logging
     */
    public static void reset() {
        if (currentWorldName != null || retrofitWorldName != null) {
            EmeraldMod.LOGGER.info("[ClientWorldTracker] 🔄 Resetting - was in world '{}', retrofit world '{}'",
                    currentWorldName, retrofitWorldName);
        }

        currentWorldName = null;
        retrofitWorldName = null;
        lastWorldChangeTime = System.currentTimeMillis();

        EmeraldMod.LOGGER.info("[ClientWorldTracker] ✅ Reset complete");
    }

    /**
     * 🔧 FIXED: Check if should show retrofit UI dengan proper validation
     */
    public static boolean shouldShowRetrofitUI() {
        // Rule 1: Harus ada retrofit world yang di-set
        if (retrofitWorldName == null) {
            return false;
        }

        // Rule 2: Current world harus sama dengan retrofit world
        if (currentWorldName == null || !currentWorldName.equals(retrofitWorldName)) {
            // Log warning jika ada mismatch
            if (currentWorldName != null) {
                EmeraldMod.LOGGER.debug("[ClientWorldTracker] ❌ Not showing retrofit UI - " +
                        "current: '{}', retrofit: '{}'", currentWorldName, retrofitWorldName);
            }
            return false;
        }

        // Rule 3: Tidak baru saja ganti world (debounce)
        long timeSinceChange = System.currentTimeMillis() - lastWorldChangeTime;
        if (timeSinceChange < 1000) {
            EmeraldMod.LOGGER.debug("[ClientWorldTracker] ⏳ Waiting for world stabilization ({}ms)",
                    timeSinceChange);
            return false;
        }

        return true;
    }

    /**
     * 🔧 NEW: Get time since last world change (untuk debugging)
     */
    public static long getTimeSinceLastWorldChange() {
        return System.currentTimeMillis() - lastWorldChangeTime;
    }

    /**
     * 🔧 NEW: Force clear all state (emergency cleanup)
     */
    public static void forceReset() {
        EmeraldMod.LOGGER.warn("[ClientWorldTracker] ⚠️ FORCE RESET called");
        currentWorldName = null;
        retrofitWorldName = null;
        lastWorldChangeTime = 0;
    }

    /**
     * 🔧 NEW: Get debug info
     */
    public static String getDebugInfo() {
        long timeSinceChange = System.currentTimeMillis() - lastWorldChangeTime;
        return String.format(
                "ClientWorldTracker[current='%s', retrofit='%s', inRetrofit=%s, timeSince=%dms]",
                currentWorldName, retrofitWorldName, isInRetrofitWorld(), timeSinceChange
        );
    }
}