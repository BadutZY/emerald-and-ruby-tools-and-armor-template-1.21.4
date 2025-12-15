package com.example.emeraldmod.event;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoReplantHandler {

    // Map crop block ke seed item yang diperlukan untuk replant
    private static final Map<Block, ItemStack> CROP_TO_SEED = new HashMap<>();

    static {
        // Basic Crops
        CROP_TO_SEED.put(Blocks.WHEAT, new ItemStack(Items.WHEAT_SEEDS));
        CROP_TO_SEED.put(Blocks.CARROTS, new ItemStack(Items.CARROT));
        CROP_TO_SEED.put(Blocks.POTATOES, new ItemStack(Items.POTATO));
        CROP_TO_SEED.put(Blocks.BEETROOTS, new ItemStack(Items.BEETROOT_SEEDS));

        // Nether Wart
        CROP_TO_SEED.put(Blocks.NETHER_WART, new ItemStack(Items.NETHER_WART));

        // Melon & Pumpkin Stems (tidak perlu replant, hanya harvest)
        // Akan dihandle secara khusus

        // Sweet Berry Bush (tidak perlu replant, hanya harvest)
        // Akan dihandle secara khusus
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Cek apakah player menggunakan Emerald Hoe
            ItemStack heldItem = player.getStackInHand(hand);
            if (heldItem.getItem() != ModItems.EMERALD_HOE) {
                return ActionResult.PASS;
            }

            // Cek apakah di server side
            if (world.isClient) {
                return ActionResult.SUCCESS; // Client side sukses, server akan handle
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

        EmeraldMod.LOGGER.info("✓ Registered Auto-Replant Handler for Emerald Hoe");
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

        // Handle AttachedStemBlock (Connected Melon/Pumpkin Stems)
        if (block instanceof AttachedStemBlock) {
            return handleAttachedStem(serverWorld, player, pos, state, hoe);
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
            return false; // Crop belum dewasa, tidak diproses
        }

        // Dapatkan loot drops
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Drop semua items
        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Replant crop (reset ke age 0)
        BlockState newState = crop.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        // Play sound
        world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Damage hoe
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        // Add experience (seperti harvest normal)
        player.addExperience(1);

        EmeraldMod.LOGGER.debug("Auto-replanted crop at {}", pos);
        return true;
    }

    private static boolean handleNetherWart(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack hoe) {
        // Cek apakah nether wart sudah dewasa (age 3)
        int age = state.get(NetherWartBlock.AGE);
        if (age < 3) {
            return false; // Belum dewasa
        }

        // Dapatkan loot drops
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Drop semua items
        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Replant nether wart (reset ke age 0)
        BlockState newState = Blocks.NETHER_WART.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        // Play sound
        world.playSound(null, pos, SoundEvents.BLOCK_NETHER_WART_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Damage hoe
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        EmeraldMod.LOGGER.debug("Auto-replanted nether wart at {}", pos);
        return true;
    }

    private static boolean handleStemBlock(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, StemBlock stem, ItemStack hoe) {
        // Stem block hanya harvest jika sudah dewasa dan belum attached
        int age = state.get(StemBlock.AGE);
        if (age < 7) {
            return false; // Belum dewasa
        }

        // Cek apakah ada gourd (melon/pumpkin) di sekitar
        boolean hasGourd = false;
        Block expectedGourd = null;

        // Tentukan gourd yang diharapkan berdasarkan jenis stem
        if (stem == Blocks.MELON_STEM) {
            expectedGourd = Blocks.MELON;
        } else if (stem == Blocks.PUMPKIN_STEM) {
            expectedGourd = Blocks.PUMPKIN;
        }

        // Cek di 4 arah horizontal
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

        // Jika ada gourd, tidak perlu harvest stem (biarkan attached)
        if (hasGourd) {
            return false;
        }

        // Stem sudah dewasa tapi tidak ada gourd
        // Reset ke age 0 untuk grow lagi
        BlockState newState = stem.getDefaultState();
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        // Play sound
        world.playSound(null, pos, SoundEvents.BLOCK_STEM_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Damage hoe (minimal karena hanya reset stem)
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        EmeraldMod.LOGGER.debug("Reset stem at {}", pos);
        return true;
    }

    private static boolean handleAttachedStem(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack hoe) {
        // Attached stem tidak perlu diproses karena gourd-nya yang di-harvest
        // Return false agar tidak ada action
        return false;
    }

    private static boolean handleGourdBlock(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, Block block, ItemStack hoe) {
        // Harvest melon atau pumpkin
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Drop semua items
        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Hapus gourd block
        world.removeBlock(pos, false);

        // Play sound
        if (block == Blocks.MELON) {
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 0.8f);
        }

        // Damage hoe
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        // Add experience
        player.addExperience(1);

        EmeraldMod.LOGGER.debug("Harvested gourd at {}", pos);
        return true;
    }

    private static boolean handleSweetBerryBush(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack hoe) {
        // Sweet berry bush memiliki 4 age (0-3)
        // Hanya harvest jika age 2 atau 3 (ada berries)
        int age = state.get(SweetBerryBushBlock.AGE);
        if (age < 2) {
            return false; // Belum ada berries
        }

        // Dapatkan loot drops
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Drop semua items
        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Reset ke age 1 (bush tetap ada, tapi berries hilang)
        BlockState newState = state.with(SweetBerryBushBlock.AGE, 1);
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        // Play sound
        world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0f, 1.0f);

        // Damage hoe
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        // Add experience
        player.addExperience(1);

        EmeraldMod.LOGGER.debug("Harvested sweet berries at {}", pos);
        return true;
    }

    private static boolean handleCocoa(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state, ItemStack hoe) {
        // Cocoa memiliki 3 age (0-2)
        int age = state.get(CocoaBlock.AGE);
        if (age < 2) {
            return false; // Belum dewasa
        }

        // Dapatkan loot drops
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, hoe);

        // Drop semua items
        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        // Replant cocoa (reset ke age 0, tetap di posisi yang sama)
        BlockState newState = Blocks.COCOA.getDefaultState()
                .with(CocoaBlock.AGE, 0)
                .with(CocoaBlock.FACING, state.get(CocoaBlock.FACING)); // Pertahankan facing

        world.setBlockState(pos, newState, Block.NOTIFY_ALL);

        // Play sound
        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 1.5f);

        // Damage hoe
        hoe.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);

        // Add experience
        player.addExperience(1);

        EmeraldMod.LOGGER.debug("Auto-replanted cocoa at {}", pos);
        return true;
    }
}