package com.acstelic.serverannounce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;
import java.util.Random;

public class AnnouncePlugin extends JavaPlugin {

    private BukkitTask announceTask;
    private int nextIndex = 0;
    private final Random random = new Random();

    /** Non-null only when the floodgate plugin is installed on this server. */
    private FloodgateApi floodgate;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Detect Floodgate (companion auth plugin for Geyser) at runtime.
        if (getServer().getPluginManager().getPlugin("floodgate") != null) {
            try {
                floodgate = FloodgateApi.getInstance();
                getLogger().info("Floodgate detected: Bedrock players will get announcement titles.");
            } catch (NoClassDefFoundError | Exception e) {
                getLogger().warning("Floodgate was found but its API failed to load: " + e.getMessage());
            }
        }

        AnnounceCommand executor = new AnnounceCommand(this);
        PluginCommand cmd = getCommand("announce");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        scheduleAnnouncements();
        getLogger().info("ServerAnnounce enabled.");
    }

    @Override
    public void onDisable() {
        stopTask();
    }

    public void stopTask() {
        if (announceTask != null) {
            announceTask.cancel();
            announceTask = null;
        }
    }

    /** (Re)starts the automatic announcement timer from the current config. */
    public void scheduleAnnouncements() {
        stopTask();
        long minutes = Math.max(1, getConfig().getLong("interval-minutes", 15));
        announceTask = Bukkit.getScheduler().runTaskTimer(
                this, this::broadcastNextAnnouncement, 20L * 60 * minutes, 20L * 60 * minutes);
    }

    /** Picks the next configured message and broadcasts it to everyone. */
    public void broadcastNextAnnouncement() {
        List<String> messages = getConfig().getStringList("auto-announcements");
        if (messages.isEmpty()) {
            return;
        }
        String mode = getConfig().getString("rotation-mode", "sequential");
        String raw;
        if ("random".equalsIgnoreCase(mode)) {
            raw = messages.get(random.nextInt(messages.size()));
        } else {
            if (nextIndex >= messages.size()) nextIndex = 0;
            raw = messages.get(nextIndex++);
        }
        broadcast(raw);
    }

    /**
     * Broadcasts one announcement to all online players.
     * Java players: colored chat message with prefix.
     * Bedrock players (via Floodgate): colored chat message plus an on-screen title.
     */
    public void broadcast(String rawMessage) {
        String prefix = color(getConfig().getString("prefix", "&b[Server] "));
        String chatLine = prefix + color(rawMessage);

        boolean titlesForBedrock = getConfig().getBoolean("bedrock-titles", true);
        boolean titlesForJava = getConfig().getBoolean("java-titles", false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(chatLine);
            boolean isBedrock = isBedrock(player);
            if ((isBedrock && titlesForBedrock) || (!isBedrock && titlesForJava)) {
                sendTitle(player, rawMessage);
            }
        }
        // Also echo to the console log
        getServer().getConsoleSender().sendMessage(chatLine);
    }

    /** Sends a title/subtitle to a player. Geyser translates title packets to Bedrock. */
    private void sendTitle(Player player, String rawMessage) {
        // Keep titles short: take text before the first "|" as the title,
        // the rest (if any) as the subtitle, e.g. "&aEvent tonight! | &7Parkour at 8pm"
        String[] parts = rawMessage.split("\\|", 2);
        String title = ChatColor.stripColor(color(parts[0]));
        String subtitle = parts.length > 1 ? ChatColor.stripColor(color(parts[1])) : "";

        int fadeIn = getConfig().getInt("title.fade-in-ticks", 10);
        int stay = getConfig().getInt("title.stay-ticks", 60);
        int fadeOut = getConfig().getInt("title.fade-out-ticks", 10);

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    private boolean isBedrock(Player player) {
        if (floodgate == null) {
            return false;
        }
        try {
            return floodgate.isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    /** Translates legacy '&x' color codes; strips the prefix marker if present. */
    public static String color(String text) {
        if (text == null) return "";
        if (text.startsWith("prefix")) text = text.substring(6);
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
