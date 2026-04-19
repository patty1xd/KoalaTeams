package com.teams.commands;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamPvpCommand implements CommandExecutor {
    private final TeamsPlugin plugin;
    public TeamPvpCommand(TeamsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(plugin.getConfig().getString("messages.not-in-team", "§cNot in a team.")); return true; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(plugin.getConfig().getString("messages.not-leader", "§cNot the leader.")); return true; }
        team.setPvpEnabled(!team.isPvpEnabled());
        plugin.getTeamManager().saveAll();
        String msg = team.isPvpEnabled()
            ? plugin.getConfig().getString("messages.teampvp-on", "§cFriendly fire ON!")
            : plugin.getConfig().getString("messages.teampvp-off", "§aFriendly fire OFF.");
        // Broadcast to team
        for (java.util.UUID uuid : team.getMembers()) {
            Player m = org.bukkit.Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(msg);
        }
        return true;
    }
}
