package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class AntiGravityHandler {

    private static final Set<Block> FALLING_BLOCKS = new HashSet<>(Arrays.asList(
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL,
            Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
            Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER,
            Blocks.MAGENTA_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE_POWDER,
            Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER,
            Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER,
            Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.CYAN_CONCRETE_POWDER,
            Blocks.PURPLE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER,
            Blocks.BROWN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER,
            Blocks.RED_CONCRETE_POWDER, Blocks.BLACK_CONCRETE_POWDER,
            Blocks.DRAGON_EGG, Blocks.SCAFFOLDING
    ));

    private static final Set<BlockPos> STABILIZED_BLOCKS = new HashSet<>();
    private static int particleSpawnCounter = 0;

    private static final int MAX_HEIGHT_CHECK = 256;
    private static final int MAX_HORIZONTAL_CHECK = 10;
    private static final int PARTICLE_SPAWN_INTERVAL = 20;

    // Track blocks yang sedang di-break untuk prevent infinite loop
    private static final ThreadLocal<Set<BlockPos>> BREAKING_BLOCKS = ThreadLocal.withInitial(HashSet::new);

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return true;

            ItemStack tool = player.getMainHandStack();
            boolean isEmeraldShovel = tool.getItem() == ModItems.EMERALD_SHOVEL;
            boolean isRubyShovel = tool.getItem() == ModItems.RUBY_SHOVEL;

            if (!isEmeraldShovel && !isRubyShovel) {
                return true;
            }

            ServerWorld serverWorld = (ServerWorld) world;
            EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

            if (!stateManager.isToolsEnabled(player.getUuid())) {
                if (STABILIZED_BLOCKS.remove(pos)) {
                    EmeraldMod.LOGGER.debug("Removed stabilized block at {}", pos);
                }
                return true;
            }

            // Ruby Shovel: 3x3 area break
            if (isRubyShovel) {
                // Check if this block is already being broken (prevent recursion)
                if (BREAKING_BLOCKS.get().contains(pos)) {
                    return true;
                }

                // Handle 3x3 area break
                handle3x3AreaBreak(serverWorld, player, pos, state, tool);
            }

            stabilizeSurroundingFallingBlocks(serverWorld, pos);

            if (STABILIZED_BLOCKS.remove(pos)) {
                EmeraldMod.LOGGER.debug("Removed stabilized block at {}", pos);
            }

            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return;

            ItemStack tool = player.getMainHandStack();
            if (tool.getItem() != ModItems.EMERALD_SHOVEL && tool.getItem() != ModItems.RUBY_SHOVEL) {
                return;
            }

            ServerWorld serverWorld = (ServerWorld) world;
            EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

            if (!stateManager.isToolsEnabled(player.getUuid())) {
                return;
            }

            spawnStabilizationEffects(serverWorld, pos);

            // Clear breaking blocks tracking
            BREAKING_BLOCKS.get().clear();
        });

        EmeraldMod.LOGGER.info("✓ Registered Anti-Gravity Handler (Toggleable)");
        EmeraldMod.LOGGER.info("  - Emerald Shovel: Anti-gravity only");
        EmeraldMod.LOGGER.info("  - Ruby Shovel: Anti-gravity + 3x3 area break");
    }

    /**
     * Handle 3x3 area break untuk Ruby Shovel
     * Deteksi arah breaking (floor/ceiling/wall) dan break 3x3 sesuai orientasi
     */
    private static void handle3x3AreaBreak(ServerWorld world, PlayerEntity player, BlockPos centerPos,
                                           BlockState centerState, ItemStack shovel) {
        // Add center ke tracking
        BREAKING_BLOCKS.get().add(centerPos);

        // Determine breaking direction dari player look direction
        Vec3d lookVec = player.getRotationVec(1.0f);
        Direction breakDirection = getBreakDirection(lookVec);

        // Get 3x3 positions based on direction
        List<BlockPos> positions = get3x3Positions(centerPos, breakDirection);

        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                shovel
        );

        int totalBlocksBroken = 0;
        Map<ItemStack, Integer> totalDrops = new HashMap<>();

        // Break all blocks in 3x3 area
        for (BlockPos targetPos : positions) {
            // Skip if same as center (will be broken naturally)
            if (targetPos.equals(centerPos)) continue;

            // Skip if already breaking
            if (BREAKING_BLOCKS.get().contains(targetPos)) continue;

            BlockState targetState = world.getBlockState(targetPos);
            Block targetBlock = targetState.getBlock();

            // Skip air and unbreakable blocks
            if (targetState.isAir() || targetState.getHardness(world, targetPos) < 0) continue;

            // Check if player can break this block
            if (!targetState.isToolRequired() || shovel.isSuitableFor(targetState)) {
                // Add to tracking
                BREAKING_BLOCKS.get().add(targetPos);

                // ✅ STABILIZE SURROUNDING FALLING BLOCKS BEFORE BREAKING
                stabilizeSurroundingFallingBlocks(world, targetPos);

                // Get drops
                List<ItemStack> drops = Block.getDroppedStacks(targetState, world, targetPos,
                        world.getBlockEntity(targetPos), player, shovel);

                for (ItemStack drop : drops) {
                    addToTotalDrops(totalDrops, drop, drop.getCount());
                }

                // Break block
                world.breakBlock(targetPos, false, player);
                totalBlocksBroken++;

                // ✅ REMOVE FROM STABILIZED IF IT WAS STABILIZED
                if (STABILIZED_BLOCKS.remove(targetPos)) {
                    EmeraldMod.LOGGER.debug("Removed stabilized block at {}", targetPos);
                }

                // Spawn break particles
                world.spawnParticles(ParticleTypes.POOF,
                        targetPos.getX() + 0.5,
                        targetPos.getY() + 0.5,
                        targetPos.getZ() + 0.5,
                        10,
                        0.3, 0.3, 0.3,
                        0.1
                );

                // Add flash effect
                world.spawnParticles(ParticleTypes.FLASH,
                        targetPos.getX() + 0.5,
                        targetPos.getY() + 0.5,
                        targetPos.getZ() + 0.5,
                        1,
                        0.0, 0.0, 0.0,
                        0.0
                );

                // ✅ SPAWN STABILIZATION EFFECTS FOR THIS POSITION
                spawnStabilizationEffects(world, targetPos);
            }
        }

        // Drop all collected items at center position
        if (!totalDrops.isEmpty()) {
            for (Map.Entry<ItemStack, Integer> entry : totalDrops.entrySet()) {
                ItemStack itemToDrop = entry.getKey().copy();
                int totalCount = entry.getValue();

                int maxStackSize = itemToDrop.getMaxCount();
                while (totalCount > 0) {
                    int stackSize = Math.min(totalCount, maxStackSize);
                    ItemStack dropStack = itemToDrop.copy();
                    dropStack.setCount(stackSize);

                    ItemEntity itemEntity = new ItemEntity(
                            world,
                            centerPos.getX() + 0.5,
                            centerPos.getY() + 0.5,
                            centerPos.getZ() + 0.5,
                            dropStack
                    );

                    itemEntity.setVelocity(
                            (world.random.nextDouble() - 0.5) * 0.1,
                            0.2,
                            (world.random.nextDouble() - 0.5) * 0.1
                    );

                    world.spawnEntity(itemEntity);
                    totalCount -= stackSize;
                }
            }
        }

        // Damage tool based on blocks broken
        if (totalBlocksBroken > 0) {
            int durabilityDamage = Math.min(totalBlocksBroken, shovel.getMaxDamage() - shovel.getDamage());
            if (durabilityDamage > 0) {
                shovel.damage(durabilityDamage, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
            }

            // Play sound effect
            world.playSound(null, centerPos, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.BLOCKS, 0.8f, 0.9f);

            EmeraldMod.LOGGER.debug("Ruby Shovel 3x3 break: {} blocks broken (direction: {})",
                    totalBlocksBroken, breakDirection);
        }
    }

    /**
     * Determine breaking direction dari player look vector
     */
    private static Direction getBreakDirection(Vec3d lookVec) {
        double absX = Math.abs(lookVec.x);
        double absY = Math.abs(lookVec.y);
        double absZ = Math.abs(lookVec.z);

        // Prioritas: Y axis (floor/ceiling) > horizontal
        if (absY > absX && absY > absZ) {
            return lookVec.y > 0 ? Direction.UP : Direction.DOWN;
        }

        // Horizontal direction
        if (absX > absZ) {
            return lookVec.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return lookVec.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     * Get 3x3 positions based on breaking direction
     */
    private static List<BlockPos> get3x3Positions(BlockPos center, Direction direction) {
        List<BlockPos> positions = new ArrayList<>();

        switch (direction) {
            case DOWN, UP -> {
                // Breaking floor/ceiling - 3x3 horizontal plane
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        positions.add(center.add(dx, 0, dz));
                    }
                }
            }
            case NORTH, SOUTH -> {
                // Breaking wall (North/South facing) - 3x3 vertical XY plane
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        positions.add(center.add(dx, dy, 0));
                    }
                }
            }
            case WEST, EAST -> {
                // Breaking wall (West/East facing) - 3x3 vertical ZY plane
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        positions.add(center.add(0, dy, dz));
                    }
                }
            }
        }

        return positions;
    }

    /**
     * Helper method untuk menambahkan drops ke total
     */
    private static void addToTotalDrops(Map<ItemStack, Integer> totalDrops, ItemStack item, int count) {
        boolean found = false;
        for (Map.Entry<ItemStack, Integer> entry : totalDrops.entrySet()) {
            if (ItemStack.areItemsEqual(entry.getKey(), item)) {
                entry.setValue(entry.getValue() + count);
                found = true;
                break;
            }
        }
        if (!found) {
            totalDrops.put(item.copy(), count);
        }
    }

    private static void stabilizeSurroundingFallingBlocks(ServerWorld world, BlockPos breakPos) {
        Set<BlockPos> toStabilize = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();

        toCheck.add(breakPos);
        visited.add(breakPos);

        while (!toCheck.isEmpty() && toStabilize.size() < 500) {
            BlockPos current = toCheck.poll();

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = current.offset(direction);

                if (visited.contains(neighborPos)) continue;
                visited.add(neighborPos);

                int dx = Math.abs(neighborPos.getX() - breakPos.getX());
                int dy = Math.abs(neighborPos.getY() - breakPos.getY());
                int dz = Math.abs(neighborPos.getZ() - breakPos.getZ());

                if (dx > MAX_HORIZONTAL_CHECK || dy > MAX_HEIGHT_CHECK || dz > MAX_HORIZONTAL_CHECK) {
                    continue;
                }

                BlockState neighborState = world.getBlockState(neighborPos);
                Block neighborBlock = neighborState.getBlock();

                if (FALLING_BLOCKS.contains(neighborBlock)) {
                    if (needsStabilization(world, neighborPos, breakPos)) {
                        toStabilize.add(neighborPos.toImmutable());
                        toCheck.add(neighborPos);
                    }
                }
            }
        }

        if (!toStabilize.isEmpty()) {
            for (BlockPos pos : toStabilize) {
                if (STABILIZED_BLOCKS.add(pos)) {
                    world.scheduleBlockTick(pos, world.getBlockState(pos).getBlock(), 1);
                }
            }

            EmeraldMod.LOGGER.info("Stabilized {} blocks", toStabilize.size());
        }
    }

    private static boolean needsStabilization(World world, BlockPos pos, BlockPos breakPos) {
        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);
        Block belowBlock = belowState.getBlock();

        if (below.equals(breakPos)) return true;
        if (belowState.isAir()) return true;
        if (STABILIZED_BLOCKS.contains(below)) return true;
        if (FALLING_BLOCKS.contains(belowBlock)) return true;
        if (!belowState.isSolidBlock(world, below)) return true;

        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = pos.offset(dir);
            if (adjacentPos.equals(breakPos) && dir == Direction.DOWN) {
                return true;
            }
        }

        return false;
    }

    private static void spawnStabilizationEffects(ServerWorld world, BlockPos pos) {
        int count = 0;

        for (int x = -MAX_HORIZONTAL_CHECK; x <= MAX_HORIZONTAL_CHECK; x++) {
            for (int y = -5; y <= MAX_HEIGHT_CHECK; y++) {
                for (int z = -MAX_HORIZONTAL_CHECK; z <= MAX_HORIZONTAL_CHECK; z++) {
                    BlockPos checkPos = pos.add(x, y, z);

                    if (STABILIZED_BLOCKS.contains(checkPos)) {
                        count++;

                        if (Math.abs(x) <= 20 && Math.abs(y) <= 20 && Math.abs(z) <= 20) {
                            spawnAntiGravityParticles(world, checkPos);
                        }
                    }
                }
            }
        }

        if (count > 0) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.5f, 1.5f);
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.3f, 1.2f);
        }
    }

    private static void spawnAntiGravityParticles(ServerWorld world, BlockPos pos) {
        world.spawnParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                15, 0.3, 0.3, 0.3, 0.1);
        world.spawnParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                8, 0.2, 0.1, 0.2, 0.5);
        world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                3, 0.15, 0.1, 0.15, 0.02);
    }

    public static void tick(ServerWorld world) {
        if (STABILIZED_BLOCKS.isEmpty()) return;

        particleSpawnCounter++;

        if (particleSpawnCounter >= PARTICLE_SPAWN_INTERVAL) {
            particleSpawnCounter = 0;

            Iterator<BlockPos> iterator = STABILIZED_BLOCKS.iterator();

            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();

                BlockState currentState = world.getBlockState(pos);
                if (currentState.isAir() || !FALLING_BLOCKS.contains(currentState.getBlock())) {
                    iterator.remove();
                    continue;
                }

                world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                        1, 0.1, 0.1, 0.1, 0.01);

                if (world.getRandom().nextFloat() < 0.3f) {
                    world.spawnParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            2, 0.2, 0.2, 0.2, 0.3);
                }
            }
        }
    }

    public static boolean isStabilized(BlockPos pos) {
        return STABILIZED_BLOCKS.contains(pos);
    }

    public static boolean unstabilize(BlockPos pos) {
        return STABILIZED_BLOCKS.remove(pos);
    }

    public static void cleanup() {
        STABILIZED_BLOCKS.clear();
        particleSpawnCounter = 0;
    }

    public static int getStabilizedBlockCount() {
        return STABILIZED_BLOCKS.size();
    }

    public static Set<BlockPos> getStabilizedBlocks() {
        return new HashSet<>(STABILIZED_BLOCKS);
    }
}