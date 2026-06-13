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
                                        player.sendMessage(Text.literal("§a已验证，无需重复注册"));
                                        return 1;
                                    }

                                    if (auth.isRegistered(username)) {
                                        player.sendMessage(Text.literal("§c已注册！请使用 /login <密码>"));
                                        return 1;
                                    }

                                    if (!password.equals(confirm)) {
                                        player.sendMessage(Text.literal("§c两次输入的密码不一致！"));
                                        return 1;
                                    }

                                    if (password.length() < 4) {
                                        player.sendMessage(Text.literal("§c密码长度至少为4个字符！"));
                                        return 1;
                                    }

                                    auth.registerPassword(username, password);
                                    auth.markAuthenticated(player.getUuid());
                                    // Restore visibility and vulnerability
                                    player.setInvisible(false);
                                    player.setInvulnerable(false);
                                    mod.getNetworkHandler().sendAuthSuccess(player);
                                    player.sendMessage(Text.literal("§a[MCVCAuth] §a注册并验证成功！"));
                                    MCVCAuthMod.LOGGER.info("Player '{}' registered password", username);
                                    return 1;
                                })))
                .executes(ctx -> {
                    ctx.getSource().sendError(Text.literal("用法: /register <密码> <确认密码>"));
                    return 0;
                })
        );
    }
}
