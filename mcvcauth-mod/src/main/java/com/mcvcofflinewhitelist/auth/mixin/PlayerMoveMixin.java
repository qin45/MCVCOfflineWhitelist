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
 * <p><b>Strategy (three layers):</b><br>
 * <ol>
 *   <li><b>Cancels {@code onPlayerMove}</b> — server never processes
 *       incoming position/rotation changes.</li>
 *   <li><b>Rate-limited {@code requestTeleport}</b> — periodically
 *       rubber-bands the client back to the freeze point so the local
 *       viewport snaps back. Throttled to avoid "moved too fast" kicks.</li>
 *   <li><b>{@code playerTick()} cancellation</b> (separate mixin) —
 *       prevents ALL tick processing on the player entity.</li>
 * </ol>
 * </p>
 */
@Mixin(ServerPlayNetworkHandler.class)
public class PlayerMoveMixin {

    @Shadow
    private ServerPlayerEntity player;

    // ── Freeze position ───────────────────────────────────────────────

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

    /** Nano time of the last teleport — rate-limit to avoid spam kicks. */
    @Unique
    private long mcvcauth_lastTeleport = 0L;

    /** Minimum interval between teleport packets (nano = ms × 1_000_000). */
    @Unique
    private static final long TELEPORT_INTERVAL_NANO = 100_000_000L; // 100 ms

    // ── Cancel movement packets ───────────────────────────────────────

    /**
     * Discards position/rotation packets before the server processes them.
     */
    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    public void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            ci.cancel();
        }
    }

    // ── Periodic teleport (client rubber-band) ────────────────────────

    /**
     * After every tick, teleport the client back to the freeze position.
     * Rate-limited so we don't trigger the anti-cheat "moved too fast" check.
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

            // Rate-limited teleport back to freeze point
            long now = System.nanoTime();
            if (now - mcvcauth_lastTeleport >= TELEPORT_INTERVAL_NANO) {
                player.networkHandler.requestTeleport(
                        mcvcauth_freezeX, mcvcauth_freezeY, mcvcauth_freezeZ,
                        mcvcauth_freezeYaw, mcvcauth_freezePitch);
                mcvcauth_lastTeleport = now;
            }
        } else if (mcvcauth_freezeInitialized) {
            // Player authenticated — cleanup
            mcvcauth_freezeInitialized = false;
        }
    }
}
