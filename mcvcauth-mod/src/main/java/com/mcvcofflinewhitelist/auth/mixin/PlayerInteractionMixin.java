package com.mcvcofflinewhitelist.auth.mixin;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks interactions (block breaking, entity interaction, item use, etc.)
 * for unauthenticated players.
 *
 * <p>Each injection uses {@code require = 0} so that if a Yarn method name
 * does not match, the mixin still applies — the player can still move
 * correctly and interaction blocking is a best-effort bonus.</p>
 */
@Mixin(ServerPlayNetworkHandler.class)
public class PlayerInteractionMixin {

    @Shadow
    private ServerPlayerEntity player;

    /** Block digging / item drop / swap actions. */
    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true, require = 0)
    public void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            player.sendMessage(Text.literal("§c请先完成验证！"));
            ci.cancel();
        }
    }

    /** Right-click block (place blocks, open chests/doors, etc.). */
    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true, require = 0)
    public void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            ci.cancel();
        }
    }

    /** Right-click entity (open villager trades, attack, etc.). */
    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true, require = 0)
    public void onPlayerInteractEntity(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            ci.cancel();
        }
    }

    /** Use item (eat, throw, etc.). */
    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true, require = 0)
    public void onPlayerInteractItem(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            ci.cancel();
        }
    }
}
