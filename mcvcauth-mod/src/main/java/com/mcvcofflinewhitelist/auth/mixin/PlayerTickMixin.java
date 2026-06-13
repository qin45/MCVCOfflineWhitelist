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
 * <p>This is the most reliable way to freeze a player server-side.
 * Combined with packet cancellation ({@code onPlayerMove}) and periodic
 * {@code requestTeleport}, it creates a complete lock.</p>
 */
@Mixin(ServerPlayerEntity.class)
public class PlayerTickMixin {

    @Unique
    private final ServerPlayerEntity mcvcauth_player = (ServerPlayerEntity) (Object) this;

    @Inject(method = "playerTick", at = @At("HEAD"), cancellable = true)
    public void onPlayerTick(CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(mcvcauth_player.getUuid())) {
            ci.cancel();
        }
    }
}
