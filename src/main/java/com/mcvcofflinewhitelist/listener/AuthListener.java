package com.mcvcofflinewhitelist.listener;

import com.mcvcofflinewhitelist.MCVCOfflineWhitelist;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Handles server-side password authentication flow via plugin messaging
 * with a Fabric mod on the backend server.
 *
 * <p>Protocol (channel {@code mcvcofflinewhitelist:auth}):</p>
 * <ul>
 *   <li>Server → Proxy: {@code check_player|<uuid>|<username>}</li>
 *   <li>Proxy → Server: {@code whitelist_status|<uuid>|<username>|true|false}</li>
 *   <li>Server → Proxy: {@code auth_success|<uuid>|<username>}</li>
 *   <li>Proxy → Server: {@code auth_confirmed|<uuid>|<username>}</li>
 * </ul>
 */
public class AuthListener {

    private final MCVCOfflineWhitelist plugin;
    private final ChannelIdentifier channel;

    public AuthListener(MCVCOfflineWhitelist plugin, ChannelIdentifier channel) {
        this.plugin = plugin;
        this.channel = channel;
    }

    // ------------------------------------------------------------------
    // Player joins a backend server
    // ------------------------------------------------------------------

    @Subscribe(async = false)
    public void onServerConnected(ServerConnectedEvent event) {
        // The Fabric mod will send a check_player query; we just log here.
        Player player = event.getPlayer();
        if (plugin.getWhitelistManager().isWhitelisted(player.getUsername())) {
            plugin.getLogger().info("Whitelisted player '{}' connected to '{}'",
                    player.getUsername(),
                    event.getServer().getServerInfo().getName());
        }
    }

    // ------------------------------------------------------------------
    // Block server switching while pending auth
    // ------------------------------------------------------------------

    @Subscribe(async = false)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        if (plugin.getAuthManager().isPending(player.getUniqueId())) {
            // Allow the initial connection (no current server yet) but
            // block any subsequent server switch until authenticated.
            if (player.getCurrentServer().isPresent()) {
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                player.sendMessage(Component.text(
                        "请先完成验证再切换服务器！使用 /login 登录")
                        .color(NamedTextColor.RED));
            }
        }
    }

    // ------------------------------------------------------------------
    // Plugin messaging from the Fabric mod on the backend server
    // ------------------------------------------------------------------

    @Subscribe(async = false)
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) {
            return;
        }
        // Only handle messages from backend servers, not from clients
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        String msg = new String(event.getData(), StandardCharsets.UTF_8);
        String[] parts = msg.split("\\|", 4);
        if (parts.length < 1) {
            return;
        }

        switch (parts[0]) {
            case "check_player" -> handleCheckPlayer(parts);
            case "auth_success" -> handleAuthSuccess(parts);
        }

        // Do NOT forward auth messages to the Minecraft client
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    // ------------------------------------------------------------------
    // Message handlers
    // ------------------------------------------------------------------

    /**
     * The Fabric mod asks: "is this player on the offline whitelist?"
     * Respond with the boolean status and mark them pending if whitelisted.
     */
    private void handleCheckPlayer(String[] parts) {
        if (parts.length < 3) {
            return;
        }
        UUID uuid = UUID.fromString(parts[1]);
        String username = parts[2];

        boolean whitelisted = plugin.getWhitelistManager().isWhitelisted(username);

        if (whitelisted) {
            plugin.getAuthManager().markPending(uuid);
            plugin.getLogger().info("Player '{}' marked for password auth", username);
        }

        respond(uuid, "whitelist_status", uuid.toString(), username,
                String.valueOf(whitelisted));
    }

    /**
     * The Fabric mod says: "player has entered correct password."
     * Confirm authentication and notify the server.
     */
    private void handleAuthSuccess(String[] parts) {
        if (parts.length < 3) {
            return;
        }
        UUID uuid = UUID.fromString(parts[1]);
        String username = parts[2];

        if (plugin.getAuthManager().isPending(uuid)) {
            plugin.getAuthManager().markAuthenticated(uuid);
            plugin.getLogger().info("Player '{}' authenticated (password)", username);
            respond(uuid, "auth_confirmed", uuid.toString(), username);
        }
    }

    // ------------------------------------------------------------------
    // Send a plugin message to the backend server that hosts the player
    // ------------------------------------------------------------------

    private void respond(UUID playerUuid, String type, String... data) {
        Player player = plugin.getProxy().getPlayer(playerUuid).orElse(null);
        if (player == null || player.getCurrentServer().isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(type);
        for (String d : data) {
            sb.append('|').append(d);
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        player.getCurrentServer().get().sendPluginMessage(channel, bytes);
    }
}
