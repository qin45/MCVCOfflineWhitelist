package com.mcvcofflinewhitelist.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players are waiting for password authentication
 * on the backend server.
 *
 * <p>Players are added to the pending set when the Fabric mod
 * sends a {@code check_player} query and the proxy confirms they
 * are on the offline whitelist. They are removed when the mod
 * sends {@code auth_success}.</p>
 */
public class AuthManager {

    private final Set<UUID> pendingAuth = Collections.synchronizedSet(new HashSet<>());

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
     * Removes the player from the pending set (authentication complete).
     */
    public void markAuthenticated(UUID playerUuid) {
        pendingAuth.remove(playerUuid);
    }

    /**
     * Returns the number of players currently pending authentication.
     */
    public int getPendingCount() {
        return pendingAuth.size();
    }
}
