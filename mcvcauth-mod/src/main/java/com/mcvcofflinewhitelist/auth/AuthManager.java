package com.mcvcofflinewhitelist.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Manages password storage (BCrypt) and per-session authentication state.
 *
 * <p>Password file: {@code config/mcvcauth/passwords.json}</p>
 */
public class AuthManager {

    private static final Path PASSWORDS_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("mcvcauth/passwords.json");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Players who are whitelisted and still need to authenticate this session. */
    private final Set<UUID> needsAuth = ConcurrentHashMap.newKeySet();

    /** Players who have successfully authenticated this session. */
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();

    /** Password hashes keyed by lowercased username. */
    private final Map<String, String> passwordHashes = new ConcurrentHashMap<>();

    public AuthManager() {
        loadPasswords();
    }

    // ------------------------------------------------------------------
    // Persistent password storage
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void loadPasswords() {
        if (!Files.exists(PASSWORDS_FILE)) {
            return;
        }
        try {
            String json = Files.readString(PASSWORDS_FILE, StandardCharsets.UTF_8);
            Map<String, Object> data = gson.fromJson(json, Map.class);
            if (data != null && data.containsKey("players")) {
                Map<String, Map<String, Object>> players =
                        (Map<String, Map<String, Object>>) data.get("players");
                players.forEach((name, entry) -> {
                    if (entry.containsKey("hash")) {
                        passwordHashes.put(name.toLowerCase(), (String) entry.get("hash"));
                    }
                });
            }
            MCVCAuthMod.LOGGER.info("Loaded {} password hashes", passwordHashes.size());
        } catch (Exception e) {
            MCVCAuthMod.LOGGER.error("Failed to load passwords file: {}", PASSWORDS_FILE, e);
        }
    }

    private void savePasswords() {
        try {
            Files.createDirectories(PASSWORDS_FILE.getParent());
            Map<String, Object> players = new HashMap<>();
            passwordHashes.forEach((name, hash) -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("hash", hash);
                entry.put("registeredAt", System.currentTimeMillis());
                players.put(name, entry);
            });
            Map<String, Object> data = new HashMap<>();
            data.put("players", players);
            String json = gson.toJson(data);
            Files.writeString(PASSWORDS_FILE, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            MCVCAuthMod.LOGGER.error("Failed to save passwords file: {}", PASSWORDS_FILE, e);
        }
    }

    // ------------------------------------------------------------------
    // Per-session auth state
    // ------------------------------------------------------------------

    /**
     * Mark a whitelisted player as needing password authentication.
     */
    public void markNeedsAuth(UUID playerUuid) {
        needsAuth.add(playerUuid);
    }

    /**
     * Returns {@code true} if the player still needs to authenticate.
     */
    public boolean needsAuth(UUID playerUuid) {
        return needsAuth.contains(playerUuid);
    }

    /**
     * Returns {@code true} if the player has authenticated this session.
     */
    public boolean isAuthenticated(UUID playerUuid) {
        return authenticated.contains(playerUuid);
    }

    /**
     * Mark player as successfully authenticated (session).
     */
    public void markAuthenticated(UUID playerUuid) {
        needsAuth.remove(playerUuid);
        authenticated.add(playerUuid);
    }

    // ------------------------------------------------------------------
    // Password operations
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} if a password has been registered for this player.
     */
    public boolean isRegistered(String username) {
        return passwordHashes.containsKey(username.toLowerCase());
    }

    /**
     * Register (hash and store) a password for the player.
     */
    public void registerPassword(String username, String password) {
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        passwordHashes.put(username.toLowerCase(), hash);
        savePasswords();
        MCVCAuthMod.LOGGER.info("Password registered for '{}'", username);
    }

    /**
     * Verify a password attempt against the stored hash.
     *
     * @return {@code true} if the password is correct
     */
    public boolean verifyPassword(String username, String password) {
        String hash = passwordHashes.get(username.toLowerCase());
        if (hash == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
        return result.verified;
    }
}
