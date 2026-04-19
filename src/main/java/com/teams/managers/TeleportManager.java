package com.teams.managers;

import com.teams.TeamsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final TeamsPlugin plugin;
    private final Map<UUID, Long> homeCooldowns = new HashMap<>();
    private final Map<UUID, Long> warpCooldowns = new HashMap<>();
    private final Map<UUID, Integer> pendingTeleports = new HashMap<>(); // uuid -> taskId
    private final Map<UUID, Location> teleportStartLocations = new HashMap<>();

    public TeleportManager(TeamsPlugin plugin) { this.plugin = plugin; }

    public void teleportHome(Player player, Location dest) {
        int cooldown = plugin.getConfig().getInt("home.cooldown", 60);
        int wait = plugin.getConfig().getInt("home.wait-time", 10);
        if (isOnHomeCooldown(player.getUniqueId())) {
            long remaining = getRemainingHomeCooldown(player.getUniqueId());
            String msg = plugin.getConfig().getString("messages.teleport-cooldown", "§cWait {time}s!")
                .replace("{time}", String.valueOf(remaining));
            player.sendMessage(msg);
            return;
        }
        startTeleport(player, dest, wait, () -> homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L)));
    }

    public void teleportWarp(Player player, Location dest) {
        int cooldown = plugin.getConfig().getInt("warp.cooldown", 30);
        int wait = plugin.getConfig().getInt("warp.wait-time", 10);
        if (isOnWarpCooldown(player.getUniqueId())) {
            long remaining = getRemainingWarpCooldown(player.getUniqueId());
            String msg = plugin.getConfig().getString("messages.teleport-cooldown", "§cWait {time}s!")
                .replace("{time}", String.valueOf(remaining));
            player.sendMessage(msg);
            return;
        }
        startTeleport(player, dest, wait, () -> warpCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L)));
    }

    private void startTeleport(Player player, Location dest, int waitSeconds, Runnable onSuccess) {
        // Cancel any existing pending teleport
        cancelTeleport(player.getUniqueId());

        teleportStartLocations.put(player.getUniqueId(), player.getLocation().clone());

        String msg = plugin.getConfig().getString("messages.teleport-wait", "§eTeleporting in {time}s. Don't move!")
            .replace("{time}", String.valueOf(waitSeconds));
        player.sendMessage(msg);

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(player.getUniqueId());
            teleportStartLocations.remove(player.getUniqueId());
            player.teleport(dest);
            onSuccess.run();
        }, waitSeconds * 20L).getTaskId();

        pendingTeleports.put(player.getUniqueId(), taskId);
    }

    public void cancelTeleport(UUID uuid) {
        Integer taskId = pendingTeleports.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        teleportStartLocations.remove(uuid);
    }

    public boolean hasPendingTeleport(UUID uuid) { return pendingTeleports.containsKey(uuid); }
    public Location getTeleportStartLocation(UUID uuid) { return teleportStartLocations.get(uuid); }

    public boolean isOnHomeCooldown(UUID uuid) {
        Long time = homeCooldowns.get(uuid);
        return time != null && System.currentTimeMillis() < time;
    }

    public boolean isOnWarpCooldown(UUID uuid) {
        Long time = warpCooldowns.get(uuid);
        return time != null && System.currentTimeMillis() < time;
    }

    public long getRemainingHomeCooldown(UUID uuid) {
        Long time = homeCooldowns.get(uuid);
        if (time == null) return 0;
        return Math.max(0, (time - System.currentTimeMillis()) / 1000);
    }

    public long getRemainingWarpCooldown(UUID uuid) {
        Long time = warpCooldowns.get(uuid);
        if (time == null) return 0;
        return Math.max(0, (time - System.currentTimeMillis()) / 1000);
    }
}
