package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class TreeChoppingHandler {

    private static final Random RANDOM = new Random();

    // Konfigurasi untuk pohon besar
    private static final int MAX_LOGS = 500;
    private static final int SEARCH_RADIUS_HORIZONTAL = 50;
    private static final int SEARCH_RADIUS_VERTICAL = 80;
    private static final int LEAF_CHECK_RADIUS = 15;
    private static final int LEAF_CHECK_HEIGHT = 30;

    // Set of log blocks yang bisa di-chop
    private static final Set<Block> LOG_BLOCKS = new HashSet<>(Arrays.asList(
            Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG,
            Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG,
            Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG,
            Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG,
            Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG,
            Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG,
            Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG,
            Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM,
            Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM,
            // Wood blocks
            Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD,
            Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD,
            Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD,
            Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD,
            Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD,
            Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD,
            Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD,
            Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD,
            Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE,
            Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE
    ));

    // Set of leaf blocks untuk deteksi pohon
    private static final Set<Block> LEAF_BLOCKS = new HashSet<>(Arrays.asList(
            Blocks.OAK_LEAVES,
            Blocks.SPRUCE_LEAVES,
            Blocks.BIRCH_LEAVES,
            Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.DARK_OAK_LEAVES,
            Blocks.MANGROVE_LEAVES,
            Blocks.CHERRY_LEAVES,
            Blocks.AZALEA_LEAVES,
            Blocks.FLOWERING_AZALEA_LEAVES,
            Blocks.NETHER_WART_BLOCK,
            Blocks.WARPED_WART_BLOCK
    ));

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            ItemStack tool = player.getMainHandStack();
            if (tool.getItem() == ModItems.EMERALD_AXE) {
                Block block = state.getBlock();

                if (LOG_BLOCKS.contains(block)) {
                    if (isPartOfTree(world, pos)) {
                        handleTreeChopping(world, player, pos, state, tool);
                        return false; // Cancel default break
                    }
                }
            }
            return true;
        });

        EmeraldMod.LOGGER.info("✓ Registered Tree Chopping Handler with Fortune & Silk Touch Support");
    }

    private static boolean isPartOfTree(World world, BlockPos pos) {
        for (int dx = -LEAF_CHECK_RADIUS; dx <= LEAF_CHECK_RADIUS; dx++) {
            for (int dy = -5; dy <= LEAF_CHECK_HEIGHT; dy++) {
                for (int dz = -LEAF_CHECK_RADIUS; dz <= LEAF_CHECK_RADIUS; dz++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    if (LEAF_BLOCKS.contains(world.getBlockState(checkPos).getBlock())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void handleTreeChopping(World world, PlayerEntity player, BlockPos startPos, BlockState startState, ItemStack tool) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;

        // Get Fortune level
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                tool
        );

        // Get Silk Touch level
        int silkTouchLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH),
                tool
        );

        // Get Efficiency level
        int efficiencyLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.EFFICIENCY),
                tool
        );

        // Find all connected logs
        Set<BlockPos> logsToBreak = findConnectedLogs(world, startPos);

        if (logsToBreak.isEmpty()) {
            return;
        }

        if (logsToBreak.size() > MAX_LOGS) {
            EmeraldMod.LOGGER.warn("Tree extremely large! Found {} logs, limiting to {}", logsToBreak.size(), MAX_LOGS);
            Set<BlockPos> limited = new HashSet<>();
            int count = 0;
            for (BlockPos pos : logsToBreak) {
                if (count++ >= MAX_LOGS) break;
                limited.add(pos);
            }
            logsToBreak = limited;
        }

        EmeraldMod.LOGGER.info("Chopping tree: {} logs (Fortune: {}, Silk Touch: {}, Efficiency: {})",
                logsToBreak.size(), fortuneLevel, silkTouchLevel, efficiencyLevel);

        // Break all logs with proper drops
        int successfulBreaks = 0;
        int totalItemsDropped = 0;

        for (BlockPos logPos : logsToBreak) {
            BlockState logState = world.getBlockState(logPos);
            Block logBlock = logState.getBlock();

            if (LOG_BLOCKS.contains(logBlock)) {
                List<ItemStack> drops;

                if (silkTouchLevel > 0) {
                    // Silk Touch: drop the block itself
                    drops = new ArrayList<>();
                    drops.add(new ItemStack(logBlock));
                } else {
                    // Normal drop with Fortune
                    drops = Block.getDroppedStacks(logState, serverWorld, logPos, null, player, tool);

                    // Apply ADDITIONAL Fortune bonus
                    if (fortuneLevel > 0) {
                        applyFortuneBonus(drops, fortuneLevel);
                    }
                }

                // Remove block
                serverWorld.removeBlock(logPos, false);

                // Drop items at position
                for (ItemStack drop : drops) {
                    Block.dropStack(serverWorld, logPos, drop);
                    totalItemsDropped += drop.getCount();
                }

                successfulBreaks++;
            }
        }

        // Calculate durability damage
        // Unbreaking enchantment is handled automatically by damage() method
        int durabilityDamage = Math.max(1, Math.min(successfulBreaks / 5, 20));
        tool.damage(durabilityDamage, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        // Add experience based on logs chopped
        int experience = Math.min(successfulBreaks / 10, 10);
        if (experience > 0) {
            player.addExperience(experience);
        }

        EmeraldMod.LOGGER.info("Tree chopped! {} logs broken, {} items dropped (Fortune: {}, Silk Touch: {}), {} durability damage",
                successfulBreaks, totalItemsDropped, fortuneLevel, silkTouchLevel, durabilityDamage);
    }

    /**
     * Apply ADDITIONAL Fortune bonus to log drops
     * This adds extra items on top of vanilla drops
     */
    private static void applyFortuneBonus(List<ItemStack> drops, int fortuneLevel) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;

            // Calculate bonus items based on Fortune level
            // Fortune increases drop chance for logs
            // Formula: Base + (0 to fortuneLevel) bonus items per log

            int currentCount = drop.getCount();
            int bonusItems = 0;

            // Each Fortune level gives a chance for extra drops
            for (int i = 0; i < currentCount; i++) {
                // Fortune I: 33% chance for +1
                // Fortune II: 50% chance for +1-2
                // Fortune III: 66% chance for +1-3

                float baseChance = 0.25f + (0.15f * fortuneLevel); // 40%, 55%, 70%

                if (RANDOM.nextFloat() < baseChance) {
                    bonusItems += RANDOM.nextInt(fortuneLevel) + 1;
                }
            }

            // Apply bonus
            if (bonusItems > 0) {
                drop.setCount(currentCount + bonusItems);
                EmeraldMod.LOGGER.debug("Fortune bonus for {}: +{} items (Fortune {})",
                        drop.getItem().getName().getString(), bonusItems, fortuneLevel);
            }
        }
    }

    private static Set<BlockPos> findConnectedLogs(World world, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        toCheck.add(startPos);
        visited.add(startPos);

        int iterationCount = 0;
        final int MAX_ITERATIONS = 10000;

        while (!toCheck.isEmpty() && result.size() < MAX_LOGS && iterationCount < MAX_ITERATIONS) {
            iterationCount++;
            BlockPos current = toCheck.poll();
            BlockState currentState = world.getBlockState(current);
            Block currentBlock = currentState.getBlock();

            if (LOG_BLOCKS.contains(currentBlock)) {
                result.add(current);

                for (BlockPos neighbor : getExtendedNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);

                        if (isWithinSearchRadius(startPos, neighbor)) {
                            BlockState neighborState = world.getBlockState(neighbor);
                            Block neighborBlock = neighborState.getBlock();

                            if (LOG_BLOCKS.contains(neighborBlock)) {
                                toCheck.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        if (iterationCount >= MAX_ITERATIONS) {
            EmeraldMod.LOGGER.warn("Tree search reached max iterations! Found {} logs", result.size());
        }

        return result;
    }

    private static List<BlockPos> getExtendedNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 6 cardinal directions
        neighbors.add(pos.up());
        neighbors.add(pos.down());
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());

        // Horizontal diagonals
        neighbors.add(pos.north().east());
        neighbors.add(pos.north().west());
        neighbors.add(pos.south().east());
        neighbors.add(pos.south().west());

        // Vertical diagonals
        neighbors.add(pos.up().north());
        neighbors.add(pos.up().south());
        neighbors.add(pos.up().east());
        neighbors.add(pos.up().west());
        neighbors.add(pos.down().north());
        neighbors.add(pos.down().south());
        neighbors.add(pos.down().east());
        neighbors.add(pos.down().west());

        // 3D diagonals
        neighbors.add(pos.up().north().east());
        neighbors.add(pos.up().north().west());
        neighbors.add(pos.up().south().east());
        neighbors.add(pos.up().south().west());
        neighbors.add(pos.down().north().east());
        neighbors.add(pos.down().north().west());
        neighbors.add(pos.down().south().east());
        neighbors.add(pos.down().south().west());

        // Extended vertical
        neighbors.add(pos.up(2));
        neighbors.add(pos.down(2));

        // Extended horizontal
        neighbors.add(pos.north(2));
        neighbors.add(pos.south(2));
        neighbors.add(pos.east(2));
        neighbors.add(pos.west(2));

        return neighbors;
    }

    private static boolean isWithinSearchRadius(BlockPos start, BlockPos pos) {
        int dx = Math.abs(pos.getX() - start.getX());
        int dy = Math.abs(pos.getY() - start.getY());
        int dz = Math.abs(pos.getZ() - start.getZ());

        return dx <= SEARCH_RADIUS_HORIZONTAL &&
                dy <= SEARCH_RADIUS_VERTICAL &&
                dz <= SEARCH_RADIUS_HORIZONTAL;
    }
}