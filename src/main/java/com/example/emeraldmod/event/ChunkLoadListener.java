package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.world.gen.OreRetrofitGenerator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Listener untuk event chunk load
 * Setiap kali chunk di-load, kita cek apakah perlu di-retrofit dengan ore baru
 */
public class ChunkLoadListener {

    private static int totalRetrofittedChunks = 0;
    private static int chunksCheckedThisSession = 0;

    public static void register() {
        // Register event listener untuk chunk load
        ServerChunkEvents.CHUNK_LOAD.register(ChunkLoadListener::onChunkLoad);

        EmeraldMod.LOGGER.info(" 'âœ' Registered Chunk Load Listener for Ore Retrofit");
    }

    /**
     * Called ketika chunk di-load
     */
    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        // Skip jika bukan chunk yang valid
        if (chunk == null || world == null) {
            return;
        }

        // Skip jika dimension tidak supported (hanya Overworld dan Nether)
        if (world.getRegistryKey() != net.minecraft.world.World.OVERWORLD &&
                world.getRegistryKey() != net.minecraft.world.World.NETHER) {
            return;
        }

        chunksCheckedThisSession++;

        // Try retrofit chunk (akan skip otomatis jika sudah di-retrofit)
        try {
            boolean wasRetrofitted = OreRetrofitGenerator.retrofitChunk(world, chunk);

            if (wasRetrofitted) {
                totalRetrofittedChunks++;

                // Log setiap 50 chunks yang di-retrofit
                if (totalRetrofittedChunks % 50 == 0) {
                    EmeraldMod.LOGGER.info(" 'âœ' Ore Retrofit Progress: {} chunks retrofitted in {} (Total checked: {})",
                    totalRetrofittedChunks,
                            world.getRegistryKey().getValue().getPath(),
                            chunksCheckedThisSession);
                }
            }

        } catch (Exception e) {
            EmeraldMod.LOGGER.error("Error retrofitting chunk at ({}, {}): {}",
                    chunk.getPos().x, chunk.getPos().z, e.getMessage());
        }
    }

    /**
     * Get statistics untuk debugging
     */
    public static int getTotalRetrofittedChunks() {
        return totalRetrofittedChunks;
    }

    public static int getChunksCheckedThisSession() {
        return chunksCheckedThisSession;
    }

    /**
     * Reset statistics (untuk debugging)
     */
    public static void resetStatistics() {
        totalRetrofittedChunks = 0;
        chunksCheckedThisSession = 0;
        EmeraldMod.LOGGER.info("Reset retrofit statistics");
    }
}