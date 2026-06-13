package com.mcvcofflinewhitelist.auth.mixin;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents unauthenticated players from moving or turning.
 *
 * <p><b>Why {@code tick()} instead of just {@code onPlayerMove}:</b><br>
 * In Minecraft 1.21 the movement-packet handling may dispatch through
 * multiple specific handler methods. Cancelling {@code onPlayerMove}
 * alone could miss some of them. By injecting at {@code TAIL} of
 * {@code tick()}, we reset the player's position and rotation after
 * EVERY tick — regardless of which packets were processed.</p>
 *
 * <p>Additional injections cancel the most common packet types early
 * so the server never processes invalid position data at all.</p>
 */
@Mixin(ServerPlayNetworkHandler.class)
public class PlayerMoveMixin {

    @Shadow
    private ServerPlayerEntity player;

    // ── Freeze position storage ───────────────────────────────────────

    @Unique
    private double mcvcauth_freezeX;

    @Unique
    private double mcvcauth_freezeY;

    @Unique
    private double mcvcauth_freezeZ;

    @Unique
    private float mcvcauth_freezeYaw;

    @Unique
    private float mcvcauth_freezePitch;

    @Unique
    private boolean mcvcauth_freezeInitialized = false;

    // ── Cancel incoming movement packets (early rejection) ────────────

    /**
     * Catches all position/rotation change packets and discards them
     * before the server processes them.
     */
    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    public void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            ci.cancel();
        }
    }

    // ── Periodic position & rotation reset (guaranteed freeze) ────────

    /**
     * After every tick, restore the player to their freeze position.
     * This catches any movement that slipped through packet handlers
     * (vehicle movement, teleport confirms, future packet changes, etc.).
     */
    @Inject(method = "tick", at = @At("TAIL"))
    public void onTickTail(CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            // Capture initial position on first tick after needsAuth
            if (!mcvcauth_freezeInitialized) {
                mcvcauth_freezeX = player.getX();
                mcvcauth_freezeY = player.getY();
                mcvcauth_freezeZ = player.getZ();
                mcvcauth_freezeYaw = player.getYaw();
                mcvcauth_freezePitch = player.getPitch();
                mcvcauth_freezeInitialized = true;
                MCVCAuthMod.LOGGER.info("Freezing '{}' at ({}, {}, {})",
                        player.getName().getString(),
                        Math.round(mcvcauth_freezeX),
                        Math.round(mcvcauth_freezeY),
                        Math.round(mcvcauth_freezeZ));
            }

            // Reset both server-side position and client view
            player.updatePositionAndAngles(
                    mcvcauth_freezeX, mcvcauth_freezeY, mcvcauth_freezeZ,
                    mcvcauth_freezeYaw, mcvcauth_freezePitch);
            player.networkHandler.syncWithPlayerPosition();
        } else if (mcvcauth_freezeInitialized) {
            // Player authenticated — clear stored position
            mcvcauth_freezeInitialized = false;
        }
    }
}
