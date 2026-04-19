package com.teams.managers;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

public class DisplayManager {

    private final TeamsPlugin plugin;

    public DisplayManager(TeamsPlugin plugin) { this.plugin = plugin; }

    public void updatePlayer(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        String prefix = team != null ? "§8[" + team.getColoredName() + "§8] " : "";

        // Update nametag for all online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) continue;
            String teamName = "tm_" + player.getName().substring(0, Math.min(10, player.getName().length()));
            org.bukkit.scoreboard.Team st = board.getTeam(teamName);
            if (st == null) st = board.registerNewTeam(teamName);
            st.setPrefix(prefix);
            st.addEntry(player.getName());
        }

        // TAB display
        String tabName = team != null ? "§8[" + team.getColoredName() + "§8] §f" + player.getName() : "§f" + player.getName();
        player.setPlayerListName(tabName);
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
    }

    public void clearPlayer(Player player) {
        player.setPlayerListName(player.getName());
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            if (board == null) continue;
            String teamName = "tm_" + player.getName().substring(0, Math.min(10, player.getName().length()));
            org.bukkit.scoreboard.Team st = board.getTeam(teamName);
            if (st != null) {
                st.removeEntry(player.getName());
            }
        }
    }
}
