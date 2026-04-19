package com.teams.commands;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AllyChatCommand implements CommandExecutor {
    private final TeamsPlugin plugin;
    public AllyChatCommand(TeamsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(plugin.getConfig().getString("messages.not-in-team")); return true; }
        if (team.getAllies().isEmpty()) { p.sendMessage("§cYour team has no allies."); return true; }
        plugin.getChatManager().toggleAllyChat(p);
        return true;
    }
}
