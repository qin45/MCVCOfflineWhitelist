package com.mcvcofflinewhitelist.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks both pending and authenticated players for the
 * password-auth flow with the Fabric mod.
 *
 * <p>Players are added to the pending set when the Fabric mod
 * first sends a {@code check_player} query and the proxy confirms
 * they are on the offline whitelist. Once they authenticate via
 * {@code auth_success}, they are moved to the {@link #authenticated}
 * set so that switching backend servers does NOT re-trigger auth.</p>
 */
public class AuthManager {

    private final Set<UUID> pendingAuth = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> authenticated = Collections.synchronizedSet(new HashSet<>());

    // ------------------------------------------------------------------
    // Pending (needs auth right now)
    // ------------------------------------------------------------------

    /**
     * Marks a player as pending password authentication.
     */
    public void markPending(UUID playerUuid) {
        pendingAuth.add(playerUuid);
    }

    /**
     * Returns {@code true} if the player is still waiting for
     * password authentication.
     */
    public boolean isPending(UUID playerUuid) {
        return pendingAuth.contains(playerUuid);
    }

    /**
     * Returns the number of players currently pending authentication.
     */
    public int getPendingCount() {
        return pendingAuth.size();
    }

    // ------------------------------------------------------------------
    // Authenticated (has completed auth this session)
    // ------------------------------------------------------------------

    /**
     * Marks the player as fully authenticated. Removes from pending
     * and adds to the authenticated set so future server switches
     * do not re-trigger auth.
     */
    public void markAuthenticated(UUID playerUuid) {
        pendingAuth.remove(playerUuid);
        authenticated.add(playerUuid);
    }

    /**
     * Returns {@code true} if the player has authenticated at least
     * once this session (even if they also appear in pending because
     * of a race / server-switch).
     */
    public boolean isAuthenticated(UUID playerUuid) {
        return authenticated.contains(playerUuid);
    }

    /**
     * Removes a player from both pending and authenticated sets.
     * Called on disconnect so that reconnecting forces re-authentication.
     */
    public void removePlayer(UUID playerUuid) {
        pendingAuth.remove(playerUuid);
        authenticated.remove(playerUuid);
    }
}
