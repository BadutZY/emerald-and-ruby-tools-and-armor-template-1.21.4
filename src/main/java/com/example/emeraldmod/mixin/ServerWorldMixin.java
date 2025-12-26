package com.example.emeraldmod.mixin;

import com.example.emeraldmod.EmeraldMod;
import com.example.emeraldmod.effect.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin untuk ServerWorld - mencegah game events dari player dengan Silent Step
 * untuk detection systems dan sounds, tetapi allow pressure plates
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    /**
     * Inject ke method emitGameEvent untuk cancel events dari silent players
     * Signature: emitGameEvent(RegistryEntry<GameEvent>, Vec3d, GameEvent.Emitter)
     */
    @Inject(
            method = "emitGameEvent(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/event/GameEvent$Emitter;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void emeraldmod$cancelEventsForSilentPlayers(
            RegistryEntry<GameEvent> event,
            Vec3d pos,
            GameEvent.Emitter emitter,
            CallbackInfo ci) {

        Entity sourceEntity = emitter.sourceEntity();

        if (sourceEntity instanceof PlayerEntity player) {
            if (player.hasStatusEffect(ModEffects.SILENT_STEP_ENTRY)) {
                try {
                    var eventKey = event.getKey();
                    if (eventKey.isPresent()) {
                        String eventId = eventKey.get().getValue().toString();

                        // Cancel specific events untuk stealth
                        // TIDAK termasuk entity_place (untuk pressure plates)
                        if (eventId.equals("minecraft:step") ||           // Footsteps
                                eventId.equals("minecraft:hit_ground") ||     // Landing
                                eventId.equals("minecraft:swim") ||           // Swimming
                                eventId.equals("minecraft:splash") ||         // Water splash
                                eventId.equals("minecraft:entity_interact")) { // Tripwires

                            ci.cancel();

                            EmeraldMod.LOGGER.debug("Cancelled {} event from silent player: {}",
                                    eventId, player.getName().getString());
                        }
                    }
                } catch (Exception e) {
                    EmeraldMod.LOGGER.error("Error in Silent Step mixin", e);
                }
            }
        }
    }
}