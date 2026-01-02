package com.example.emeraldmod.world.gen;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detector untuk check apakah world sudah memiliki Ruby Ores atau belum
 * Digunakan untuk menentukan apakah perlu retrofit atau tidak
 */
public class WorldOreDetector {

    private static final int SAMPLE_CHUNKS = 20; // Sample 20 chunks untuk check
    private static final int MIN_ORES_FOUND = 1; // Minimal 1 ore ditemukan = world has ores

    /**
     * Check apakah world ini sudah memiliki Ruby Ores atau belum
     * @return true jika world sudah punya Ruby Ores (tidak perlu retrofit)
     */
    public static boolean worldHasRubyOres(MinecraftServer server, ServerWorld world) {
        EmeraldMod.LOGGER.info("[OreDetector] Checking if world has Ruby Ores...");

        // Get world folder
        File worldDir = server.getSavePath(WorldSavePath.ROOT).toFile();

        // Get region folder
        File regionDir;
        if (world.getRegistryKey() == World.OVERWORLD) {
            regionDir = new File(worldDir, "region");
        } else if (world.getRegistryKey() == World.NETHER) {
            regionDir = new File(new File(worldDir, "DIM-1"), "region");
        } else {
            return false; // End doesn't have ruby ores
        }

        if (!regionDir.exists() || !regionDir.isDirectory()) {
            EmeraldMod.LOGGER.info("[OreDetector] Region folder not found - new world");
            return false; // New world, no chunks yet
        }

        // Get region files
        File[] regionFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));

        if (regionFiles == null || regionFiles.length == 0) {
            EmeraldMod.LOGGER.info("[OreDetector] No region files - new world");
            return false; // New world
        }

        EmeraldMod.LOGGER.info("[OreDetector] Found {} region files, sampling chunks...",
                regionFiles.length);

        // Sample random chunks from region files
        List<ChunkPos> chunksToCheck = getRandomChunks(regionFiles, SAMPLE_CHUNKS);

        int oresFound = 0;
        int chunksChecked = 0;

        for (ChunkPos pos : chunksToCheck) {
            try {
                // Try to load chunk
                WorldChunk chunk = world.getChunk(pos.x, pos.z);

                if (chunk == null) {
                    continue;
                }

                // Check if this chunk has ruby ores
                if (chunkHasRubyOres(world, chunk)) {
                    oresFound++;
                    EmeraldMod.LOGGER.info("[OreDetector] Found Ruby Ores in chunk ({}, {})",
                            pos.x, pos.z);

                    // Found ore = world has ores
                    if (oresFound >= MIN_ORES_FOUND) {
                        EmeraldMod.LOGGER.info("[OreDetector] ✓ World HAS Ruby Ores (found in {} chunks)",
                                oresFound);
                        return true;
                    }
                }

                chunksChecked++;

            } catch (Exception e) {
                // Skip chunk if error
                continue;
            }
        }

        EmeraldMod.LOGGER.info("[OreDetector] ✗ World does NOT have Ruby Ores (checked {} chunks)",
                chunksChecked);
        return false;
    }

    /**
     * Get random chunks from region files to sample
     */
    private static List<ChunkPos> getRandomChunks(File[] regionFiles, int maxSamples) {
        List<ChunkPos> chunks = new ArrayList<>();
        Pattern pattern = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

        // Get chunks from multiple region files
        int regionsToCheck = Math.min(regionFiles.length, 5); // Check max 5 regions

        for (int i = 0; i < regionsToCheck && chunks.size() < maxSamples; i++) {
            File regionFile = regionFiles[i];
            Matcher matcher = pattern.matcher(regionFile.getName());

            if (matcher.matches()) {
                int regionX = Integer.parseInt(matcher.group(1));
                int regionZ = Integer.parseInt(matcher.group(2));

                // Sample 4 chunks per region (corners and center)
                chunks.add(new ChunkPos(regionX * 32, regionZ * 32)); // Top-left
                chunks.add(new ChunkPos(regionX * 32 + 31, regionZ * 32)); // Top-right
                chunks.add(new ChunkPos(regionX * 32, regionZ * 32 + 31)); // Bottom-left
                chunks.add(new ChunkPos(regionX * 32 + 16, regionZ * 32 + 16)); // Center
            }
        }

        return chunks;
    }

    /**
     * Check if a single chunk has ruby ores
     */
    private static boolean chunkHasRubyOres(ServerWorld world, WorldChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        int minY = world.getBottomY();
        int maxY;

        if (world.getRegistryKey() == World.NETHER) {
            maxY = 128;
        } else {
            maxY = 320;
        }

        // Sample every 4 blocks in X,Z and every 8 blocks in Y for efficiency
        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int y = minY; y < maxY; y += 8) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);

                    try {
                        BlockState state = world.getBlockState(pos);

                        if (isRubyOre(state)) {
                            return true; // Found ore!
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if block is ruby ore
     */
    private static boolean isRubyOre(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.RUBY_ORE ||
                block == ModBlocks.DEEPSLATE_RUBY_ORE ||
                block == ModBlocks.NETHER_RUBY_ORE ||
                block == ModBlocks.RUBY_DEBRIS;
    }
}