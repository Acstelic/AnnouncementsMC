package com.acstelic.serverannounce;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AnnounceCommand implements CommandExecutor, TabCompleter {

    private final AnnouncePlugin plugin;

    public AnnounceCommand(AnnouncePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!sender.hasPermission("serverannounce.use")) {
            sender.sendMessage(AnnouncePlugin.color("&cYou don't have permission to use this."));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.scheduleAnnouncements();
            sender.sendMessage(AnnouncePlugin.color("&aServerAnnounce configuration reloaded."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(AnnouncePlugin.color("&eUsage: /announce <message> | &e/announce reload"));
            return true;
        }

        String message = String.join(" ", args);
        plugin.broadcast(message);
        sender.sendMessage(AnnouncePlugin.color("&aAnnouncement sent to "
                + Bukkit.getOnlinePlayers().size() + " player(s)."));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("serverannounce.use")) {
            out.add("reload");
        }
        return out;
    }
}
