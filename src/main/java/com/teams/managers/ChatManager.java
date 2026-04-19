package com.teams.managers;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatManager {

    private final TeamsPlugin plugin;
    private final Set<UUID> teamChatToggled = new HashSet<>();
    private final Set<UUID> allyChatToggled = new HashSet<>();

    public ChatManager(TeamsPlugin plugin) { this.plugin = plugin; }

    public boolean isTeamChat(UUID uuid) { return teamChatToggled.contains(uuid); }
    public boolean isAllyChat(UUID uuid) { return allyChatToggled.contains(uuid); }

    public void toggleTeamChat(Player player) {
        if (teamChatToggled.contains(player.getUniqueId())) {
            teamChatToggled.remove(player.getUniqueId());
            player.sendMessage(plugin.getConfig().getString("messages.teamchat-off", "§7Team chat disabled."));
        } else {
            teamChatToggled.add(player.getUniqueId());
            allyChatToggled.remove(player.getUniqueId()); // can't have both
            player.sendMessage(plugin.getConfig().getString("messages.teamchat-on", "§aTeam chat enabled."));
        }
    }

    public void toggleAllyChat(Player player) {
        if (allyChatToggled.contains(player.getUniqueId())) {
            allyChatToggled.remove(player.getUniqueId());
            player.sendMessage(plugin.getConfig().getString("messages.allychat-off", "§7Ally chat disabled."));
        } else {
            allyChatToggled.add(player.getUniqueId());
            teamChatToggled.remove(player.getUniqueId()); // can't have both
            player.sendMessage(plugin.getConfig().getString("messages.allychat-on", "§aAlly chat enabled."));
        }
    }

    public void sendTeamChat(Player player, Team team, String message) {
        String format = plugin.getConfig().getString("messages.teamchat-format", "§8[§5TEAM§8] {team} §7{player}§f: {message}")
            .replace("{team}", team.getColoredName())
            .replace("{player}", player.getName())
            .replace("{message}", message);
        for (UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(format);
        }
    }

    public void sendAllyChat(Player player, Team team, String message) {
        String format = plugin.getConfig().getString("messages.allychat-format", "§8[§6ALLY§8] {team} §7{player}§f: {message}")
            .replace("{team}", team.getColoredName())
            .replace("{player}", player.getName())
            .replace("{message}", message);

        // Send to own team
        for (UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(format);
        }
        // Send to all ally teams
        for (String allyName : team.getAllies()) {
            Team ally = plugin.getTeamManager().getTeam(allyName);
            if (ally == null) continue;
            for (UUID uuid : ally.getMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(format);
            }
        }
    }

    public void clearPlayer(UUID uuid) {
        teamChatToggled.remove(uuid);
        allyChatToggled.remove(uuid);
    }
}
