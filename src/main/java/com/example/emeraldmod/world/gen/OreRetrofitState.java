package com.example.emeraldmod.world.gen;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Persistent state untuk melacak retrofit progress per-world PER-DIMENSION
 *
 * ✨ ENHANCED: Resume support - saves progress dan melanjutkan dari checkpoint
 */
public class OreRetrofitState extends PersistentState {

    // NBT keys
    private static final String NBT_KEY_BASE = "EmeraldModOreRetrofit";
    private static final String NBT_VERSION = "RetrofitVersion";
    private static final String NBT_COMPLETE = "IsComplete";
    private static final String NBT_IN_PROGRESS = "InProgress";
    private static final String NBT_CHUNKS = "RetrofittedChunks";
    private static final String NBT_WORLD_NAME = "WorldName";
    private static final String NBT_DIMENSION = "Dimension";

    // ✨ NEW: Progress tracking
    private static final String NBT_TOTAL_CHUNKS = "TotalChunks";
    private static final String NBT_PROCESSED_CHUNKS = "ProcessedChunks";
    private static final String NBT_CURRENT_DIMENSION = "CurrentDimension";
    private static final String NBT_LAST_CHECKPOINT = "LastCheckpoint";

    private static final int CURRENT_VERSION = 4; // ✨ Incremented version

    // State data
    private final Set<String> retrofittedChunks;
    private String worldName;
    private String dimensionName;
    private int version;
    private boolean isComplete;
    private boolean inProgress;

    // ✨ NEW: Progress tracking fields
    private int totalChunks;
    private int processedChunks;
    private String currentDimension;
    private long lastCheckpoint;

    /**
     * Constructor untuk state baru
     */
    public OreRetrofitState() {
        this.retrofittedChunks = new HashSet<>();
        this.worldName = "";
        this.dimensionName = "";
        this.version = CURRENT_VERSION;
        this.isComplete = false;
        this.inProgress = false;
        this.totalChunks = 0;
        this.processedChunks = 0;
        this.currentDimension = "";
        this.lastCheckpoint = 0;
    }

    /**
     * Constructor dengan world name dan dimension
     */
    public OreRetrofitState(String worldName, String dimensionName) {
        this();
        this.worldName = worldName;
        this.dimensionName = dimensionName;
    }

    // ============================================
    // PUBLIC API
    // ============================================

    /**
     * Get atau create state untuk world dan dimension tertentu
     */
    public static OreRetrofitState get(MinecraftServer server, ServerWorld world) {
        PersistentStateManager stateManager = world.getPersistentStateManager();

        String worldName = server.getSaveProperties().getLevelName();
        String dimensionName = world.getRegistryKey().getValue().getPath();
        String stateKey = generateStateKey(worldName, dimensionName);

        OreRetrofitState state = stateManager.getOrCreate(
                new PersistentState.Type<>(
                        () -> new OreRetrofitState(worldName, dimensionName),
                        (nbt, lookup) -> createFromNbt(nbt, lookup, worldName, dimensionName),
                        null
                ),
                stateKey
        );

        return state;
    }

    /**
     * Check apakah chunk sudah di-retrofit
     */
    public boolean isChunkRetrofitted(ChunkPos pos) {
        return retrofittedChunks.contains(chunkPosToString(pos));
    }

    /**
     * Mark chunk sebagai sudah di-retrofit
     */
    public void markChunkRetrofitted(ChunkPos pos) {
        String key = chunkPosToString(pos);

        if (!retrofittedChunks.contains(key)) {
            retrofittedChunks.add(key);
            processedChunks++; // ✨ Increment processed count
            updateCheckpoint(); // ✨ Save checkpoint
            markDirty();

            // Log progress setiap 100 chunks
            if (retrofittedChunks.size() % 100 == 0) {
                EmeraldMod.LOGGER.info("[RetrofitState] Progress: {}/{} chunks ({}) in '{}' ({})",
                        processedChunks, totalChunks, getPercentage(), worldName, dimensionName);
            }
        }
    }

    /**
     * Get jumlah chunks yang sudah di-retrofit
     */
    public int getRetrofittedChunkCount() {
        return retrofittedChunks.size();
    }

    // ============================================
    // ✨ NEW: PROGRESS TRACKING API
    // ============================================

    /**
     * Set total chunks untuk retrofit
     */
    public void setTotalChunks(int total) {
        this.totalChunks = total;
        markDirty();
        EmeraldMod.LOGGER.info("[RetrofitState] Set total chunks: {} for '{}' ({})",
                total, worldName, dimensionName);
    }

    /**
     * Get total chunks
     */
    public int getTotalChunks() {
        return totalChunks;
    }

    /**
     * Get processed chunks count
     */
    public int getProcessedChunks() {
        return processedChunks;
    }

    /**
     * Set processed chunks (untuk resume)
     */
    public void setProcessedChunks(int processed) {
        this.processedChunks = processed;
        markDirty();
    }

    /**
     * Get progress percentage
     */
    public int getPercentage() {
        if (totalChunks == 0) return 0;
        return (processedChunks * 100) / totalChunks;
    }

    /**
     * Set current dimension being processed
     */
    public void setCurrentDimension(String dimension) {
        this.currentDimension = dimension;
        markDirty();
    }

    /**
     * Get current dimension being processed
     */
    public String getCurrentDimension() {
        return currentDimension;
    }

    /**
     * Update checkpoint timestamp
     */
    private void updateCheckpoint() {
        this.lastCheckpoint = System.currentTimeMillis();
        markDirty();
    }

    /**
     * Get last checkpoint time
     */
    public long getLastCheckpoint() {
        return lastCheckpoint;
    }

    /**
     * Check if retrofit can be resumed
     */
    public boolean canResume() {
        return inProgress && !isComplete && processedChunks > 0 && processedChunks < totalChunks;
    }

    /**
     * Get resume info string
     */
    public String getResumeInfo() {
        if (!canResume()) return "Cannot resume";

        int percentage = getPercentage();
        long timeSince = (System.currentTimeMillis() - lastCheckpoint) / 1000;

        return String.format("Resume from %d%% (%d/%d chunks) - Last checkpoint: %ds ago",
                percentage, processedChunks, totalChunks, timeSince);
    }

    // ============================================
    // EXISTING API (unchanged)
    // ============================================

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        if (this.isComplete != complete) {
            this.isComplete = complete;

            if (complete) {
                this.inProgress = false;
                this.processedChunks = this.totalChunks; // ✨ Mark all as processed
                updateCheckpoint();
                EmeraldMod.LOGGER.info("[RetrofitState] Marked as COMPLETE for world '{}' dimension '{}' ({} chunks)",
                        worldName, dimensionName, retrofittedChunks.size());
            }

            markDirty();
        }
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        if (this.inProgress != inProgress) {
            this.inProgress = inProgress;

            if (inProgress) {
                this.isComplete = false;
                updateCheckpoint();
                EmeraldMod.LOGGER.info("[RetrofitState] Marked as IN PROGRESS for world '{}' dimension '{}'",
                        worldName, dimensionName);
            }

            markDirty();
        }
    }

    public void clearAll() {
        retrofittedChunks.clear();
        isComplete = false;
        inProgress = false;
        totalChunks = 0;
        processedChunks = 0;
        currentDimension = "";
        lastCheckpoint = 0;
        markDirty();

        EmeraldMod.LOGGER.info("[RetrofitState] Cleared all data for world '{}' dimension '{}'",
                worldName, dimensionName);
    }

    public String getWorldName() {
        return worldName;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    // ============================================
    // NBT SERIALIZATION (ENHANCED)
    // ============================================

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        // Save version
        nbt.putInt(NBT_VERSION, CURRENT_VERSION);

        // Save world name dan dimension
        nbt.putString(NBT_WORLD_NAME, worldName);
        nbt.putString(NBT_DIMENSION, dimensionName);

        // Save status flags
        nbt.putBoolean(NBT_COMPLETE, isComplete);
        nbt.putBoolean(NBT_IN_PROGRESS, inProgress);

        // ✨ NEW: Save progress tracking
        nbt.putInt(NBT_TOTAL_CHUNKS, totalChunks);
        nbt.putInt(NBT_PROCESSED_CHUNKS, processedChunks);
        nbt.putString(NBT_CURRENT_DIMENSION, currentDimension);
        nbt.putLong(NBT_LAST_CHECKPOINT, lastCheckpoint);

        // Save retrofitted chunks
        NbtCompound chunksNbt = new NbtCompound();
        for (String chunkKey : retrofittedChunks) {
            chunksNbt.putBoolean(chunkKey, true);
        }
        nbt.put(NBT_CHUNKS, chunksNbt);

        return nbt;
    }

    public static OreRetrofitState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup,
                                                 String worldName, String dimensionName) {
        OreRetrofitState state = new OreRetrofitState(worldName, dimensionName);

        // Load version
        state.version = nbt.getInt(NBT_VERSION);

        // Check version compatibility
        if (state.version != CURRENT_VERSION) {
            EmeraldMod.LOGGER.warn("[RetrofitState] Version mismatch for world '{}' dimension '{}' (saved: {}, current: {})",
                    worldName, dimensionName, state.version, CURRENT_VERSION);
            EmeraldMod.LOGGER.warn("[RetrofitState] State will be reset to current version");

            state.version = CURRENT_VERSION;
            state.markDirty();
            return state;
        }

        // Load world name dan dimension
        if (nbt.contains(NBT_WORLD_NAME)) {
            state.worldName = nbt.getString(NBT_WORLD_NAME);
        }
        if (nbt.contains(NBT_DIMENSION)) {
            state.dimensionName = nbt.getString(NBT_DIMENSION);
        }

        // Load status flags
        state.isComplete = nbt.getBoolean(NBT_COMPLETE);
        state.inProgress = nbt.getBoolean(NBT_IN_PROGRESS);

        // ✨ NEW: Load progress tracking
        if (nbt.contains(NBT_TOTAL_CHUNKS)) {
            state.totalChunks = nbt.getInt(NBT_TOTAL_CHUNKS);
        }
        if (nbt.contains(NBT_PROCESSED_CHUNKS)) {
            state.processedChunks = nbt.getInt(NBT_PROCESSED_CHUNKS);
        }
        if (nbt.contains(NBT_CURRENT_DIMENSION)) {
            state.currentDimension = nbt.getString(NBT_CURRENT_DIMENSION);
        }
        if (nbt.contains(NBT_LAST_CHECKPOINT)) {
            state.lastCheckpoint = nbt.getLong(NBT_LAST_CHECKPOINT);
        }

        // Load retrofitted chunks
        if (nbt.contains(NBT_CHUNKS)) {
            NbtCompound chunksNbt = nbt.getCompound(NBT_CHUNKS);
            for (String key : chunksNbt.getKeys()) {
                state.retrofittedChunks.add(key);
            }
        }

        // ✨ Sync processed count dengan actual chunk count
        if (state.processedChunks != state.retrofittedChunks.size()) {
            EmeraldMod.LOGGER.warn("[RetrofitState] Syncing processed count: {} -> {}",
                    state.processedChunks, state.retrofittedChunks.size());
            state.processedChunks = state.retrofittedChunks.size();
        }

        // Log loaded state
        String status = state.isComplete ? "COMPLETE" :
                state.inProgress ? "IN_PROGRESS" : "NOT_STARTED";

        EmeraldMod.LOGGER.info("[RetrofitState] Loaded state for world '{}' dimension '{}': {} ({}/{} chunks - {}%)",
                worldName, dimensionName, status, state.processedChunks, state.totalChunks, state.getPercentage());

        // ✨ Log resume info if applicable
        if (state.canResume()) {
            EmeraldMod.LOGGER.info("[RetrofitState] 🔄 CAN RESUME: {}", state.getResumeInfo());
        }

        return state;
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private static String generateStateKey(String worldName, String dimensionName) {
        String sanitizedWorld = worldName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String sanitizedDimension = dimensionName.replaceAll("[^a-zA-Z0-9_-]", "_");

        return String.format("%s_%s_%s", NBT_KEY_BASE, sanitizedWorld, sanitizedDimension);
    }

    private static String chunkPosToString(ChunkPos pos) {
        return pos.x + "_" + pos.z;
    }

    // ============================================
    // DEBUG & UTILITY (ENHANCED)
    // ============================================

    public String getDebugInfo() {
        return String.format(
                "RetrofitState[world='%s', dimension='%s', version=%d, complete=%s, inProgress=%s, chunks=%d/%d (%d%%), canResume=%s]",
                worldName, dimensionName, version, isComplete, inProgress,
                processedChunks, totalChunks, getPercentage(), canResume()
        );
    }

    public boolean isPristine() {
        return !isComplete && !inProgress && retrofittedChunks.isEmpty() && processedChunks == 0;
    }

    public boolean isValid() {
        // Complete dan inProgress tidak boleh true bersamaan
        if (isComplete && inProgress) {
            EmeraldMod.LOGGER.error("[RetrofitState] Invalid state for world '{}' dimension '{}': both complete and inProgress are true!",
                    worldName, dimensionName);
            return false;
        }

        // Processed chunks should not exceed total chunks
        if (processedChunks > totalChunks && totalChunks > 0) {
            EmeraldMod.LOGGER.error("[RetrofitState] Invalid state for world '{}' dimension '{}': processed ({}) > total ({})",
                    worldName, dimensionName, processedChunks, totalChunks);
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return getDebugInfo();
    }
}