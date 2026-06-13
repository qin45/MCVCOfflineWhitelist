package com.mcvcofflinewhitelist.auth.mixin;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels chat messages from unauthenticated players.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class ChatAuthMixin {

    @Shadow
    private ServerPlayerEntity player;

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    public void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            player.sendMessage(Text.literal("§c请先完成验证！"));
            ci.cancel();
        }
    }
}
