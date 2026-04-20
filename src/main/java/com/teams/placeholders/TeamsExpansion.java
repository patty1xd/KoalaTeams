package com.teams.placeholders;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamsExpansion extends PlaceholderExpansion {

    private final TeamsPlugin plugin;

    public TeamsExpansion(TeamsPlugin plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "teams"; }
    @Override public @NotNull String getAuthor() { return "Teams"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        return switch (identifier) {
            case "name" -> team != null ? team.getColoredName() : "";
            case "name_strip" -> team != null ? team.getStripName() : "None";
            case "role" -> {
                if (team == null) yield "§7None";
                if (team.isLeader(player.getUniqueId())) yield "§6Leader";
                if (team.isPromoted(player.getUniqueId())) yield "§eOfficer";
                yield "§7Member";
            }
            case "size" -> team != null ? String.valueOf(team.getMembers().size()) : "0";
            case "allies" -> team != null ? String.valueOf(team.getAllies().size()) : "0";
            case "prefix" -> team != null ? "§8[" + team.getColoredName() + "§8]" : "";
            default -> null;
        };
    }
}
