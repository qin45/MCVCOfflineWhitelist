package com.mcvcofflinewhitelist.auth.command;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /login <password> — authenticate after registration.
 */
public class LoginCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, MCVCAuthMod mod) {
        dispatcher.register(CommandManager.literal("login")
                .then(CommandManager.argument("password", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            if (player == null) return 0;

                            String password = StringArgumentType.getString(ctx, "password");
                            var auth = mod.getAuthManager();
                            String username = player.getName().getString();

                            if (!auth.needsAuth(player.getUuid())) {
                                player.sendMessage(Text.literal("§aYou are already authenticated."));
                                return 1;
                            }

                            if (!auth.isRegistered(username)) {
                                player.sendMessage(Text.literal("§cNo password registered! Use /register <password> <confirm>"));
                                return 1;
                            }

                            if (auth.verifyPassword(username, password)) {
                                auth.markAuthenticated(player.getUuid());
                                mod.getNetworkHandler().sendAuthSuccess(player);
                                player.sendMessage(Text.literal("§a[MCVCAuth] §aAuthentication successful!"));
                                MCVCAuthMod.LOGGER.info("Player '{}' logged in", username);
                            } else {
                                player.sendMessage(Text.literal("§c[MCVCAuth] §cWrong password! Try again."));
                            }
                            return 1;
                        }))
                .executes(ctx -> {
                    ctx.getSource().sendError(Text.literal("Usage: /login <password>"));
                    return 0;
                })
        );
    }
}
