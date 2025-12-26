package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class AutoSmeltHandler {

    private static final Random RANDOM = new Random();

    // Maximum ore blocks yang bisa di-mine dalam satu kali (untuk prevent lag)
    private static final int MAX_VEIN_SIZE = 64;

    // Radius pencarian ore di sekitar (3 blocks dalam semua arah = 7x7x7 area)
    private static final int SEARCH_RADIUS = 3;

    // Map ore block ke smelted item
    private static final Map<Block, ItemStack> ORE_TO_INGOT = new HashMap<>();

    // Set semua ore blocks untuk vein mining detection
    private static final Set<Block> ALL_ORES = new HashSet<>();

    static {
        // Copper Ore
        ORE_TO_INGOT.put(Blocks.COPPER_ORE, new ItemStack(Items.COPPER_INGOT));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_COPPER_ORE, new ItemStack(Items.COPPER_INGOT));

        // Iron Ore
        ORE_TO_INGOT.put(Blocks.IRON_ORE, new ItemStack(Items.IRON_INGOT));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_IRON_ORE, new ItemStack(Items.IRON_INGOT));

        // Gold Ore
        ORE_TO_INGOT.put(Blocks.GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
        ORE_TO_INGOT.put(Blocks.NETHER_GOLD_ORE, new ItemStack(Items.GOLD_INGOT));

        // Ancient Debris -> Netherite Scrap
        ORE_TO_INGOT.put(Blocks.ANCIENT_DEBRIS, new ItemStack(Items.NETHERITE_SCRAP));

        // Tambahkan ore lainnya
        ORE_TO_INGOT.put(Blocks.COAL_ORE, new ItemStack(Items.COAL));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_COAL_ORE, new ItemStack(Items.COAL));

        ORE_TO_INGOT.put(Blocks.DIAMOND_ORE, new ItemStack(Items.DIAMOND));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_DIAMOND_ORE, new ItemStack(Items.DIAMOND));

        ORE_TO_INGOT.put(Blocks.EMERALD_ORE, new ItemStack(Items.EMERALD));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_EMERALD_ORE, new ItemStack(Items.EMERALD));

        ORE_TO_INGOT.put(Blocks.LAPIS_ORE, new ItemStack(Items.LAPIS_LAZULI));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_LAPIS_ORE, new ItemStack(Items.LAPIS_LAZULI));

        ORE_TO_INGOT.put(Blocks.REDSTONE_ORE, new ItemStack(Items.REDSTONE));
        ORE_TO_INGOT.put(Blocks.DEEPSLATE_REDSTONE_ORE, new ItemStack(Items.REDSTONE));

        ORE_TO_INGOT.put(Blocks.NETHER_QUARTZ_ORE, new ItemStack(Items.QUARTZ));

        // Populate ALL_ORES set
        ALL_ORES.addAll(ORE_TO_INGOT.keySet());
    }

    public static void register() {
        // Gunakan BEFORE untuk cancel drop default
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            // Cek apakah player menggunakan Emerald Pickaxe
            ItemStack tool = player.getMainHandStack();
            if (tool.getItem() == ModItems.EMERALD_PICKAXE) {
                // ✅ CHECK: Apakah tools effect enabled?
                if (!world.isClient) {
                    ServerWorld serverWorld = (ServerWorld) world;
                    EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

                    if (!stateManager.isToolsEnabled(player.getUuid())) {
                        // Tools effect DISABLED, biarkan break normal
                        EmeraldMod.LOGGER.debug("Auto-smelt disabled for player {}", player.getName().getString());
                        return true; // Allow normal vanilla break
                    }
                }

                // Cek apakah ada Silk Touch enchantment
                int silkTouchLevel = EnchantmentHelper.getLevel(
                        world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                                .getOrThrow(Enchantments.SILK_TOUCH),
                        tool
                );

                // Jika ada Silk Touch, biarkan drop normal (ore block)
                if (silkTouchLevel > 0) {
                    return true; // Allow normal drop (ore block)
                }

                Block block = state.getBlock();
                // Cek apakah block adalah ore yang bisa di-smelt
                if (ORE_TO_INGOT.containsKey(block)) {
                    // Handle vein mining dengan auto-smelt
                    handleVeinMining(world, player, pos, state, tool);
                    // Return false untuk cancel drop default dari loot table
                    return false;
                }
            }
            // Return true untuk allow normal drop
            return true;
        });

        EmeraldMod.LOGGER.info("✓ Registered Auto-Smelt + Vein Mining Handler for Emerald Pickaxe (Toggleable)");
    }

    /**
     * Main handler untuk vein mining dengan auto-smelt
     */
    private static void handleVeinMining(World world, PlayerEntity player, BlockPos startPos, BlockState startState, ItemStack tool) {
        if (world.isClient) return;

        ServerWorld serverWorld = (ServerWorld) world;
        Block targetOre = startState.getBlock();

        // Set untuk track blocks yang sudah di-process
        Set<BlockPos> processedBlocks = new HashSet<>();

        // Queue untuk BFS (Breadth-First Search)
        Queue<BlockPos> toProcess = new LinkedList<>();
        toProcess.add(startPos);
        processedBlocks.add(startPos);

        // Counter untuk total ores yang ditemukan
        int totalOresMined = 0;

        // Map untuk track total drops per item type
        Map<ItemStack, Integer> totalDrops = new HashMap<>();

        // Get Fortune level sekali saja
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                tool
        );

        EmeraldMod.LOGGER.debug("Starting vein mining for {} at {}", targetOre.getName().getString(), startPos);

        // BFS untuk menemukan semua ore yang connected
        while (!toProcess.isEmpty() && totalOresMined < MAX_VEIN_SIZE) {
            BlockPos currentPos = toProcess.poll();
            BlockState currentState = serverWorld.getBlockState(currentPos);
            Block currentBlock = currentState.getBlock();

            // Cek apakah block ini adalah ore yang sama dengan target
            if (currentBlock == targetOre && ORE_TO_INGOT.containsKey(currentBlock)) {
                // Process ore ini
                totalOresMined++;

                // Hitung drops untuk ore ini
                int dropCount = calculateDropCount(currentBlock, fortuneLevel);
                ItemStack smeltedItem = ORE_TO_INGOT.get(currentBlock).copy();

                // Tambahkan ke total drops
                addToTotalDrops(totalDrops, smeltedItem, dropCount);

                // Break block tanpa drop
                serverWorld.breakBlock(currentPos, false, player);

                // Add experience
                int experience = calculateExperience(currentBlock, dropCount);
                if (experience > 0) {
                    player.addExperience(experience);
                }

                // Cari ore di sekitar block ini
                for (BlockPos neighborPos : getNeighborPositions(currentPos)) {
                    if (!processedBlocks.contains(neighborPos) &&
                            isWithinSearchRadius(startPos, neighborPos)) {

                        BlockState neighborState = serverWorld.getBlockState(neighborPos);
                        Block neighborBlock = neighborState.getBlock();

                        // Hanya tambahkan jika block tetangga adalah ore yang SAMA
                        if (neighborBlock == targetOre) {
                            toProcess.add(neighborPos);
                            processedBlocks.add(neighborPos);
                        }
                    }
                }
            }
        }

        // Drop semua items yang terkumpul di posisi awal
        if (!totalDrops.isEmpty()) {
            for (Map.Entry<ItemStack, Integer> entry : totalDrops.entrySet()) {
                ItemStack itemToDrop = entry.getKey().copy();
                int totalCount = entry.getValue();

                // Split into multiple stacks if needed (max stack size)
                int maxStackSize = itemToDrop.getMaxCount();
                while (totalCount > 0) {
                    int stackSize = Math.min(totalCount, maxStackSize);
                    ItemStack dropStack = itemToDrop.copy();
                    dropStack.setCount(stackSize);

                    // Spawn item entity
                    ItemEntity itemEntity = new ItemEntity(
                            serverWorld,
                            startPos.getX() + 0.5,
                            startPos.getY() + 0.5,
                            startPos.getZ() + 0.5,
                            dropStack
                    );

                    // Set velocity kecil
                    itemEntity.setVelocity(
                            (RANDOM.nextDouble() - 0.5) * 0.1,
                            0.2,
                            (RANDOM.nextDouble() - 0.5) * 0.1
                    );

                    serverWorld.spawnEntity(itemEntity);
                    totalCount -= stackSize;
                }
            }
        }

        // Damage tool berdasarkan jumlah ore yang di-mine
        int durabilityDamage = Math.min(totalOresMined, tool.getMaxDamage() - tool.getDamage());
        if (durabilityDamage > 0) {
            tool.damage(durabilityDamage, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        }

        EmeraldMod.LOGGER.info("Vein mining completed: {} {} ores mined (Fortune {})",
                totalOresMined,
                targetOre.getName().getString(),
                fortuneLevel);
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

    /**
     * Get semua posisi tetangga (26 blocks di sekitar - 3x3x3 minus center)
     */
    private static List<BlockPos> getNeighborPositions(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip center
                    neighbors.add(pos.add(x, y, z));
                }
            }
        }
        return neighbors;
    }

    /**
     * Cek apakah posisi masih dalam radius pencarian dari start position
     */
    private static boolean isWithinSearchRadius(BlockPos start, BlockPos current) {
        return Math.abs(current.getX() - start.getX()) <= SEARCH_RADIUS &&
                Math.abs(current.getY() - start.getY()) <= SEARCH_RADIUS &&
                Math.abs(current.getZ() - start.getZ()) <= SEARCH_RADIUS;
    }

    private static int calculateDropCount(Block block, int fortuneLevel) {
        // Base drop count
        int baseCount = 1;

        // Copper Ore: 2-5 raw copper (rata-rata 3.5), kita buat 1-2 ingot base
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            baseCount = RANDOM.nextInt(2) + 2; // 2-3 copper ingot base
        }

        // Nether Gold Ore: 2-6 gold nugget base, kita buat jadi 1 gold ingot
        if (block == Blocks.NETHER_GOLD_ORE) {
            baseCount = 1;
        }

        // Lapis: 4-9 lapis base
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            baseCount = RANDOM.nextInt(6) + 4; // 4-9 lapis
        }

        // Redstone: 4-5 redstone base
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            baseCount = RANDOM.nextInt(2) + 4; // 4-5 redstone
        }

        // Tambahkan bonus Fortune untuk SEMUA ore termasuk Ancient Debris
        if (fortuneLevel > 0) {
            // Fortune dapat menambah drop
            int maxBonus = fortuneLevel;

            for (int i = 0; i < fortuneLevel; i++) {
                // Setiap level Fortune punya 33% chance untuk menambah 1 item
                if (RANDOM.nextFloat() < 0.33f) {
                    baseCount++;
                }
            }

            // Bonus tambahan: ada small chance untuk jackpot drop
            if (fortuneLevel >= 3 && RANDOM.nextFloat() < 0.1f) {
                baseCount += RANDOM.nextInt(2) + 1;
            }
        }

        return Math.max(1, baseCount);
    }

    private static int calculateExperience(Block block, int dropCount) {
        // Experience yang didapat saat smelting
        int baseExp = 0;

        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            baseExp = 1;
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            baseExp = 1;
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE ||
                block == Blocks.NETHER_GOLD_ORE) {
            baseExp = 1;
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            baseExp = 2;
        } else if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            baseExp = 1;
        } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            baseExp = 3;
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            baseExp = 3;
        } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            baseExp = 2;
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            baseExp = 1;
        } else if (block == Blocks.NETHER_QUARTZ_ORE) {
            baseExp = 2;
        }

        // Multiply by drop count
        return baseExp * dropCount;
    }
}