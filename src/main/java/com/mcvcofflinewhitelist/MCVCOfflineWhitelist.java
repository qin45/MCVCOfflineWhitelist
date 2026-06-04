package com.mcvcofflinewhitelist;

import com.google.inject.Inject;
import com.mcvcofflinewhitelist.command.WhitelistCommand;
import com.mcvcofflinewhitelist.config.WhitelistManager;
import com.mcvcofflinewhitelist.listener.PlayerListener;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
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

    private final ProxyServer proxy;
    private final Logger logger;
    private final WhitelistManager whitelistManager;

    @Inject
    public MCVCOfflineWhitelist(ProxyServer proxy, Logger logger,
            @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.whitelistManager = new WhitelistManager(dataDirectory, logger);

        // Defer listener registration — neither EventManager nor Scheduler
        // accept unregistered plugin refs in this Velocity SNAPSHOT build.
        deferredInit();

        logger.info("MCVCOfflineWhitelist v1.0.0 constructed");
    }

    /**
     * Starts a daemon thread that waits for plugin registration to complete,
     * then registers event listeners and logs the enabled message.
     */
    private void deferredInit() {
        Thread t = new Thread(() -> {
            try {
                // Allow time for the plugin container to be registered
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            proxy.getEventManager().register(MCVCOfflineWhitelist.this,
                    new PlayerListener(MCVCOfflineWhitelist.this));

            // Register /vow command
            CommandMeta meta = proxy.getCommandManager()
                    .metaBuilder("vow")
                    .plugin(MCVCOfflineWhitelist.this)
                    .build();
            proxy.getCommandManager().register(meta,
                    new WhitelistCommand(MCVCOfflineWhitelist.this));

            logger.info("MCVCOfflineWhitelist enabled. Whitelist: {}",
                    whitelistManager.getWhitelistFile().toAbsolutePath().normalize());
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
}
