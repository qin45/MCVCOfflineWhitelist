package com.mcvcofflinewhitelist.auth.mixin;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Filters unauthenticated players from the {@link PlayerListS2CPacket},
 * hiding them from other players' tab lists.
 *
 * <p>Once authenticated, the player appears normally in the list.</p>
 */
@Mixin(PlayerListS2CPacket.class)
public class PlayerListS2CPacketMixin {

    @Mutable
    @Final
    @Shadow
    private List<PlayerListS2CPacket.Entry> entries;

    @Unique
    private static boolean isUnauthenticated(ServerPlayerEntity player) {
        return MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid());
    }

    /**
     * Multi-player constructor: filter the collection of all players.
     */
    @ModifyVariable(
            method = "<init>(Ljava/util/EnumSet;Ljava/util/Collection;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Collection<ServerPlayerEntity> filterPlayerList(
            Collection<ServerPlayerEntity> players) {
        ArrayList<ServerPlayerEntity> filtered = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            if (!isUnauthenticated(player)) {
                filtered.add(player);
            }
        }
        return filtered;
    }

    /**
     * Single-player constructor: clear entries if the player is unauthenticated.
     */
    @Redirect(
            method = "<init>(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket$Action;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket;entries:Ljava/util/List;",
                    opcode = Opcodes.PUTFIELD
            ))
    private void clearEntryIfUnauthenticated(
            PlayerListS2CPacket instance, List<PlayerListS2CPacket.Entry> entries,
            PlayerListS2CPacket.Action action, ServerPlayerEntity player) {
        if (isUnauthenticated(player)) {
            this.entries = List.of(); // empty — don't show in tab list
        } else {
            this.entries = entries;
        }
    }
}
