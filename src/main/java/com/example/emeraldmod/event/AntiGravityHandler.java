package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

public class AntiGravityHandler {

    // Set of falling blocks yang akan di-stabilize
    private static final Set<Block> FALLING_BLOCKS = new HashSet<>(Arrays.asList(
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.GRAVEL,
            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL,
            Blocks.WHITE_CONCRETE_POWDER,
            Blocks.ORANGE_CONCRETE_POWDER,
            Blocks.MAGENTA_CONCRETE_POWDER,
            Blocks.LIGHT_BLUE_CONCRETE_POWDER,
            Blocks.YELLOW_CONCRETE_POWDER,
            Blocks.LIME_CONCRETE_POWDER,
            Blocks.PINK_CONCRETE_POWDER,
            Blocks.GRAY_CONCRETE_POWDER,
            Blocks.LIGHT_GRAY_CONCRETE_POWDER,
            Blocks.CYAN_CONCRETE_POWDER,
            Blocks.PURPLE_CONCRETE_POWDER,
            Blocks.BLUE_CONCRETE_POWDER,
            Blocks.BROWN_CONCRETE_POWDER,
            Blocks.GREEN_CONCRETE_POWDER,
            Blocks.RED_CONCRETE_POWDER,
            Blocks.BLACK_CONCRETE_POWDER,
            Blocks.DRAGON_EGG,
            Blocks.SCAFFOLDING
    ));

    // Set untuk menyimpan blocks yang di-stabilize PERMANENTLY
    private static final Set<BlockPos> STABILIZED_BLOCKS = new HashSet<>();

    // Tick counter untuk particle spawning
    private static int particleSpawnCounter = 0;

    // Constants
    private static final int MAX_HEIGHT_CHECK = 256;
    private static final int MAX_HORIZONTAL_CHECK = 10;
    private static final int PARTICLE_SPAWN_INTERVAL = 20;

    public static void register() {
        // UNIVERSAL STABILIZATION: Works on ANY block broken with Emerald Shovel!
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return true;

            ItemStack tool = player.getMainHandStack();
            if (tool.getItem() != ModItems.EMERALD_SHOVEL) {
                return true;
            }

            ServerWorld serverWorld = (ServerWorld) world;

            // UNIVERSAL: Check and stabilize falling blocks regardless of what block is broken
            // This means breaking stone, dirt, wood, etc will also stabilize falling blocks above
            stabilizeSurroundingFallingBlocks(serverWorld, pos);

            // Jika block yang dihancurkan adalah stabilized block, remove dari set
            if (STABILIZED_BLOCKS.remove(pos)) {
                EmeraldMod.LOGGER.debug("Removed stabilized block at {} (player breaking it)", pos);
            }

            return true;
        });

        // AFTER event untuk effects
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return;

            ItemStack tool = player.getMainHandStack();
            if (tool.getItem() != ModItems.EMERALD_SHOVEL) {
                return;
            }

            ServerWorld serverWorld = (ServerWorld) world;
            spawnStabilizationEffects(serverWorld, pos);
        });

        EmeraldMod.LOGGER.info("✓ Registered Anti-Gravity Handler (UNIVERSAL STABILIZATION - ALL BLOCKS)");
    }

    private static void stabilizeSurroundingFallingBlocks(ServerWorld world, BlockPos breakPos) {
        Set<BlockPos> toStabilize = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();

        // Start from break position
        toCheck.add(breakPos);
        visited.add(breakPos);

        // BFS to find all falling blocks that need stabilization
        while (!toCheck.isEmpty() && toStabilize.size() < 500) {
            BlockPos current = toCheck.poll();

            // Check all 6 directions
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = current.offset(direction);

                if (visited.contains(neighborPos)) {
                    continue;
                }
                visited.add(neighborPos);

                // Check distance limits
                int dx = Math.abs(neighborPos.getX() - breakPos.getX());
                int dy = Math.abs(neighborPos.getY() - breakPos.getY());
                int dz = Math.abs(neighborPos.getZ() - breakPos.getZ());

                if (dx > MAX_HORIZONTAL_CHECK || dy > MAX_HEIGHT_CHECK || dz > MAX_HORIZONTAL_CHECK) {
                    continue;
                }

                BlockState neighborState = world.getBlockState(neighborPos);
                Block neighborBlock = neighborState.getBlock();

                // Check if it's a falling block
                if (FALLING_BLOCKS.contains(neighborBlock)) {
                    // Check if this falling block would fall if we break the current block
                    if (needsStabilization(world, neighborPos, breakPos)) {
                        toStabilize.add(neighborPos.toImmutable());
                        toCheck.add(neighborPos); // Continue searching from this position

                        EmeraldMod.LOGGER.debug("Found falling block at {} that needs stabilization", neighborPos);
                    }
                }
            }
        }

        // Stabilize all found blocks
        if (!toStabilize.isEmpty()) {
            for (BlockPos pos : toStabilize) {
                if (STABILIZED_BLOCKS.add(pos)) {
                    world.scheduleBlockTick(pos, world.getBlockState(pos).getBlock(), 1);
                    EmeraldMod.LOGGER.debug("Stabilized falling block at {}", pos);
                }
            }

            EmeraldMod.LOGGER.info("Stabilized {} falling blocks around position {}",
                    toStabilize.size(), breakPos);
        }
    }

    private static boolean needsStabilization(World world, BlockPos pos, BlockPos breakPos) {
        // A falling block needs stabilization if breaking breakPos would cause it to fall

        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);
        Block belowBlock = belowState.getBlock();

        // If the block below is the one being broken, definitely needs stabilization
        if (below.equals(breakPos)) {
            return true;
        }

        // If block below is air, needs stabilization
        if (belowState.isAir()) {
            return true;
        }

        // If block below is already stabilized, this needs stabilization too
        if (STABILIZED_BLOCKS.contains(below)) {
            return true;
        }

        // If block below is another falling block, needs stabilization
        if (FALLING_BLOCKS.contains(belowBlock)) {
            return true;
        }

        // If block below cannot support (torch, flower, etc), needs stabilization
        if (!belowState.isSolidBlock(world, below)) {
            return true;
        }

        // Check if any adjacent block that provides support is being broken
        // This handles cases like sand next to a wall
        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = pos.offset(dir);
            if (adjacentPos.equals(breakPos)) {
                // An adjacent support block is being broken
                // Check if this block would fall without that support
                if (dir == Direction.DOWN) {
                    // Direct support below is being removed
                    return true;
                }
            }
        }

        return false;
    }

    private static void spawnStabilizationEffects(ServerWorld world, BlockPos pos) {
        int count = 0;

        // Check blocks around the break position for effects
        for (int x = -MAX_HORIZONTAL_CHECK; x <= MAX_HORIZONTAL_CHECK; x++) {
            for (int y = -5; y <= MAX_HEIGHT_CHECK; y++) {
                for (int z = -MAX_HORIZONTAL_CHECK; z <= MAX_HORIZONTAL_CHECK; z++) {
                    BlockPos checkPos = pos.add(x, y, z);

                    if (STABILIZED_BLOCKS.contains(checkPos)) {
                        count++;

                        // Only spawn particles for nearby blocks (within 20 blocks)
                        if (Math.abs(x) <= 20 && Math.abs(y) <= 20 && Math.abs(z) <= 20) {
                            spawnAntiGravityParticles(world, checkPos);
                        }
                    }
                }
            }
        }

        if (count > 0) {
            // Play sounds
            world.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.BLOCK_BEACON_ACTIVATE,
                    SoundCategory.BLOCKS,
                    0.5f,
                    1.5f
            );

            world.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.BLOCKS,
                    0.3f,
                    1.2f
            );

            EmeraldMod.LOGGER.info("Stabilized {} total blocks around position {}", count, pos);
        }
    }

    private static void spawnAntiGravityParticles(ServerWorld world, BlockPos pos) {
        // Spawn portal particles
        world.spawnParticles(
                ParticleTypes.PORTAL,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                15,
                0.3, 0.3, 0.3,
                0.1
        );

        // Spawn enchant particles
        world.spawnParticles(
                ParticleTypes.ENCHANT,
                pos.getX() + 0.5,
                pos.getY() + 0.3,
                pos.getZ() + 0.5,
                8,
                0.2, 0.1, 0.2,
                0.5
        );

        // Spawn END_ROD particles
        world.spawnParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5,
                pos.getY() + 0.1,
                pos.getZ() + 0.5,
                3,
                0.15, 0.1, 0.15,
                0.02
        );
    }

    public static void tick(ServerWorld world) {
        if (STABILIZED_BLOCKS.isEmpty()) {
            return;
        }

        particleSpawnCounter++;

        if (particleSpawnCounter >= PARTICLE_SPAWN_INTERVAL) {
            particleSpawnCounter = 0;

            Iterator<BlockPos> iterator = STABILIZED_BLOCKS.iterator();

            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();

                BlockState currentState = world.getBlockState(pos);
                if (currentState.isAir() || !FALLING_BLOCKS.contains(currentState.getBlock())) {
                    iterator.remove();
                    EmeraldMod.LOGGER.debug("Removed stabilized block at {} (no longer exists)", pos);
                    continue;
                }

                // Spawn subtle particles
                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        pos.getX() + 0.5,
                        pos.getY() + 0.2,
                        pos.getZ() + 0.5,
                        1,
                        0.1, 0.1, 0.1,
                        0.01
                );

                if (world.getRandom().nextFloat() < 0.3f) {
                    world.spawnParticles(
                            ParticleTypes.ENCHANT,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            2,
                            0.2, 0.2, 0.2,
                            0.3
                    );
                }
            }
        }
    }

    public static boolean isStabilized(BlockPos pos) {
        return STABILIZED_BLOCKS.contains(pos);
    }

    public static boolean unstabilize(BlockPos pos) {
        boolean removed = STABILIZED_BLOCKS.remove(pos);
        if (removed) {
            EmeraldMod.LOGGER.debug("Manually unstabilized block at {}", pos);
        }
        return removed;
    }

    public static void cleanup() {
        int count = STABILIZED_BLOCKS.size();
        STABILIZED_BLOCKS.clear();
        particleSpawnCounter = 0;
        EmeraldMod.LOGGER.info("Cleared {} stabilized blocks on cleanup", count);
    }

    public static int getStabilizedBlockCount() {
        return STABILIZED_BLOCKS.size();
    }

    public static Set<BlockPos> getStabilizedBlocks() {
        return new HashSet<>(STABILIZED_BLOCKS);
    }
}