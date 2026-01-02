package com.example.emeraldmod.config;

/**
 * Configuration untuk Ore Retrofit System
 * Anda bisa customize behavior retrofit disini
 */
public class RetrofitConfig {

    /**
     * Enable/disable automatic retrofit saat chunk load
     * Default: true (recommended)
     */
    public static final boolean ENABLE_AUTOMATIC_RETROFIT = true;

    /**
     * Enable/disable retrofit di Overworld
     * Default: true
     */
    public static final boolean RETROFIT_OVERWORLD = true;

    /**
     * Enable/disable retrofit di Nether
     * Default: true
     */
    public static final boolean RETROFIT_NETHER = true;

    /**
     * Enable/disable retrofit di End
     * Default: false (karena tidak ada Ruby Ore di End)
     */
    public static final boolean RETROFIT_END = false;

    /**
     * Versi retrofit - increment untuk force re-retrofit semua chunks
     * Useful jika Anda update ore distribution
     * Default: 1
     */
    public static final int RETROFIT_VERSION = 1;

    /**
     * Log interval - log setiap berapa chunks yang di-retrofit
     * Default: 50 chunks
     */
    public static final int LOG_INTERVAL = 50;

    /**
     * Enable detailed debug logging
     * Default: false (set true untuk troubleshooting)
     */
    public static final boolean DEBUG_LOGGING = false;

    /**
     * Max chunks to retrofit per tick (rate limiting)
     * Set 0 untuk no limit
     * Default: 0 (no limit, karena retrofit cepat)
     */
    public static final int MAX_RETROFITS_PER_TICK = 0;

    /**
     * Probability multiplier untuk ore generation
     * 1.0 = normal, 2.0 = 2x ore, 0.5 = half ore
     * Default: 1.0
     */
    public static final double ORE_GENERATION_MULTIPLIER = 1.0;

    /**
     * Enable/disable command "/emeraldmod retrofit"
     * Default: true
     */
    public static final boolean ENABLE_RETROFIT_COMMAND = true;

    /**
     * Minimum OP level required untuk retrofit command
     * Default: 2
     */
    public static final int REQUIRED_OP_LEVEL = 2;

    /**
     * Check if retrofit enabled untuk dimension ini
     */
    public static boolean isRetrofitEnabledForDimension(String dimensionId) {
        return switch (dimensionId) {
            case "minecraft:overworld" -> RETROFIT_OVERWORLD;
            case "minecraft:the_nether" -> RETROFIT_NETHER;
            case "minecraft:the_end" -> RETROFIT_END;
            default -> false;
        };
    }

    /**
     * Calculate actual attempts berdasarkan multiplier
     */
    public static int getAdjustedAttempts(int baseAttempts) {
        return Math.max(1, (int)(baseAttempts * ORE_GENERATION_MULTIPLIER));
    }

    /**
     * Check if should log this retrofit
     */
    public static boolean shouldLog(int retrofitCount) {
        return retrofitCount % LOG_INTERVAL == 0;
    }
}