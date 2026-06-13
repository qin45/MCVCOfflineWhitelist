package com.mcvcofflinewhitelist.auth;

import com.mcvcofflinewhitelist.auth.command.LoginCommand;
import com.mcvcofflinewhitelist.auth.command.RegisterCommand;
import com.mcvcofflinewhitelist.auth.network.AuthNetworkHandler;
import com.mcvcofflinewhitelist.auth.network.AuthPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCVCAuthMod implements ModInitializer {
    public static final String MOD_ID = "mcvcauth";
    public static final String AUTH_CHANNEL = "mcvcofflinewhitelist:auth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static MCVCAuthMod instance;
    private AuthManager authManager;
    private AuthNetworkHandler networkHandler;

    public static MCVCAuthMod getInstance() {
        return instance;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public AuthNetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("MCVCAuth mod initializing...");

        this.authManager = new AuthManager();
        this.networkHandler = new AuthNetworkHandler(this);

        // ── Register payload types (required by Fabric API 0.100+) ──
        // C2S — proxy sends to mod (appears as if client sent it)
        PayloadTypeRegistry.playC2S().register(AuthPayload.ID, AuthPayload.CODEC);
        // S2C — mod sends to proxy (appears as if server sends to client)
        PayloadTypeRegistry.playS2C().register(AuthPayload.ID, AuthPayload.CODEC);

        // Register plugin-message receiver (C2S — messages from proxy)
        networkHandler.registerReceiver();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LoginCommand.register(dispatcher, this);
            RegisterCommand.register(dispatcher, this);
        });

        // On player join, send check_player query to the proxy
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            networkHandler.sendCheckPlayer(player);
        });

        LOGGER.info("MCVCAuth mod initialized");
    }
}
