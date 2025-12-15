package com.example.emeraldmod.client;

import com.example.emeraldmod.item.EmeraldArmorItem;
import com.example.emeraldmod.item.ModItems;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TooltipHandler {

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            Item item = stack.getItem();

            // Cek apakah Shift ditekan
            boolean shiftPressed = isShiftPressed();

            // HANYA tambahkan tooltip jika item adalah salah satu dari mod items
            if (item == ModItems.EMERALD_SWORD) {
                // Tambahkan blank line HANYA untuk mod items
                lines.add(Text.literal(""));
                addSwordTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_PICKAXE) {
                lines.add(Text.literal(""));
                addPickaxeTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_AXE) {
                lines.add(Text.literal(""));
                addAxeTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_SHOVEL) {
                lines.add(Text.literal(""));
                addShovelTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_HOE) {
                lines.add(Text.literal(""));
                addHoeTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_HELMET) {
                lines.add(Text.literal(""));
                addHelmetTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_CHESTPLATE) {
                lines.add(Text.literal(""));
                addChestplateTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_LEGGINGS) {
                lines.add(Text.literal(""));
                addLeggingsTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_BOOTS) {
                lines.add(Text.literal(""));
                addBootsTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_HORSE_ARMOR) {
                lines.add(Text.literal(""));
                addHorseArmorTooltip(lines, shiftPressed);
            } else if (item == ModItems.EMERALD_UPGRADE_SMITHING_TEMPLATE) {
                lines.add(Text.literal(""));
                addUpgradeTooltip(lines);
            }
        });
    }

    private static void addSwordTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Shockwave").formatted(Formatting.AQUA));
            addDescriptionSword(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionSword(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("Every 3rd hit creates a powerful shockwave").formatted(Formatting.GRAY));
        lines.add(Text.literal("Launches enemies into the air").formatted(Formatting.GRAY));
    }

    private static void addPickaxeTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Auto Smelt").formatted(Formatting.DARK_RED));
            addDescriptionPickaxe(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionPickaxe(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("Smelts automatically when mining ores").formatted(Formatting.GRAY));
    }

    private static void addAxeTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Tree Chopper").formatted(Formatting.RED));
            addDescriptionAxe(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionAxe(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("Chops entire trees at once").formatted(Formatting.GRAY));
    }

    private static void addShovelTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Anti-Gravity").formatted(Formatting.DARK_AQUA));
            addDescriptionShovel(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionShovel(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("prevent blocks from falling if the surrounding").formatted(Formatting.GRAY));
        lines.add(Text.literal("area is destroyed with a shovel").formatted(Formatting.GRAY));
    }

    private static void addHoeTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Auto-Replant").formatted(Formatting.DARK_GREEN));
            addDescriptionHoe(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionHoe(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("Right-click mature crops to harvest").formatted(Formatting.GRAY));
    }

    private static void addHelmetTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Water Breathing").formatted(Formatting.AQUA));
            lines.add(Text.literal("• Fire Resistance").formatted(Formatting.DARK_RED));
            addDescriptionHelmet(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionHelmet(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("- Infinite Water Breathing").formatted(Formatting.GRAY));
        lines.add(Text.literal("- Infinite Fire Resistance").formatted(Formatting.GRAY));
    }

    private static void addChestplateTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Dolphin's Grace").formatted(Formatting.AQUA));
            lines.add(Text.literal("• Fire Resistance").formatted(Formatting.DARK_RED));
            addDescriptionChestplate(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionChestplate(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("- Speed up swimming").formatted(Formatting.GRAY));
        lines.add(Text.literal("- Infinite Fire Resistance").formatted(Formatting.GRAY));
    }

    private static void addLeggingsTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Fire Resistance").formatted(Formatting.DARK_RED));
            addDescriptionLeggings(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionLeggings(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("- Infinite Fire Resistance").formatted(Formatting.GRAY));
    }

    private static void addBootsTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Snow Walk").formatted(Formatting.AQUA));
            lines.add(Text.literal("• Fire Resistance").formatted(Formatting.DARK_RED));
            addDescriptionBoots(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionBoots(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("- Walk on powder snow without sinking").formatted(Formatting.GRAY));
        lines.add(Text.literal("- Infinite Fire Resistance").formatted(Formatting.GRAY));
    }

    private static void addHorseArmorTooltip(List<Text> lines, boolean shift) {
        lines.add(Text.literal("Special Effect: ").formatted(Formatting.GOLD));

        if (shift) {
            lines.add(Text.literal("• Speed").formatted(Formatting.AQUA));
            lines.add(Text.literal("• Regeneration").formatted(Formatting.RED));
            lines.add(Text.literal("• Fire resistant").formatted(Formatting.DARK_RED));
            lines.add(Text.literal("• Jump Boost").formatted(Formatting.BLUE));
            lines.add(Text.literal("• Resistant").formatted(Formatting.WHITE));
            addDescriptionHorseArmor(lines);
        } else {
            addShiftHint(lines);
        }
    }

    private static void addDescriptionHorseArmor(List<Text> lines) {
        lines.add(Text.literal(" ").formatted(Formatting.WHITE));
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("- Increase speed").formatted(Formatting.GRAY));
        lines.add(Text.literal("- Healing").formatted(Formatting.GRAY));
        lines.add(Text.literal("- Infinite Fire Resistance").formatted(Formatting.GRAY));
        lines.add(Text.literal("- Increase Jump").formatted(Formatting.GRAY));
        lines.add(Text.literal("- Reduces incoming damage").formatted(Formatting.GRAY));
    }

    private static void addUpgradeTooltip(List<Text> lines) {
        lines.add(Text.literal("Description:").formatted(Formatting.BLUE));
        lines.add(Text.literal("Upgrade netherite gear to").formatted(Formatting.GRAY));
        lines.add(Text.literal("emerald tier").formatted(Formatting.GRAY));
        lines.add(Text.literal("  ").formatted(Formatting.GRAY));
        lines.add(Text.literal("Applies to:").formatted(Formatting.GRAY));
        lines.add(Text.literal(" Netherite Equipment").formatted(Formatting.BLUE));
        lines.add(Text.literal("Ingredients:").formatted(Formatting.GRAY));
        lines.add(Text.literal(" Emerald").formatted(Formatting.BLUE));
    }

    private static void addShiftHint(List<Text> lines) {
        lines.add(Text.literal("")
                .append(Text.literal("[").formatted(Formatting.DARK_GRAY))
                .append(Text.literal("Shift").formatted(Formatting.YELLOW))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal("for details").formatted(Formatting.GRAY))
        );
    }

    private static boolean isShiftPressed() {
        // Cek apakah Left Shift atau Right Shift ditekan
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}