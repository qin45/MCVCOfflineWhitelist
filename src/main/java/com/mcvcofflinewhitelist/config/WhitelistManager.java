package com.mcvcofflinewhitelist.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
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
}
