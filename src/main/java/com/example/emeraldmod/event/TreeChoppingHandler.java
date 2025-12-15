package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class TreeChoppingHandler {

    // Konfigurasi untuk pohon besar
    private static final int MAX_LOGS = 500; // Ditingkatkan untuk pohon raksasa
    private static final int SEARCH_RADIUS_HORIZONTAL = 50; // Radius horizontal yang lebih besar
    private static final int SEARCH_RADIUS_VERTICAL = 80; // Radius vertical untuk pohon tinggi
    private static final int LEAF_CHECK_RADIUS = 15; // Radius cek leaves lebih besar
    private static final int LEAF_CHECK_HEIGHT = 30; // Height cek leaves lebih tinggi

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
            // Wood blocks (all sides bark)
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
            // Cek apakah player menggunakan Emerald Axe
            ItemStack tool = player.getMainHandStack();
            if (tool.getItem() == ModItems.EMERALD_AXE) {
                Block block = state.getBlock();

                // Cek apakah block adalah log
                if (LOG_BLOCKS.contains(block)) {
                    // Cek apakah ini bagian dari pohon (ada leaves di sekitarnya)
                    if (isPartOfTree(world, pos)) {
                        // Handle tree chopping
                        handleTreeChopping(world, player, pos, state, tool);
                        return false; // Cancel default break
                    }
                }
            }
            return true; // Allow normal break
        });

        EmeraldMod.LOGGER.info("✓ Registered Tree Chopping Handler for Emerald Axe (Giant Tree Support)");
    }

    private static boolean isPartOfTree(World world, BlockPos pos) {
        // Cek apakah ada leaves dalam radius yang lebih besar untuk pohon raksasa
        for (int dx = -LEAF_CHECK_RADIUS; dx <= LEAF_CHECK_RADIUS; dx++) {
            for (int dy = -5; dy <= LEAF_CHECK_HEIGHT; dy++) { // Cek ke bawah juga untuk pohon yang akarnya di atas
                for (int dz = -LEAF_CHECK_RADIUS; dz <= LEAF_CHECK_RADIUS; dz++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    if (LEAF_BLOCKS.contains(world.getBlockState(checkPos).getBlock())) {
                        return true; // Ada leaves, ini adalah pohon
                    }
                }
            }
        }

        return false; // Tidak ada leaves, kemungkinan bukan pohon alami
    }

    private static void handleTreeChopping(World world, PlayerEntity player, BlockPos startPos, BlockState startState, ItemStack tool) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;

        // Find all connected logs menggunakan BFS dengan algoritma yang lebih agresif
        Set<BlockPos> logsToBreak = findConnectedLogs(world, startPos);

        if (logsToBreak.isEmpty()) {
            return;
        }

        // Limit jumlah logs untuk mencegah lag ekstrem
        if (logsToBreak.size() > MAX_LOGS) {
            EmeraldMod.LOGGER.warn("Tree extremely large! Found {} logs, limiting to {}", logsToBreak.size(), MAX_LOGS);
            // Ambil hanya MAX_LOGS pertama
            Set<BlockPos> limited = new HashSet<>();
            int count = 0;
            for (BlockPos pos : logsToBreak) {
                if (count++ >= MAX_LOGS) break;
                limited.add(pos);
            }
            logsToBreak = limited;
        }

        EmeraldMod.LOGGER.info("Chopping giant tree with {} logs", logsToBreak.size());

        // Break semua logs
        int successfulBreaks = 0;
        for (BlockPos logPos : logsToBreak) {
            BlockState logState = world.getBlockState(logPos);
            Block logBlock = logState.getBlock();

            if (LOG_BLOCKS.contains(logBlock)) {
                // Break block dan drop items
                boolean broken = serverWorld.breakBlock(logPos, true, player);
                if (broken) {
                    successfulBreaks++;
                }
            }
        }

        // Damage tool berdasarkan jumlah logs yang di-chop (lebih adil untuk pohon besar)
        int durabilityDamage = Math.min(successfulBreaks / 5, 20); // 1 durability per 5 logs, max 50
        if (durabilityDamage == 0 && successfulBreaks > 0) {
            durabilityDamage = 1; // Minimal 1 durability
        }
        tool.damage(durabilityDamage, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        EmeraldMod.LOGGER.info("Giant tree chopped! {} logs broken, {} durability damage", successfulBreaks, durabilityDamage);
    }

    private static Set<BlockPos> findConnectedLogs(World world, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        toCheck.add(startPos);
        visited.add(startPos);

        int iterationCount = 0;
        final int MAX_ITERATIONS = 10000; // Safety limit untuk mencegah infinite loop

        while (!toCheck.isEmpty() && result.size() < MAX_LOGS && iterationCount < MAX_ITERATIONS) {
            iterationCount++;
            BlockPos current = toCheck.poll();
            BlockState currentState = world.getBlockState(current);
            Block currentBlock = currentState.getBlock();

            // Cek apakah block ini adalah log
            if (LOG_BLOCKS.contains(currentBlock)) {
                result.add(current);

                // Cek semua neighbor blocks dengan radius lebih besar untuk pohon raksasa
                for (BlockPos neighbor : getExtendedNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);

                        // Cek apakah neighbor dalam radius yang wajar
                        if (isWithinSearchRadius(startPos, neighbor)) {
                            BlockState neighborState = world.getBlockState(neighbor);
                            Block neighborBlock = neighborState.getBlock();

                            // Tambahkan ke queue jika log
                            if (LOG_BLOCKS.contains(neighborBlock)) {
                                toCheck.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        if (iterationCount >= MAX_ITERATIONS) {
            EmeraldMod.LOGGER.warn("Tree search reached max iterations! Found {} logs so far", result.size());
        }

        return result;
    }

    private static List<BlockPos> getExtendedNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 6 directions utama
        neighbors.add(pos.up());
        neighbors.add(pos.down());
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());

        // Diagonal horizontal (untuk pohon yang tumbuh miring seperti acacia)
        neighbors.add(pos.north().east());
        neighbors.add(pos.north().west());
        neighbors.add(pos.south().east());
        neighbors.add(pos.south().west());

        // Diagonal vertikal (untuk pohon yang cabangnya diagonal)
        neighbors.add(pos.up().north());
        neighbors.add(pos.up().south());
        neighbors.add(pos.up().east());
        neighbors.add(pos.up().west());
        neighbors.add(pos.down().north());
        neighbors.add(pos.down().south());
        neighbors.add(pos.down().east());
        neighbors.add(pos.down().west());

        // Diagonal 3D (untuk pohon raksasa dengan cabang kompleks)
        neighbors.add(pos.up().north().east());
        neighbors.add(pos.up().north().west());
        neighbors.add(pos.up().south().east());
        neighbors.add(pos.up().south().west());
        neighbors.add(pos.down().north().east());
        neighbors.add(pos.down().north().west());
        neighbors.add(pos.down().south().east());
        neighbors.add(pos.down().south().west());

        // Extended vertical untuk pohon sangat tinggi
        neighbors.add(pos.up(2));
        neighbors.add(pos.down(2));

        // Extended horizontal untuk cabang yang jauh
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