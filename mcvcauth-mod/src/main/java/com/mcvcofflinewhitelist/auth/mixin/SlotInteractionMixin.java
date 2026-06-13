package com.mcvcofflinewhitelist.auth.mixin;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents unauthenticated players from moving items in any inventory
 * (hotbar, main inventory, chests, crafting tables, etc.).
 *
 * <p>Works by injecting into {@link Slot#canTakeItems(PlayerEntity)}
 * and returning {@code false} for unauthenticated players, plus sending
 * a visual update packet so the client doesn't show the item as taken.</p>
 */
@Mixin(Slot.class)
public class SlotInteractionMixin {

    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    public void onCanTakeItems(PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!(playerEntity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (MCVCAuthMod.getInstance().getAuthManager().needsAuth(player.getUuid())) {
            // Send a visual update so the client puts the item back
            player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(
                            -2,
                            0,
                            player.getInventory().selectedSlot,
                            player.getInventory().getStack(player.getInventory().selectedSlot)));
            cir.setReturnValue(false);
        }
    }
}
