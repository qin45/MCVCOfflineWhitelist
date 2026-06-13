package com.mcvcofflinewhitelist;

import com.google.inject.Inject;
import com.mcvcofflinewhitelist.auth.AuthManager;
import com.mcvcofflinewhitelist.command.WhitelistCommand;
import com.mcvcofflinewhitelist.config.WhitelistManager;
import com.mcvcofflinewhitelist.listener.AuthListener;
import com.mcvcofflinewhitelist.listener.PlayerListener;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.nio.file.Path;
import org.slf4j.Logger;

/**
 * Velocity plugin that allows whitelisted offline-mode players to connect
 * to an online-mode proxy via {@code forceOfflineMode()} on {@code PreLoginEvent}.
 *
 * <p>Listener registration is deferred via a daemon thread (instead of in the
 * constructor) because recent Velocity SNAPSHOT builds validate the plugin
 * container during both {@code EventManager.register()} and
 * {@code Scheduler.buildTask()} — and the plugin is not yet registered with
 * the PluginManager when the constructor runs.</p>
 */

@Plugin(
    id = "mcvcofflinewhitelist",
    name = "MCVCOfflineWhitelist",
    version = "1.0.0",
    authors = {"qin45"}
)
public class MCVCOfflineWhitelist {

    /** Plugin messaging channel for password auth with the Fabric mod. */
    public static final MinecraftChannelIdentifier AUTH_CHANNEL =
            MinecraftChannelIdentifier.create("mcvcofflinewhitelist", "auth");

    private final ProxyServer proxy;
    private final Logger logger;
    private final WhitelistManager whitelistManager;
    private final AuthManager authManager;

    @Inject
    public MCVCOfflineWhitelist(ProxyServer proxy, Logger logger,
            @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.whitelistManager = new WhitelistManager(dataDirectory, logger);
        this.authManager = new AuthManager();

        // Defer listener/channel/command registration — neither
        // EventManager nor Scheduler accept unregistered plugin refs
        // in this Velocity SNAPSHOT build.
        deferredInit();

        logger.info("MCVCOfflineWhitelist v1.0.0 constructed");
    }

    /**
     * Starts a daemon thread that waits for plugin registration to complete,
     * then registers event listeners, the plugin-messaging channel, and the
     * /vow command.
     */
    private void deferredInit() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Register plugin-messaging channel for Fabric-mod auth
            proxy.getChannelRegistrar().register(AUTH_CHANNEL);

            // Register event listeners
            proxy.getEventManager().register(MCVCOfflineWhitelist.this,
                    new PlayerListener(MCVCOfflineWhitelist.this));
            proxy.getEventManager().register(MCVCOfflineWhitelist.this,
                    new AuthListener(MCVCOfflineWhitelist.this, AUTH_CHANNEL));

            // Register /vow command
            CommandMeta meta = proxy.getCommandManager()
                    .metaBuilder("vow")
                    .plugin(MCVCOfflineWhitelist.this)
                    .build();
            proxy.getCommandManager().register(meta,
                    new WhitelistCommand(MCVCOfflineWhitelist.this));

            logger.info("MCVCOfflineWhitelist enabled (auth channel: {})",
                    AUTH_CHANNEL.getId());
        });
        t.setDaemon(true);
        t.setName("mcvcofflinewhitelist-init");
        t.start();
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }
}
