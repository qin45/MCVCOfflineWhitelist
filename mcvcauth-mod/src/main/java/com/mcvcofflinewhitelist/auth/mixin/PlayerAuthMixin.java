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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts command execution to block unauthorized players
 * from using any commands except /login and /register.
 */
@Mixin(CommandManager.class)
public class PlayerAuthMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void onExecute(ParseResults<ServerCommandSource> parseResults, String command,
                          CallbackInfo ci) {
        ServerCommandSource source = parseResults.getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            return;
        }

        var auth = MCVCAuthMod.getInstance().getAuthManager();
        if (!auth.needsAuth(player.getUuid())) {
            return;
        }

        // Allow /login, /l, /register, /reg
        // Note: the 'command' parameter does NOT include the leading '/'
        String trimmed = command.trim().toLowerCase();
        if (trimmed.startsWith("login") || trimmed.startsWith("l ") || trimmed.equals("l")
                || trimmed.startsWith("register") || trimmed.startsWith("reg ")
                || trimmed.equals("reg")) {
            return;
        }

        // Block everything else
        source.sendError(Text.literal("§c请先完成验证！使用 /login <密码> 或 /register <密码> <确认密码>"));
        ci.cancel();
    }
}
