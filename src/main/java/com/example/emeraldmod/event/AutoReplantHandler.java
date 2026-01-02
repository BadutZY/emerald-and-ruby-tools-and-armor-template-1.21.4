package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import com.example.emeraldmod.state.EffectStateManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class AutoReplantHandler {

    private static final Random RANDOM = new Random();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // ⭐ CRITICAL FIX: Only process on MAIN_HAND to prevent double processing
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }

            ItemStack heldItem = player.getStackInHand(hand);
            boolean isEmeraldHoe = heldItem.getItem() == ModItems.EMERALD_HOE;
            boolean isRubyHoe = heldItem.getItem() == ModItems.RUBY_HOE;

            // Check if using Emerald or Ruby Hoe
            if (!isEmeraldHoe && !isRubyHoe) {
                return ActionResult.PASS;
            }

            // ⭐ CRITICAL FIX: Process ONLY on server side
            if (world.isClient) {
                // Return SUCCESS to prevent vanilla behavior but don't process
                return ActionResult.SUCCESS;
            }

            // Check tools effect enabled (server side only)
            ServerWorld serverWorld = (ServerWorld) world;
            EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

            if (!stateManager.isToolsEnabled(player.getUuid())) {
                return ActionResult.PASS;
            }

            BlockPos centerPos = hitResult.getBlockPos();
            BlockState centerState = world.getBlockState(centerPos);
            Block centerBlock = centerState.getBlock();

            // Ruby Hoe: Process 3x3 area (UNBREAKABLE - NO DURABILITY LOSS)
            if (isRubyHoe) {
                boolean processed = handleRubyAreaReplant(serverWorld, player, centerPos, heldItem);
                return processed ? ActionResult.SUCCESS : ActionResult.PASS;
            }

            // Emerald Hoe: Process single block only (WITH DURABILITY LOSS)
            if (isEmeraldHoe) {
                if (handleEmeraldSingleReplant(serverWorld, player, centerPos, centerState, centerBlock, heldItem)) {
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });

        EmeraldMod.LOGGER.info("✅ Registered Auto-Replant Handler (Toggleable)");
        EmeraldMod.LOGGER.info("  - Emerald Hoe: Single block (1x1) WITH durability loss");
        EmeraldMod.LOGGER.info("  - Ruby Hoe: Area replant (3x3) UNBREAKABLE");
    }

    /**
     * ⭐ RUBY HOE: Handle 3x3 area replant WITHOUT durability loss
     * Ruby tools are UNBREAKABLE so NO damage is applied
     */
    private static boolean handleRubyAreaReplant(ServerWorld world, PlayerEntity player, BlockPos centerPos, ItemStack hoe) {
        int blocksProcessed = 0;

        // Process 3x3 area (center ± 1 block di X dan Z)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos targetPos = centerPos.add(dx, 0, dz);
                BlockState targetState = world.getBlockState(targetPos);
                Block targetBlock = targetState.getBlock();

                // Process crop without durability damage
                if (processCropBlock(world, player, targetPos, targetState, targetBlock, hoe)) {
                    blocksProcessed++;
                }
            }
        }

        if (blocksProcessed > 0) {
            // ⭐ RUBY HOE: NO DURABILITY DAMAGE (Unbreakable)
            // Ruby hoe durability is managed by RubyToolItem.inventoryTick()

            EmeraldMod.LOGGER.debug("Ruby Hoe 3x3 replant: {} crops processed at {} (NO durability loss)",
                    blocksProcessed, centerPos);

            return true;
        }

        return false;
    }

    /**
     * ⭐ EMERALD HOE: Handle single block replant WITH durability loss
     * Emerald tools have normal durability behavior
     */
    private static boolean handleEmeraldSingleReplant(ServerWorld world, PlayerEntity player,
                                                      BlockPos pos, BlockState state, Block block,
                                                      ItemStack hoe) {
        // Process single crop
        boolean processed = processCropBlock(world, player, pos, state, block, hoe);

        if (processed) {
            // ⭐ EMERALD HOE: APPLY DURABILITY DAMAGE (Normal behavior)
            hoe.damage(1, player, player.getPreferredEquipmentSlot(hoe));

            EmeraldMod.LOGGER.debug("Emerald Hoe single replant at {} (durability -1)", pos);
        }

        return processed;
    }

    /**
     * Process single crop block (shared logic for both hoes)
     * Returns true if crop was successfully processed
     */
    private static boolean processCropBlock(ServerWorld world, PlayerEntity player,
                                            BlockPos pos, BlockState state, Block block,
                                            ItemStack hoe) {
        if (block instanceof CropBlock cropBlock) {
            return handleStandardCrop(world, player, pos, state, cropBlock, hoe);
        }

        if (block instanceof NetherWartBlock) {
            return handleNetherWart(world, player, pos, state, hoe);
        }

        if (block instanceof StemBlock stemBlock) {
            return handleStemBlock(world, player, pos, state, stemBlock, hoe);
        }

        if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            return handleGourdBlock(world, player, pos, state, block, hoe);
        }

        if (block instanceof SweetBerryBushBlock) {
            return handleSweetBerryBush(world, player, pos, state, hoe);
        }

        if (block instanceof CocoaBlock) {
            return handleCocoa(world, player, pos, state, hoe);
        }

        return false;
    }

    private static boolean handleStandardCrop(ServerWorld world, PlayerEntity player, BlockPos pos,
                                              BlockState state, CropBlock crop, ItemStack hoe) {
        // ✅ CHECK: Hanya process jika MATURE
        if (!crop.isMature(state)) return false;

        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE), hoe);

        // Get drops
        java.util.List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Apply fortune bonus
        if (fortuneLevel > 0) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        // Drop items
        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Replant dengan age 0
        BlockState newState = crop.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        // Play sound
        world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Add experience
        player.addExperience(fortuneLevel > 0 ? 2 : 1);

        return true;
    }

    private static boolean handleNetherWart(ServerWorld world, PlayerEntity player, BlockPos pos,
                                            BlockState state, ItemStack hoe) {
        int age = state.get(NetherWartBlock.AGE);
        // ✅ CHECK: Hanya process jika MATURE (age 3)
        if (age < 3) return false;

        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE), hoe);

        java.util.List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        if (fortuneLevel > 0) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Replant dengan age 0
        BlockState newState = Blocks.NETHER_WART.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        world.playSound(null, pos, SoundEvents.BLOCK_NETHER_WART_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);

        return true;
    }

    private static boolean handleStemBlock(ServerWorld world, PlayerEntity player, BlockPos pos,
                                           BlockState state, StemBlock stem, ItemStack hoe) {
        int age = state.get(StemBlock.AGE);
        // ✅ CHECK: Hanya process jika MATURE (age 7)
        if (age < 7) return false;

        boolean hasGourd = false;
        Block expectedGourd = null;

        if (stem == Blocks.MELON_STEM) {
            expectedGourd = Blocks.MELON;
        } else if (stem == Blocks.PUMPKIN_STEM) {
            expectedGourd = Blocks.PUMPKIN;
        }

        if (expectedGourd != null) {
            for (var direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                BlockPos adjacentPos = pos.offset(direction);
                Block adjacentBlock = world.getBlockState(adjacentPos).getBlock();
                if (adjacentBlock == expectedGourd) {
                    hasGourd = true;
                    break;
                }
            }
        }

        // Jangan reset jika ada gourd (biarkan tetap mature)
        if (hasGourd) return false;

        // Reset ke age 0
        BlockState newState = stem.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        world.playSound(null, pos, SoundEvents.BLOCK_STEM_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);

        return true;
    }

    private static boolean handleGourdBlock(ServerWorld world, PlayerEntity player, BlockPos pos,
                                            BlockState state, Block block, ItemStack hoe) {
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE), hoe);

        java.util.List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        if (fortuneLevel > 0 && block == Blocks.MELON) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        world.removeBlock(pos, false);

        if (block == Blocks.MELON) {
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 0.8f);
        }

        player.addExperience(1);

        return true;
    }

    private static boolean handleSweetBerryBush(ServerWorld world, PlayerEntity player, BlockPos pos,
                                                BlockState state, ItemStack hoe) {
        int age = state.get(SweetBerryBushBlock.AGE);
        // ✅ CHECK: Hanya process jika MATURE (age 2+)
        if (age < 2) return false;

        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE), hoe);

        java.util.List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        if (fortuneLevel > 0) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Reset ke age 1 (bukan 0, karena berry bush tidak mulai dari 0)
        BlockState newState = state.with(SweetBerryBushBlock.AGE, 1);
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundCategory.BLOCKS, 1.0f, 1.0f);

        player.addExperience(1);

        return true;
    }

    private static boolean handleCocoa(ServerWorld world, PlayerEntity player, BlockPos pos,
                                       BlockState state, ItemStack hoe) {
        int age = state.get(CocoaBlock.AGE);
        // ✅ CHECK: Hanya process jika MATURE (age 2)
        if (age < 2) return false;

        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE), hoe);

        java.util.List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        if (fortuneLevel > 0) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Reset ke age 0, preserve facing
        BlockState newState = Blocks.COCOA.getDefaultState()
                .with(CocoaBlock.AGE, 0)
                .with(CocoaBlock.FACING, state.get(CocoaBlock.FACING));

        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 1.5f);

        player.addExperience(1);

        return true;
    }

    private static void applyFortuneBonus(java.util.List<ItemStack> drops, int fortuneLevel) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;

            int currentCount = drop.getCount();
            int bonusItems = 0;

            for (int i = 0; i < currentCount; i++) {
                float chance = 0.25f * fortuneLevel;

                if (RANDOM.nextFloat() < chance) {
                    bonusItems += RANDOM.nextInt(fortuneLevel) + 1;
                }
            }

            if (bonusItems > 0) {
                drop.setCount(currentCount + bonusItems);
            }
        }
    }
}