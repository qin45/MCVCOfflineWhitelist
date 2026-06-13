package com.mcvcofflinewhitelist.auth.network;

import com.mcvcofflinewhitelist.auth.MCVCAuthMod;
import com.mcvcofflinewhitelist.auth.AuthManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

/**
 * Handles plugin-message communication with the Velocity proxy
 * using the 1.21 {@link CustomPayload} networking API.
 *
 * <p>Channel: {@code mcvcofflinewhitelist:auth}</p>
 */
public class AuthNetworkHandler {

    private final MCVCAuthMod mod;

    public AuthNetworkHandler(MCVCAuthMod mod) {
        this.mod = mod;
    }

    /**
     * Register the global receiver for messages from the proxy.
     */
    public void registerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(AuthPayload.ID, (payload, context) -> {
            String msg = payload.text();
            String[] parts = msg.split("\\|", 4);
            if (parts.length < 1) return;

            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                switch (parts[0]) {
                    case "whitelist_status" -> handleWhitelistStatus(parts, player);
                    case "auth_confirmed" -> handleAuthConfirmed(parts, player);
                }
            });
        });
    }

    /**
     * Send a {@code check_player} query to the proxy when a player joins.
     */
    public void sendCheckPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        String username = player.getName().getString();
        String payload = "check_player|" + uuid + "|" + username;
        ServerPlayNetworking.send(player, new AuthPayload(payload));
        MCVCAuthMod.LOGGER.info("Sent check_player for '{}'", username);
    }

    private void handleWhitelistStatus(String[] parts, ServerPlayerEntity player) {
        if (parts.length < 4) return;
        boolean whitelisted = Boolean.parseBoolean(parts[3]);

        if (whitelisted) {
            AuthManager auth = mod.getAuthManager();
            UUID uuid = player.getUuid();
            String username = player.getName().getString();

            auth.markNeedsAuth(uuid);

            if (auth.isRegistered(username)) {
                player.sendMessage(
                        net.minecraft.text.Text.literal(
                                "§6[MCVCAuth] §e请输入密码登录: /login <密码>"));
            } else {
                player.sendMessage(
                        net.minecraft.text.Text.literal(
                                "§6[MCVCAuth] §e请注册密码: /register <密码> <确认密码>"));
            }
            MCVCAuthMod.LOGGER.info("Player '{}' needs auth (whitelisted)", username);
        }
    }

    private void handleAuthConfirmed(String[] parts, ServerPlayerEntity player) {
        player.sendMessage(
                net.minecraft.text.Text.literal("§a[MCVCAuth] §a验证完成！"));
        MCVCAuthMod.LOGGER.info("Proxy confirmed auth for '{}'",
                player.getName().getString());
    }

    /**
     * Send {@code auth_success} to proxy after password verification.
     */
    public void sendAuthSuccess(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        String username = player.getName().getString();
        String payload = "auth_success|" + uuid + "|" + username;
        ServerPlayNetworking.send(player, new AuthPayload(payload));
        MCVCAuthMod.LOGGER.info("Sent auth_success for '{}'", username);
    }
}
