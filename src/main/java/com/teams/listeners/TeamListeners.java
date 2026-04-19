package com.teams.listeners;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

public class TeamListeners implements Listener {

    private final TeamsPlugin plugin;

    public TeamListeners(TeamsPlugin plugin) { this.plugin = plugin; }

    // Friendly fire prevention
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Team attackerTeam = plugin.getTeamManager().getPlayerTeam(attacker.getUniqueId());
        Team victimTeam = plugin.getTeamManager().getPlayerTeam(victim.getUniqueId());

        if (attackerTeam != null && victimTeam != null) {
            // Same team
            if (attackerTeam == victimTeam) {
                if (!attackerTeam.isPvpEnabled()) {
                    event.setCancelled(true);
                    attacker.sendMessage(plugin.getConfig().getString("messages.friendly-fire", "§cCan't attack teammate!"));
                    return;
                }
            }
            // Allied teams
            if (plugin.getTeamManager().areAllies(attackerTeam, victimTeam)) {
                event.setCancelled(true);
                attacker.sendMessage(plugin.getConfig().getString("messages.ally-fire", "§cCan't attack ally!"));
                return;
            }
        }

        // Cancel teleport if victim takes damage during wait
        if (plugin.getTeleportManager().hasPendingTeleport(victim.getUniqueId())) {
            plugin.getTeleportManager().cancelTeleport(victim.getUniqueId());
            victim.sendMessage(plugin.getConfig().getString("messages.teleport-cancelled", "§cTeleport cancelled!"));
        }
    }

    // Cancel teleport on move
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getTeleportManager().hasPendingTeleport(player.getUniqueId())) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            plugin.getTeleportManager().cancelTeleport(player.getUniqueId());
            player.sendMessage(plugin.getConfig().getString("messages.teleport-cancelled", "§cTeleport cancelled!"));
        }
    }

    // Chat handling
    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getChatManager().isTeamChat(player.getUniqueId())) {
            event.setCancelled(true);
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) return;
            plugin.getChatManager().sendTeamChat(player, team, event.getMessage());
        } else if (plugin.getChatManager().isAllyChat(player.getUniqueId())) {
            event.setCancelled(true);
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) return;
            plugin.getChatManager().sendAllyChat(player, team, event.getMessage());
        }
    }

    // Join - restore display
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () ->
            plugin.getDisplayManager().updatePlayer(event.getPlayer()), 5L);
    }

    // Quit - clean up
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getChatManager().clearPlayer(event.getPlayer().getUniqueId());
        plugin.getTeleportManager().cancelTeleport(event.getPlayer().getUniqueId());
        plugin.getTeamManager().saveAll();
    }
}
