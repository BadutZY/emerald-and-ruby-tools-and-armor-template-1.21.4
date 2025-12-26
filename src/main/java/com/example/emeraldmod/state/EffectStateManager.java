package com.example.emeraldmod.state;

import com.example.emeraldmod.EmeraldMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager untuk menyimpan state effect (on/off) per player
 * Data disimpan per-world untuk persistence
 */
public class EffectStateManager extends PersistentState {

    private static final String NBT_KEY = "EmeraldModEffectStates";

    // Map: PlayerUUID -> EffectStates
    private final Map<UUID, PlayerEffectState> playerStates = new HashMap<>();

    public EffectStateManager() {
        super();
    }

    /**
     * Get effect state manager dari server
     */
    public static EffectStateManager getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD)
                .getPersistentStateManager();

        EffectStateManager state = persistentStateManager.getOrCreate(
                new PersistentState.Type<>(
                        EffectStateManager::new,
                        EffectStateManager::createFromNbt,
                        null
                ),
                NBT_KEY
        );

        return state;
    }

    /**
     * Create dari NBT
     */
    public static EffectStateManager createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        EffectStateManager manager = new EffectStateManager();

        NbtCompound statesNbt = nbt.getCompound("PlayerStates");
        for (String uuidString : statesNbt.getKeys()) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                NbtCompound playerNbt = statesNbt.getCompound(uuidString);

                boolean toolsEnabled = playerNbt.getBoolean("ToolsEnabled");
                boolean armorEnabled = playerNbt.getBoolean("ArmorEnabled");

                manager.playerStates.put(uuid, new PlayerEffectState(toolsEnabled, armorEnabled));
            } catch (IllegalArgumentException e) {
                EmeraldMod.LOGGER.warn("Invalid UUID in effect state: {}", uuidString);
            }
        }

        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound statesNbt = new NbtCompound();

        for (Map.Entry<UUID, PlayerEffectState> entry : playerStates.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putBoolean("ToolsEnabled", entry.getValue().toolsEnabled());
            playerNbt.putBoolean("ArmorEnabled", entry.getValue().armorEnabled());

            statesNbt.put(entry.getKey().toString(), playerNbt);
        }

        nbt.put("PlayerStates", statesNbt);
        return nbt;
    }

    /**
     * Get player effect state (default: enabled)
     */
    public PlayerEffectState getPlayerState(UUID playerUuid) {
        return playerStates.getOrDefault(playerUuid, new PlayerEffectState(true, true));
    }

    /**
     * Set tools effect enabled/disabled
     */
    public void setToolsEnabled(UUID playerUuid, boolean enabled) {
        PlayerEffectState current = getPlayerState(playerUuid);
        playerStates.put(playerUuid, new PlayerEffectState(enabled, current.armorEnabled()));
        markDirty();

        EmeraldMod.LOGGER.info("Player {} tools effect: {}", playerUuid, enabled ? "ON" : "OFF");
    }

    /**
     * Set armor effect enabled/disabled
     */
    public void setArmorEnabled(UUID playerUuid, boolean enabled) {
        PlayerEffectState current = getPlayerState(playerUuid);
        playerStates.put(playerUuid, new PlayerEffectState(current.toolsEnabled(), enabled));
        markDirty();

        EmeraldMod.LOGGER.info("Player {} armor effect: {}", playerUuid, enabled ? "ON" : "OFF");
    }

    /**
     * Toggle tools effect
     */
    public boolean toggleToolsEffect(UUID playerUuid) {
        PlayerEffectState current = getPlayerState(playerUuid);
        boolean newState = !current.toolsEnabled();
        setToolsEnabled(playerUuid, newState);
        return newState;
    }

    /**
     * Toggle armor effect
     */
    public boolean toggleArmorEffect(UUID playerUuid) {
        PlayerEffectState current = getPlayerState(playerUuid);
        boolean newState = !current.armorEnabled();
        setArmorEnabled(playerUuid, newState);
        return newState;
    }

    /**
     * Check if tools effect is enabled
     */
    public boolean isToolsEnabled(UUID playerUuid) {
        return getPlayerState(playerUuid).toolsEnabled();
    }

    /**
     * Check if armor effect is enabled
     */
    public boolean isArmorEnabled(UUID playerUuid) {
        return getPlayerState(playerUuid).armorEnabled();
    }

    /**
     * Record untuk menyimpan state per player
     */
    public record PlayerEffectState(boolean toolsEnabled, boolean armorEnabled) {}
}