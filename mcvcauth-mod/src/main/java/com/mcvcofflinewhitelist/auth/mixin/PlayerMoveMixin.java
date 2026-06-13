package com.mcvcofflinewhitelist.auth.mixin;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels movement packets from unauthenticated players, freezing
 * them in place until they authenticate.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class PlayerMoveMixin {

    @Shadow
    private ServerPlayerEntity player;

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    public void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            // Cancel movement — player stays in place on server side.
            // Send a position sync so the client rubber-bands back.
            player.networkHandler.syncWithPlayerPosition();
            ci.cancel();
        }
    }
}
