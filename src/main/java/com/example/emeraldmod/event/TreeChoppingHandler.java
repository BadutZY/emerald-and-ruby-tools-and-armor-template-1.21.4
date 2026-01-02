package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class TreeChoppingHandler {

    private static final int MAX_LOGS = 128;
    private static final int SEARCH_RADIUS = 5;

    // Map log type ke sapling untuk auto-replant (Ruby Axe only)
    private static final Map<Block, Block> LOG_TO_SAPLING = new HashMap<>();

    static {
        // Oak
        LOG_TO_SAPLING.put(Blocks.OAK_LOG, Blocks.OAK_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_OAK_LOG, Blocks.OAK_SAPLING);
        LOG_TO_SAPLING.put(Blocks.OAK_WOOD, Blocks.OAK_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_OAK_WOOD, Blocks.OAK_SAPLING);

        // Spruce
        LOG_TO_SAPLING.put(Blocks.SPRUCE_LOG, Blocks.SPRUCE_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_SAPLING);
        LOG_TO_SAPLING.put(Blocks.SPRUCE_WOOD, Blocks.SPRUCE_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_SPRUCE_WOOD, Blocks.SPRUCE_SAPLING);

        // Birch
        LOG_TO_SAPLING.put(Blocks.BIRCH_LOG, Blocks.BIRCH_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_SAPLING);
        LOG_TO_SAPLING.put(Blocks.BIRCH_WOOD, Blocks.BIRCH_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_BIRCH_WOOD, Blocks.BIRCH_SAPLING);

        // Jungle
        LOG_TO_SAPLING.put(Blocks.JUNGLE_LOG, Blocks.JUNGLE_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_JUNGLE_LOG, Blocks.JUNGLE_SAPLING);
        LOG_TO_SAPLING.put(Blocks.JUNGLE_WOOD, Blocks.JUNGLE_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_JUNGLE_WOOD, Blocks.JUNGLE_SAPLING);

        // Acacia
        LOG_TO_SAPLING.put(Blocks.ACACIA_LOG, Blocks.ACACIA_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_SAPLING);
        LOG_TO_SAPLING.put(Blocks.ACACIA_WOOD, Blocks.ACACIA_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_ACACIA_WOOD, Blocks.ACACIA_SAPLING);

        // Dark Oak
        LOG_TO_SAPLING.put(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_SAPLING);
        LOG_TO_SAPLING.put(Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.DARK_OAK_SAPLING);

        // Mangrove
        LOG_TO_SAPLING.put(Blocks.MANGROVE_LOG, Blocks.MANGROVE_PROPAGULE);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_MANGROVE_LOG, Blocks.MANGROVE_PROPAGULE);
        LOG_TO_SAPLING.put(Blocks.MANGROVE_WOOD, Blocks.MANGROVE_PROPAGULE);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_MANGROVE_WOOD, Blocks.MANGROVE_PROPAGULE);

        // Cherry
        LOG_TO_SAPLING.put(Blocks.CHERRY_LOG, Blocks.CHERRY_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_CHERRY_LOG, Blocks.CHERRY_SAPLING);
        LOG_TO_SAPLING.put(Blocks.CHERRY_WOOD, Blocks.CHERRY_SAPLING);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_CHERRY_WOOD, Blocks.CHERRY_SAPLING);

        // Crimson (Nether) - uses fungus
        LOG_TO_SAPLING.put(Blocks.CRIMSON_STEM, Blocks.CRIMSON_FUNGUS);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_CRIMSON_STEM, Blocks.CRIMSON_FUNGUS);
        LOG_TO_SAPLING.put(Blocks.CRIMSON_HYPHAE, Blocks.CRIMSON_FUNGUS);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.CRIMSON_FUNGUS);

        // Warped (Nether) - uses fungus
        LOG_TO_SAPLING.put(Blocks.WARPED_STEM, Blocks.WARPED_FUNGUS);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_WARPED_STEM, Blocks.WARPED_FUNGUS);
        LOG_TO_SAPLING.put(Blocks.WARPED_HYPHAE, Blocks.WARPED_FUNGUS);
        LOG_TO_SAPLING.put(Blocks.STRIPPED_WARPED_HYPHAE, Blocks.WARPED_FUNGUS);
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            ItemStack tool = player.getMainHandStack();
            boolean isEmeraldAxe = tool.getItem() == ModItems.EMERALD_AXE;
            boolean isRubyAxe = tool.getItem() == ModItems.RUBY_AXE;

            // Check if using Emerald or Ruby Axe
            if (!isEmeraldAxe && !isRubyAxe) {
                return true;
            }

            // Check tools effect enabled
            if (!world.isClient) {
                ServerWorld serverWorld = (ServerWorld) world;
                EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

                if (!stateManager.isToolsEnabled(player.getUuid())) {
                    return true;
                }
            }

            Block block = state.getBlock();
            if (state.isIn(BlockTags.LOGS)) {
                // Pass info apakah ini Ruby Axe untuk auto-replant
                handleTreeChopping(world, player, pos, state, tool, isRubyAxe);
                return false;
            }

            return true;
        });

        EmeraldMod.LOGGER.info("✓ Registered Tree Chopping Handler (Toggleable)");
        EmeraldMod.LOGGER.info("  - Emerald Axe: Tree chopping only");
        EmeraldMod.LOGGER.info("  - Ruby Axe: Tree chopping + Auto-replant sapling");
    }

    private static void handleTreeChopping(World world, PlayerEntity player, BlockPos startPos,
                                           BlockState startState, ItemStack axe, boolean isRubyAxe) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;
        Block targetLog = startState.getBlock();

        Set<BlockPos> processedBlocks = new HashSet<>();
        Queue<BlockPos> toProcess = new LinkedList<>();
        toProcess.add(startPos);
        processedBlocks.add(startPos);

        int totalLogsMined = 0;
        Map<ItemStack, Integer> totalDrops = new HashMap<>();

        // Track posisi terendah untuk auto-replant
        BlockPos lowestLogPos = startPos;
        int lowestY = startPos.getY();

        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                axe
        );

        EmeraldMod.LOGGER.debug("Starting tree chopping at {} (Ruby Axe: {})", startPos, isRubyAxe);

        // BFS untuk menemukan semua logs
        while (!toProcess.isEmpty() && totalLogsMined < MAX_LOGS) {
            BlockPos currentPos = toProcess.poll();
            BlockState currentState = serverWorld.getBlockState(currentPos);
            Block currentBlock = currentState.getBlock();

            if (currentState.isIn(BlockTags.LOGS) && currentBlock == targetLog) {
                totalLogsMined++;

                // Track posisi terendah
                if (currentPos.getY() < lowestY) {
                    lowestY = currentPos.getY();
                    lowestLogPos = currentPos;
                }

                // Get drops dari log
                List<ItemStack> drops = Block.getDroppedStacks(currentState, serverWorld, currentPos, null, player, axe);

                for (ItemStack drop : drops) {
                    addToTotalDrops(totalDrops, drop, drop.getCount());
                }

                // Break block
                serverWorld.breakBlock(currentPos, false, player);

                // Cari logs di sekitar
                for (BlockPos neighborPos : getNeighborPositions(currentPos)) {
                    if (!processedBlocks.contains(neighborPos) && isWithinSearchRadius(startPos, neighborPos)) {
                        BlockState neighborState = serverWorld.getBlockState(neighborPos);
                        if (neighborState.isIn(BlockTags.LOGS)) {
                            toProcess.add(neighborPos);
                            processedBlocks.add(neighborPos);
                        }
                    }
                }
            }
        }

        // Drop all collected items
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
                            serverWorld,
                            startPos.getX() + 0.5,
                            startPos.getY() + 0.5,
                            startPos.getZ() + 0.5,
                            dropStack
                    );

                    itemEntity.setVelocity(
                            (serverWorld.random.nextDouble() - 0.5) * 0.1,
                            0.2,
                            (serverWorld.random.nextDouble() - 0.5) * 0.1
                    );

                    serverWorld.spawnEntity(itemEntity);
                    totalCount -= stackSize;
                }
            }
        }

        // ============================================
        // AUTO-REPLANT SAPLING (RUBY AXE ONLY)
        // ============================================
        if (isRubyAxe && LOG_TO_SAPLING.containsKey(targetLog)) {
            // Cari posisi ground yang cocok untuk plant sapling
            BlockPos replantPos = findGroundPosition(serverWorld, lowestLogPos);

            if (replantPos != null) {
                Block saplingBlock = LOG_TO_SAPLING.get(targetLog);
                BlockState saplingState = saplingBlock.getDefaultState();

                // Validasi placement menggunakan canPlaceAt dari BlockState
                if (saplingState.canPlaceAt(serverWorld, replantPos)) {
                    // Place sapling
                    serverWorld.setBlockState(replantPos, saplingState);

                    EmeraldMod.LOGGER.info("Auto-replanted {} at {}",
                            saplingBlock.getName().getString(), replantPos);
                } else {
                    EmeraldMod.LOGGER.debug("Cannot replant sapling at {} (invalid placement)", replantPos);
                }
            } else {
                EmeraldMod.LOGGER.debug("Could not find valid ground position for replanting");
            }
        }

        // Damage tool
        int durabilityDamage = Math.min(totalLogsMined, axe.getMaxDamage() - axe.getDamage());
        if (durabilityDamage > 0) {
            axe.damage(durabilityDamage, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        }

        EmeraldMod.LOGGER.info("Tree chopping completed: {} logs mined{}",
                totalLogsMined,
                isRubyAxe ? " (sapling replanted)" : "");
    }

    /**
     * Find ground position untuk plant sapling
     */
    private static BlockPos findGroundPosition(ServerWorld world, BlockPos startPos) {
        // Cek dari posisi terendah log ke bawah sampai ketemu ground
        BlockPos checkPos = startPos.down();

        // Max 10 blocks ke bawah
        for (int i = 0; i < 10; i++) {
            BlockState groundState = world.getBlockState(checkPos);
            BlockState aboveState = world.getBlockState(checkPos.up());

            // Cek apakah ini ground yang cocok dan ada space di atasnya
            if (isValidGround(groundState.getBlock()) && aboveState.isAir()) {
                return checkPos.up(); // Return posisi di atas ground
            }

            checkPos = checkPos.down();
        }

        // Jika tidak ketemu, coba posisi original
        BlockState originalGround = world.getBlockState(startPos.down());
        BlockState originalAbove = world.getBlockState(startPos);

        if (isValidGround(originalGround.getBlock()) && originalAbove.isAir()) {
            return startPos;
        }

        return null;
    }

    /**
     * Check if block is valid ground untuk sapling
     */
    private static boolean isValidGround(Block block) {
        return block == Blocks.GRASS_BLOCK ||
                block == Blocks.DIRT ||
                block == Blocks.PODZOL ||
                block == Blocks.COARSE_DIRT ||
                block == Blocks.MYCELIUM ||
                block == Blocks.ROOTED_DIRT ||
                block == Blocks.MOSS_BLOCK ||
                block == Blocks.CRIMSON_NYLIUM ||
                block == Blocks.WARPED_NYLIUM;
    }

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

    private static List<BlockPos> getNeighborPositions(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    neighbors.add(pos.add(x, y, z));
                }
            }
        }
        return neighbors;
    }

    private static boolean isWithinSearchRadius(BlockPos start, BlockPos current) {
        return Math.abs(current.getX() - start.getX()) <= SEARCH_RADIUS &&
                Math.abs(current.getY() - start.getY()) <= SEARCH_RADIUS * 4 &&
                Math.abs(current.getZ() - start.getZ()) <= SEARCH_RADIUS;
    }
}