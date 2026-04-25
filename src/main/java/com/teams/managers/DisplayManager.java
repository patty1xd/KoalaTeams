package com.teams.managers;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

public class DisplayManager {

    private final TeamsPlugin plugin;

    public DisplayManager(TeamsPlugin plugin) { this.plugin = plugin; }

    public void updatePlayer(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        // White brackets: [TeamName]
        String prefix = team != null ? "§f[" + team.getColoredName() + "§f] " : "";

        // Update nametag on everyone's scoreboard
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
                board = Bukkit.getScoreboardManager().getNewScoreboard();
                viewer.setScoreboard(board);
            }
            String teamKey = "tm_" + player.getName().substring(0, Math.min(10, player.getName().length()));
            org.bukkit.scoreboard.Team st = board.getTeam(teamKey);
            if (st == null) st = board.registerNewTeam(teamKey);
            st.setPrefix(prefix);
            st.addEntry(player.getName());
        }

        // TAB display name
        String tabName = team != null
            ? "§f[" + team.getColoredName() + "§f] §f" + player.getName()
            : "§f" + player.getName();
        player.setPlayerListName(tabName);
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
    }

    public void clearPlayer(Player player) {
        player.setPlayerListName(player.getName());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null) continue;
            String teamKey = "tm_" + player.getName().substring(0, Math.min(10, player.getName().length()));
            org.bukkit.scoreboard.Team st = board.getTeam(teamKey);
            if (st != null) st.removeEntry(player.getName());
        }
    }
}
