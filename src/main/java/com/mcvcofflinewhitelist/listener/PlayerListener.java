package com.mcvcofflinewhitelist.listener;

import com.mcvcofflinewhitelist.MCVCOfflineWhitelist;
import com.mcvcofflinewhitelist.config.WhitelistManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;

/**
 * Listens for player connection events and enforces the offline whitelist.
 *
 * <p>On {@link PreLoginEvent} (fired <em>before</em> Mojang authentication):
 * <ul>
 *   <li>If the username is on the offline whitelist → force offline mode
 *       so the player can join without premium authentication.</li>
 *   <li>Otherwise → leave the default result so Velocity attempts
 *       standard online-mode authentication with Mojang.</li>
 * </ul>
 */
public class PlayerListener {

    private final MCVCOfflineWhitelist plugin;

    public PlayerListener(MCVCOfflineWhitelist plugin) {
        this.plugin = plugin;
    }

    @Subscribe(async = false)
    public void onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();
        WhitelistManager whitelist = plugin.getWhitelistManager();

        if (whitelist.isWhitelisted(username)) {
            plugin.getLogger().info(
                    "Offline-mode player '{}' found in whitelist, forcing offline mode",
                    username);
            event.setResult(PreLoginComponentResult.forceOfflineMode());
        } else {
            plugin.getLogger().info(
                    "Player '{}' NOT in offline whitelist, using online auth",
                    username);
        }
    }
}
