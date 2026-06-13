package com.mcvcofflinewhitelist.auth.command;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /register <password> <confirm> — set a password for the first time.
 */
public class RegisterCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, MCVCAuthMod mod) {
        dispatcher.register(CommandManager.literal("register")
                .then(CommandManager.argument("password", StringArgumentType.string())
                        .then(CommandManager.argument("confirm", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    ServerPlayerEntity player = source.getPlayer();
                                    if (player == null) return 0;

                                    String password = StringArgumentType.getString(ctx, "password");
                                    String confirm = StringArgumentType.getString(ctx, "confirm");
                                    var auth = mod.getAuthManager();
                                    String username = player.getName().getString();

                                    if (!auth.needsAuth(player.getUuid())) {
                                        player.sendMessage(Text.literal("§aYou are already authenticated."));
                                        return 1;
                                    }

                                    if (auth.isRegistered(username)) {
                                        player.sendMessage(Text.literal("§cYou are already registered! Use /login <password>"));
                                        return 1;
                                    }

                                    if (!password.equals(confirm)) {
                                        player.sendMessage(Text.literal("§cPasswords do not match!"));
                                        return 1;
                                    }

                                    if (password.length() < 4) {
                                        player.sendMessage(Text.literal("§cPassword must be at least 4 characters!"));
                                        return 1;
                                    }

                                    auth.registerPassword(username, password);
                                    auth.markAuthenticated(player.getUuid());
                                    mod.getNetworkHandler().sendAuthSuccess(player);
                                    player.sendMessage(Text.literal("§a[MCVCAuth] §aRegistered and authenticated!"));
                                    MCVCAuthMod.LOGGER.info("Player '{}' registered password", username);
                                    return 1;
                                })))
                .executes(ctx -> {
                    ctx.getSource().sendError(Text.literal("Usage: /register <password> <confirm>"));
                    return 0;
                })
        );
    }
}
