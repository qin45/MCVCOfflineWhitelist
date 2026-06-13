package com.mcvcofflinewhitelist.auth.mixin;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels {@link ServerPlayerEntity#playerTick()} for unauthenticated
 * players, preventing ALL per-tick processing — movement, item use,
 * potion effects, health regen, etc.
 *
 * <p>Also keeps the player invisible and invulnerable during auth so
 * other players cannot see or interact with them.</p>
 */
@Mixin(ServerPlayerEntity.class)
public class PlayerTickMixin {

    @Unique
    private final ServerPlayerEntity mcvcauth_player = (ServerPlayerEntity) (Object) this;

    @Inject(method = "playerTick", at = @At("HEAD"), cancellable = true)
    public void onPlayerTick(CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(mcvcauth_player.getUuid())) {
            // Hide from other players and prevent damage
            if (!mcvcauth_player.isInvisible()) {
                mcvcauth_player.setInvisible(true);
            }
            if (!mcvcauth_player.isInvulnerable()) {
                mcvcauth_player.setInvulnerable(true);
            }
            ci.cancel();
        }
    }
}
