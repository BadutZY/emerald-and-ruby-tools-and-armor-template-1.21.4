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
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class AutoReplantHandler {

    private static final Random RANDOM = new Random();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Cek apakah player menggunakan Emerald Hoe
            ItemStack heldItem = player.getStackInHand(hand);
            if (heldItem.getItem() != ModItems.EMERALD_HOE) {
                return ActionResult.PASS;
            }

            // ✅ CHECK: Apakah tools effect enabled? (Server-side only)
            if (!world.isClient) {
                ServerWorld serverWorld = (ServerWorld) world;
                EffectStateManager stateManager = EffectStateManager.getServerState(serverWorld.getServer());

                if (!stateManager.isToolsEnabled(player.getUuid())) {
                    // Tools effect DISABLED, biarkan vanilla behavior
                    EmeraldMod.LOGGER.debug("Auto-replant disabled for player {}", player.getName().getString());
                    return ActionResult.PASS;
                }
            }

            // Cek apakah di server side
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            // Handle berbagai jenis crops
            if (handleCropBlock(world, player, pos, state, block, heldItem)) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        EmeraldMod.LOGGER.info("✓ Registered Auto-Replant Handler with Fortune Support (Toggleable)");
    }

    private static boolean handleCropBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, Block block, ItemStack hoe) {
        ServerWorld serverWorld = (ServerWorld) world;

        // Handle CropBlock (Wheat, Carrots, Potatoes, Beetroots)
        if (block instanceof CropBlock cropBlock) {
            return handleStandardCrop(serverWorld, player, pos, state, cropBlock, hoe);
        }

        // Handle NetherWartBlock
        if (block instanceof NetherWartBlock) {
            return handleNetherWart(serverWorld, player, pos, state, hoe);
        }

        // Handle StemBlock (Melon & Pumpkin Stems)
        if (block instanceof StemBlock stemBlock) {
            return handleStemBlock(serverWorld, player, pos, state, stemBlock, hoe);
        }

        // Handle Melon & Pumpkin Fruits
        if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            return handleGourdBlock(serverWorld, player, pos, state, block, hoe);
        }

        // Handle Sweet Berry Bush
        if (block instanceof SweetBerryBushBlock) {
            return handleSweetBerryBush(serverWorld, player, pos, state, hoe);
        }

        // Handle Cocoa Block
        if (block instanceof CocoaBlock) {
            return handleCocoa(serverWorld, player, pos, state, hoe);
        }

        return false;
    }

    private static boolean handleStandardCrop(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, CropBlock crop, ItemStack hoe) {
        // Cek apakah crop sudah dewasa
        if (!crop.isMature(state)) {
            return false;
        }

        // Get Fortune level
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                hoe
        );

        // Get base drops
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Apply ADDITIONAL Fortune bonus
        if (fortuneLevel > 0) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        // Drop all items
        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Replant crop (reset to age 0)
        BlockState newState = crop.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        // Play sound
        world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Damage hoe (respects Unbreaking enchantment automatically)
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        // Add experience
        player.addExperience(fortuneLevel > 0 ? 2 : 1);

        EmeraldMod.LOGGER.debug("Auto-replanted crop at {} (Fortune: {}, Total items: {})",
                pos, fortuneLevel, drops.stream().mapToInt(ItemStack::getCount).sum());
        return true;
    }

    private static boolean handleNetherWart(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack hoe) {
        int age = state.get(NetherWartBlock.AGE);
        if (age < 3) {
            return false;
        }

        // Fortune works on Nether Wart
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                hoe
        );

        // Get drops with Fortune applied
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Apply ADDITIONAL Fortune bonus
        if (fortuneLevel > 0) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Replant
        BlockState newState = Blocks.NETHER_WART.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        world.playSound(null, pos, SoundEvents.BLOCK_NETHER_WART_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        EmeraldMod.LOGGER.debug("Auto-replanted nether wart at {} (Fortune: {}, Total items: {})",
                pos, fortuneLevel, drops.stream().mapToInt(ItemStack::getCount).sum());
        return true;
    }

    private static boolean handleStemBlock(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, StemBlock stem, ItemStack hoe) {
        int age = state.get(StemBlock.AGE);
        if (age < 7) {
            return false;
        }

        // Check for gourd
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

        if (hasGourd) {
            return false;
        }

        // Reset stem
        BlockState newState = stem.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        world.playSound(null, pos, SoundEvents.BLOCK_STEM_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        return true;
    }

    private static boolean handleGourdBlock(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, Block block, ItemStack hoe) {
        // Get Fortune level
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                hoe
        );

        // Get drops
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Apply ADDITIONAL Fortune bonus for melons (pumpkins don't benefit from fortune in vanilla)
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

        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        player.addExperience(1);

        return true;
    }

    private static boolean handleSweetBerryBush(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack hoe) {
        int age = state.get(SweetBerryBushBlock.AGE);
        if (age < 2) {
            return false;
        }

        // Fortune affects sweet berries
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                hoe
        );

        // Get drops
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Apply ADDITIONAL Fortune bonus
        if (fortuneLevel > 0) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Reset to age 1
        BlockState newState = state.with(SweetBerryBushBlock.AGE, 1);
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0f, 1.0f);
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        player.addExperience(1);

        EmeraldMod.LOGGER.debug("Harvested sweet berries at {} (Fortune: {}, Total items: {})",
                pos, fortuneLevel, drops.stream().mapToInt(ItemStack::getCount).sum());
        return true;
    }

    private static boolean handleCocoa(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack hoe) {
        int age = state.get(CocoaBlock.AGE);
        if (age < 2) {
            return false;
        }

        // Fortune affects cocoa beans
        int fortuneLevel = EnchantmentHelper.getLevel(
                world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                hoe
        );

        // Get drops
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Apply ADDITIONAL Fortune bonus
        if (fortuneLevel > 0) {
            applyFortuneBonus(drops, fortuneLevel);
        }

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Replant
        BlockState newState = Blocks.COCOA.getDefaultState()
                .with(CocoaBlock.AGE, 0)
                .with(CocoaBlock.FACING, state.get(CocoaBlock.FACING));

        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 1.5f);
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        player.addExperience(1);

        EmeraldMod.LOGGER.debug("Auto-replanted cocoa at {} (Fortune: {}, Total items: {})",
                pos, fortuneLevel, drops.stream().mapToInt(ItemStack::getCount).sum());
        return true;
    }

    /**
     * Apply ADDITIONAL Fortune bonus to drops
     * This adds extra items on top of vanilla loot table drops
     */
    private static void applyFortuneBonus(List<ItemStack> drops, int fortuneLevel) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;

            int currentCount = drop.getCount();
            int bonusItems = 0;

            // For each item in the stack, chance to get bonus
            for (int i = 0; i < currentCount; i++) {
                float chance = 0.25f * fortuneLevel; // 25%, 50%, 75%

                if (RANDOM.nextFloat() < chance) {
                    // Random bonus between 1 and fortuneLevel
                    bonusItems += RANDOM.nextInt(fortuneLevel) + 1;
                }
            }

            // Add bonus items
            if (bonusItems > 0) {
                drop.setCount(currentCount + bonusItems);
                EmeraldMod.LOGGER.debug("Fortune bonus: +{} items (Fortune {})", bonusItems, fortuneLevel);
            }
        }
    }
}