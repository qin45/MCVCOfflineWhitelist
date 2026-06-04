package com.mcvcofflinewhitelist.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

/**
 * Manages the offline-mode player whitelist.
 *
 * <p>The whitelist is stored as a plain text file (one username per line).
 * Lines starting with {@code #} and blank lines are ignored.
 * Matching is case-insensitive.</p>
 */
public class WhitelistManager {

    private final Path whitelistFile;
    private final Logger logger;
    private Set<String> whitelist = Collections.emptySet();

    public WhitelistManager(Path dataDirectory, Logger logger) {
        this.whitelistFile = dataDirectory.resolve("whitelist.txt");
        this.logger = logger;
        ensureFileExists();
        reload();
    }

    /**
     * Returns the path to the whitelist file.
     */
    public Path getWhitelistFile() {
        return whitelistFile;
    }

    /**
     * Creates a default empty whitelist file if it does not exist.
     */
    private void ensureFileExists() {
        try {
            Files.createDirectories(whitelistFile.getParent());
            if (Files.notExists(whitelistFile)) {
                Files.createFile(whitelistFile);
                logger.info("Created empty whitelist file at {}", whitelistFile);
            }
        } catch (IOException e) {
            logger.error("Failed to create whitelist file at {}", whitelistFile, e);
        }
    }

    /**
     * (Re)loads the whitelist from disk.
     */
    public void reload() {
        try (Stream<String> lines = Files.lines(whitelistFile)) {
            whitelist = lines
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(HashSet::new));
            logger.info("Loaded offline whitelist ({} entries)", whitelist.size());
        } catch (IOException e) {
            logger.error("Failed to read whitelist file: {}", whitelistFile, e);
            whitelist = Collections.emptySet();
        }
    }

    /**
     * Returns {@code true} if the given username is on the offline whitelist.
     */
    public boolean isWhitelisted(String username) {
        return whitelist.contains(username.toLowerCase());
    }

    /**
     * Returns an unmodifiable view of the current whitelist (lowercased names).
     */
    public Set<String> getWhitelistedNames() {
        return Collections.unmodifiableSet(whitelist);
    }

    /**
     * Returns the number of whitelisted entries.
     */
    public int size() {
        return whitelist.size();
    }

    /**
     * Adds a player to the whitelist (case-insensitive).
     * Updates both the in-memory set and the file on disk.
     *
     * @return {@code true} if the player was newly added
     */
    public boolean addPlayer(String username) {
        String lower = username.toLowerCase();
        boolean added = whitelist.add(lower);
        if (added) {
            try {
                Files.writeString(whitelistFile, username + System.lineSeparator(),
                        StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                logger.info("Added '{}' to offline whitelist", username);
            } catch (IOException e) {
                logger.error("Failed to write to whitelist file: {}", whitelistFile, e);
                whitelist.remove(lower); // rollback
                return false;
            }
        }
        return added;
    }

    /**
     * Removes a player from the whitelist (case-insensitive).
     * Updates both the in-memory set and the file on disk.
     *
     * @return {@code true} if the player was in the whitelist and removed
     */
    public boolean removePlayer(String username) {
        String lower = username.toLowerCase();
        boolean removed = whitelist.remove(lower);
        if (removed) {
            try {
                List<String> lines = Files.readAllLines(whitelistFile, StandardCharsets.UTF_8);
                List<String> filtered = lines.stream()
                        .filter(line -> !line.trim().equalsIgnoreCase(username))
                        .collect(Collectors.toList());
                Files.write(whitelistFile, filtered, StandardCharsets.UTF_8);
                logger.info("Removed '{}' from offline whitelist", username);
            } catch (IOException e) {
                logger.error("Failed to update whitelist file: {}", whitelistFile, e);
                whitelist.add(lower); // rollback
                return false;
            }
        }
        return removed;
    }
}
