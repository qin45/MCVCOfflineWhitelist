package com.mcvcofflinewhitelist.auth.mixin;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import com.mojang.brigadier.ParseResults;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts command execution to block unauthorized players
 * from using any commands except /login and /register.
 */
@Mixin(CommandManager.class)
public class PlayerAuthMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void onExecute(ParseResults<ServerCommandSource> parseResults, String command,
                          CallbackInfoReturnable<Integer> cir) {
        ServerCommandSource source = parseResults.getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            return; // Not a player, let it through
        }

        var auth = MCVCAuthMod.getInstance().getAuthManager();
        if (!auth.needsAuth(player.getUuid())) {
            return; // Doesn't need auth, let it through
        }

        // Allow /login, /l, /register, /reg
        String trimmed = command.trim().toLowerCase();
        if (trimmed.startsWith("/login") || trimmed.startsWith("/l ") || trimmed.equals("/l")
                || trimmed.startsWith("/register") || trimmed.startsWith("/reg ")
                || trimmed.equals("/reg")) {
            return;
        }

        // Block everything else
        source.sendError(Text.literal("§cYou must authenticate first! Use /login or /register"));
        cir.setReturnValue(0);
    }
}
