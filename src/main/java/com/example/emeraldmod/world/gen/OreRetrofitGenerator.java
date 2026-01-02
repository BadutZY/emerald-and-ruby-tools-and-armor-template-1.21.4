package com.example.emeraldmod.world.gen;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.Chunk;

/**
 * Generator untuk menambahkan ore LANGSUNG ke blocks (tanpa features)
 * Enhanced dengan skip detection untuk chunks yang sudah ada ruby ores
 */
public class OreRetrofitGenerator {

    /**
     * ⭐ NEW: Check if chunk already has ruby ores
     * Skip retrofit jika chunk sudah punya ruby ores
     */
    public static boolean chunkHasRubyOres(ServerWorld world, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        // Use safe Y range based on dimension
        int minY = world.getBottomY(); // -64 for overworld, 0 for nether
        int maxY;

        // Determine max Y based on dimension
        if (world.getRegistryKey() == net.minecraft.world.World.NETHER) {
            maxY = 128; // Nether height limit
        } else {
            maxY = 320; // Overworld height limit (1.18+)
        }

        // Sample beberapa posisi untuk check ruby ores
        // Tidak perlu scan semua blocks, cukup sample untuk efisiensi
        // Sample every 4 blocks in X,Z and every 8 blocks in Y
        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int y = minY; y < maxY; y += 8) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);

                    try {
                        BlockState state = world.getBlockState(pos);

                        if (isRubyOre(state)) {
                            // Found ruby ore - skip this chunk
                            EmeraldMod.LOGGER.debug("Chunk ({}, {}) already has ruby ores, skipping",
                                    chunkPos.x, chunkPos.z);
                            return true;
                        }
                    } catch (Exception e) {
                        // Skip if error accessing block
                        continue;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if blockstate is ruby ore
     */
    private static boolean isRubyOre(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.RUBY_ORE ||
                block == ModBlocks.DEEPSLATE_RUBY_ORE ||
                block == ModBlocks.NETHER_RUBY_ORE ||
                block == ModBlocks.RUBY_DEBRIS;
    }

    /**
     * Generate Ruby ores di chunk - DIRECT BLOCK PLACEMENT
     */
    public static boolean retrofitChunk(ServerWorld world, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();

        try {
            OreRetrofitState state = OreRetrofitState.get(world.getServer(), world);

            if (state.isChunkRetrofitted(chunkPos)) {
                return false;
            }

            // Generate ores based on dimension
            int oresPlaced = 0;

            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                oresPlaced += generateOverworldOres(world, chunk, chunkPos);
            } else if (world.getRegistryKey() == net.minecraft.world.World.NETHER) {
                oresPlaced += generateNetherOres(world, chunk, chunkPos);
            }

            state.markChunkRetrofitted(chunkPos);

            if (oresPlaced > 0) {
                EmeraldMod.LOGGER.debug("Retrofitted chunk ({}, {}) - Placed {} Ruby Ores",
                        chunkPos.x, chunkPos.z, oresPlaced);
            }

            return true;

        } catch (Exception e) {
            EmeraldMod.LOGGER.error("Failed to retrofit chunk ({}, {}): {}",
                    chunkPos.x, chunkPos.z, e.getMessage());
            return false;
        }
    }

    /**
     * Generate Ruby Ores di Overworld - DIRECT PLACEMENT
     */
    private static int generateOverworldOres(ServerWorld world, Chunk chunk, ChunkPos chunkPos) {
        Random random = Random.create(world.getSeed() ^
                ((long)chunkPos.x * 341873128712L + (long)chunkPos.z * 132897987541L));

        int oresPlaced = 0;

        // Ruby Ore - 8 attempts per chunk
        for (int i = 0; i < 8; i++) {
            int x = chunkPos.getStartX() + random.nextInt(16);
            int z = chunkPos.getStartZ() + random.nextInt(16);
            int y = world.getBottomY() + random.nextInt(128); // Y -64 to 64

            BlockPos pos = new BlockPos(x, y, z);
            oresPlaced += placeOreVein(world, pos, ModBlocks.RUBY_ORE, ModBlocks.DEEPSLATE_RUBY_ORE,
                    random, 4 + random.nextInt(5)); // Vein size 4-8
        }

        // Ruby Ore Large - 2 attempts per chunk (larger veins)
        for (int i = 0; i < 2; i++) {
            int x = chunkPos.getStartX() + random.nextInt(16);
            int z = chunkPos.getStartZ() + random.nextInt(16);
            int y = world.getBottomY() + random.nextInt(96); // Y -64 to 32

            BlockPos pos = new BlockPos(x, y, z);
            oresPlaced += placeOreVein(world, pos, ModBlocks.RUBY_ORE, ModBlocks.DEEPSLATE_RUBY_ORE,
                    random, 8 + random.nextInt(5)); // Vein size 8-12
        }

        return oresPlaced;
    }

    /**
     * Generate Ruby Ores di Nether - DIRECT PLACEMENT
     */
    private static int generateNetherOres(ServerWorld world, Chunk chunk, ChunkPos chunkPos) {
        Random random = Random.create(world.getSeed() ^
                ((long)chunkPos.x * 341873128712L + (long)chunkPos.z * 132897987541L));

        int oresPlaced = 0;

        // Nether Ruby Ore - 10 attempts per chunk
        for (int i = 0; i < 10; i++) {
            int x = chunkPos.getStartX() + random.nextInt(16);
            int z = chunkPos.getStartZ() + random.nextInt(16);
            int y = 10 + random.nextInt(108); // Y 10 to 117

            BlockPos pos = new BlockPos(x, y, z);
            oresPlaced += placeNetherOreVein(world, pos, ModBlocks.NETHER_RUBY_ORE, random,
                    2 + random.nextInt(3)); // Vein size 2-4
        }

        // Ruby Debris - 5 attempts per chunk
        for (int i = 0; i < 5; i++) {
            int x = chunkPos.getStartX() + random.nextInt(16);
            int z = chunkPos.getStartZ() + random.nextInt(16);
            int y = -35 + random.nextInt(116); // Y -35 to 80

            BlockPos pos = new BlockPos(x, y, z);
            oresPlaced += placeNetherOreVein(world, pos, ModBlocks.RUBY_DEBRIS, random,
                    1 + random.nextInt(3)); // Vein size 1-3
        }

        return oresPlaced;
    }

    /**
     * Place ore vein (untuk Overworld - stone/deepslate replacement)
     */
    private static int placeOreVein(ServerWorld world, BlockPos center, Block stoneOre,
                                    Block deepslateOre, Random random, int size) {
        int placed = 0;

        for (int i = 0; i < size; i++) {
            int offsetX = random.nextInt(3) - 1; // -1 to 1
            int offsetY = random.nextInt(3) - 1;
            int offsetZ = random.nextInt(3) - 1;

            BlockPos pos = center.add(offsetX, offsetY, offsetZ);

            if (canPlaceOreAt(world, pos)) {
                BlockState existingState = world.getBlockState(pos);

                // Choose ore type based on existing block
                BlockState oreState;
                if (existingState.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                    oreState = deepslateOre.getDefaultState();
                } else if (existingState.isIn(BlockTags.STONE_ORE_REPLACEABLES)) {
                    oreState = stoneOre.getDefaultState();
                } else {
                    continue; // Skip if not replaceable
                }

                // Place ore
                world.setBlockState(pos, oreState, Block.NOTIFY_LISTENERS);
                placed++;
            }
        }

        return placed;
    }

    /**
     * Place ore vein (untuk Nether - netherrack replacement)
     */
    private static int placeNetherOreVein(ServerWorld world, BlockPos center, Block ore,
                                          Random random, int size) {
        int placed = 0;

        for (int i = 0; i < size; i++) {
            int offsetX = random.nextInt(3) - 1;
            int offsetY = random.nextInt(3) - 1;
            int offsetZ = random.nextInt(3) - 1;

            BlockPos pos = center.add(offsetX, offsetY, offsetZ);

            if (canPlaceNetherOreAt(world, pos)) {
                BlockState existingState = world.getBlockState(pos);

                // Check if can replace (netherrack, basalt, etc)
                if (existingState.isOf(Blocks.NETHERRACK) ||
                        existingState.isOf(Blocks.BASALT) ||
                        existingState.isOf(Blocks.BLACKSTONE)) {

                    world.setBlockState(pos, ore.getDefaultState(), Block.NOTIFY_LISTENERS);
                    placed++;
                }
            }
        }

        return placed;
    }

    /**
     * Check if we can place ore at position (Overworld)
     */
    private static boolean canPlaceOreAt(ServerWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos)) {
            return false;
        }

        BlockState state = world.getBlockState(pos);

        // Can only replace stone-like blocks
        return state.isIn(BlockTags.STONE_ORE_REPLACEABLES) ||
                state.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }

    /**
     * Check if we can place ore at position (Nether)
     */
    private static boolean canPlaceNetherOreAt(ServerWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos)) {
            return false;
        }

        BlockState state = world.getBlockState(pos);

        // Can replace netherrack, basalt, blackstone
        return state.isOf(Blocks.NETHERRACK) ||
                state.isOf(Blocks.BASALT) ||
                state.isOf(Blocks.BLACKSTONE);
    }

    public static boolean needsRetrofit(ServerWorld world, ChunkPos chunkPos) {
        OreRetrofitState state = OreRetrofitState.get(world.getServer(), world);
        return !state.isChunkRetrofitted(chunkPos);
    }

    public static int getRetrofittedChunkCount(ServerWorld world) {
        OreRetrofitState state = OreRetrofitState.get(world.getServer(), world);
        return state.getRetrofittedChunkCount();
    }
}