package com.mcvcofflinewhitelist.command;

import com.mcvcofflinewhitelist.MCVCOfflineWhitelist;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Proxy-console command to hot-manage the offline whitelist without restarting.
 *
 * <p>Usage (console only):</p>
 * <ul>
 *   <li>{@code /vow add <player>}   — add a player to the whitelist</li>
 *   <li>{@code /vow remove <player>}— remove a player from the whitelist</li>
 *   <li>{@code /vow list}           — list all whitelisted players</li>
 *   <li>{@code /vow reload}         — reload the whitelist from file</li>
 * </ul>
 */
public class WhitelistCommand implements SimpleCommand {

    private final MCVCOfflineWhitelist plugin;

    public WhitelistCommand(MCVCOfflineWhitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final Invocation invocation) {
        // Only console (non-player) may run this command
        if (invocation.source() instanceof Player) {
            invocation.source().sendMessage(
                    Component.text("Only the proxy console can use this command.")
                            .color(NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sendUsage(invocation.source());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(invocation.source(), args);
            case "remove" -> handleRemove(invocation.source(), args);
            case "list" -> handleList(invocation.source());
            case "reload" -> handleReload(invocation.source());
            default -> sendUsage(invocation.source());
        }
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return List.of("add", "remove", "list", "reload");
        }
        if (args.length == 1) {
            return List.of("add", "remove", "list", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return List.of();
    }

    private void sendUsage(com.velocitypowered.api.command.CommandSource source) {
        source.sendMessage(Component.text("Usage:")
                .color(NamedTextColor.GOLD));
        source.sendMessage(Component.text("  /vow add <player>    — Add a player to the whitelist")
                .color(NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /vow remove <player> — Remove a player from the whitelist")
                .color(NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /vow list            — List all whitelisted players")
                .color(NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /vow reload          — Reload whitelist from file")
                .color(NamedTextColor.WHITE));
    }

    private void handleAdd(com.velocitypowered.api.command.CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: /vow add <player>")
                    .color(NamedTextColor.RED));
            return;
        }
        String name = args[1];
        boolean added = plugin.getWhitelistManager().addPlayer(name);
        if (added) {
            source.sendMessage(Component.text("Added '" + name + "' to the offline whitelist.")
                    .color(NamedTextColor.GREEN));
        } else {
            source.sendMessage(Component.text("'" + name + "' is already in the whitelist.")
                    .color(NamedTextColor.YELLOW));
        }
    }

    private void handleRemove(com.velocitypowered.api.command.CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(Component.text("Usage: /vow remove <player>")
                    .color(NamedTextColor.RED));
            return;
        }
        String name = args[1];
        boolean removed = plugin.getWhitelistManager().removePlayer(name);
        if (removed) {
            source.sendMessage(Component.text("Removed '" + name + "' from the offline whitelist.")
                    .color(NamedTextColor.GREEN));
        } else {
            source.sendMessage(Component.text("'" + name + "' is not in the whitelist.")
                    .color(NamedTextColor.YELLOW));
        }
    }

    private void handleList(com.velocitypowered.api.command.CommandSource source) {
        var names = plugin.getWhitelistManager().getWhitelistedNames();
        if (names.isEmpty()) {
            source.sendMessage(Component.text("The offline whitelist is empty.")
                    .color(NamedTextColor.YELLOW));
            return;
        }
        source.sendMessage(Component.text("Offline whitelist (" + names.size() + " entries):")
                .color(NamedTextColor.GOLD));
        for (String name : names) {
            source.sendMessage(Component.text("  - " + name)
                    .color(NamedTextColor.WHITE));
        }
    }

    private void handleReload(com.velocitypowered.api.command.CommandSource source) {
        plugin.getWhitelistManager().reload();
        source.sendMessage(Component.text(
                "Whitelist reloaded. (" + plugin.getWhitelistManager().size() + " entries)")
                .color(NamedTextColor.GREEN));
    }
}
