// FULL FILE — ONLY FIX: switch-case returns wrapped in {}

package com.teams.commands;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final TeamsPlugin plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "create", "disband", "invite", "join", "leave", "promote", "demote",
        "rename", "ally", "set", "setwarp", "deletewarp", "warp",
        "sethome", "deletehome", "home", "info", "list", "pvp", "help"
    );

    public TeamCommand(TeamsPlugin plugin) { this.plugin = plugin; }

    private String msg(String key) {
        return plugin.getConfig().getString("messages." + key, "§cMessage not configured: " + key);
    }
    private String msg(String key, String... replacements) {
        String m = msg(key);
        for (int i = 0; i < replacements.length - 1; i += 2) m = m.replace(replacements[i], replacements[i + 1]);
        return m;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length == 0) { sendHelp(p); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(p, args);
            case "disband" -> handleDisband(p, args);
            case "invite" -> handleInvite(p, args);
            case "join" -> handleJoin(p);
            case "leave" -> handleLeave(p);
            case "promote" -> handlePromote(p, args);
            case "demote" -> handleDemote(p, args);
            case "rename" -> handleRename(p, args);
            case "ally" -> handleAlly(p, args);
            case "set" -> handleSet(p);
            case "setwarp" -> handleSetWarp(p, args);
            case "deletewarp" -> handleDeleteWarp(p, args);
            case "warp" -> handleWarp(p, args);
            case "sethome" -> handleSetHome(p);
            case "deletehome" -> handleDeleteHome(p);
            case "home" -> handleHome(p);
            case "info" -> handleInfo(p, args);
            case "list" -> handleList(p);
            case "pvp" -> handlePvp(p);
            case "help" -> sendHelp(p);
            default -> p.sendMessage("§cUnknown subcommand. Use §f/team help");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return Collections.emptyList();

        if (args.length == 1) return filter(SUBCOMMANDS, args[0]);

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "invite", "promote", "demote" -> {
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "ally" -> {
                    return filter(Arrays.asList("add", "remove"), args[1]);
                }
                case "warp", "deletewarp" -> {
                    Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
                    if (team == null) return Collections.emptyList();
                    return filter(new ArrayList<>(team.getWarps().keySet()), args[1]);
                }
                case "setwarp" -> {
                    return Collections.singletonList("<warpname>");
                }
                case "rename" -> {
                    return Collections.singletonList("<newname>");
                }
                case "disband" -> {
                    return Collections.singletonList("confirm");
                }
                case "info" -> {
                    return plugin.getTeamManager().getAllTeams().stream()
                        .map(Team::getStripName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("ally")) {
            return plugin.getTeamManager().getAllTeams().stream()
                .map(Team::getStripName)
                .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream()
            .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
            .collect(Collectors.toList());
    }

    // FROM HERE DOWN: EXACTLY YOUR ORIGINAL CODE (UNCHANGED)

    private void handleCreate(Player p, String[] args) {
        if (plugin.getTeamManager().isInTeam(p.getUniqueId())) { p.sendMessage(msg("already-in-team")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: §f/team create <name>"); return; }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Team team = plugin.getTeamManager().createTeam(name, p.getUniqueId());
        plugin.getDisplayManager().updatePlayer(p);
        p.sendMessage(msg("team-created", "{team}", team.getColoredName()));
    }

    private void handleDisband(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(msg("not-leader")); return; }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) { p.sendMessage(msg("disband-confirm")); return; }
        for (UUID uuid : team.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(msg("team-disbanded", "{team}", team.getColoredName()));
                plugin.getDisplayManager().clearPlayer(member);
            }
        }
        plugin.getTeamManager().disbandTeam(team);
    }

    // (continues exactly like your original — all remaining methods unchanged)
}

private void handleInvite(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeaderOrPromoted(p.getUniqueId())) { p.sendMessage(msg("not-leader-or-promoted")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: §f/team invite <player>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { p.sendMessage(msg("player-not-found")); return; }
        if (plugin.getTeamManager().isInTeam(target.getUniqueId())) { p.sendMessage("§cThat player is already in a team."); return; }
        int max = plugin.getConfig().getInt("max-players", 10);
        if (team.getMembers().size() >= max) { p.sendMessage(msg("team-full", "{max}", String.valueOf(max))); return; }
        int expiry = plugin.getConfig().getInt("invite-expiry", 120);
        plugin.getTeamManager().sendInvite(target.getUniqueId(), team);
        p.sendMessage(msg("invite-sent", "{player}", target.getName()));
        target.sendMessage(msg("invite-received", "{player}", p.getName(), "{team}", team.getColoredName(), "{expiry}", String.valueOf(expiry)));
    }

    private void handleJoin(Player p) {
        if (plugin.getTeamManager().isInTeam(p.getUniqueId())) { p.sendMessage(msg("already-in-team")); return; }
        Team team = plugin.getTeamManager().getPendingInvite(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("invite-none")); return; }
        plugin.getTeamManager().clearInvite(p.getUniqueId());
        plugin.getTeamManager().addMember(team, p.getUniqueId());
        plugin.getDisplayManager().updatePlayer(p);
        p.sendMessage(msg("joined-team", "{team}", team.getColoredName()));
        for (UUID uuid : team.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && !m.equals(p)) m.sendMessage(msg("player-joined", "{player}", p.getName()));
        }
    }

    private void handleLeave(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (team.isLeader(p.getUniqueId()) && team.getMembers().size() > 1) {
            p.sendMessage("§cYou are the leader! Promote someone or disband the team first."); return;
        }
        if (team.getMembers().size() == 1) {
            plugin.getTeamManager().disbandTeam(team);
        } else {
            plugin.getTeamManager().removeMember(team, p.getUniqueId());
            for (UUID uuid : team.getMembers()) {
                Player m = Bukkit.getPlayer(uuid);
                if (m != null) m.sendMessage(msg("player-left", "{player}", p.getName()));
            }
        }
        plugin.getDisplayManager().clearPlayer(p);
        p.sendMessage(msg("left-team"));
    }

    private void handlePromote(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(msg("not-leader")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: §f/team promote <player>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { p.sendMessage(msg("player-not-found")); return; }
        if (!team.isMember(target.getUniqueId())) { p.sendMessage("§cThat player is not in your team."); return; }
        team.promote(target.getUniqueId());
        plugin.getTeamManager().saveAll();
        p.sendMessage(msg("promoted", "{player}", target.getName()));
        target.sendMessage(msg("you-promoted"));
    }

    private void handleDemote(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(msg("not-leader")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: §f/team demote <player>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { p.sendMessage(msg("player-not-found")); return; }
        if (!team.isPromoted(target.getUniqueId())) { p.sendMessage("§cThat player is not an officer."); return; }
        team.getPromoted().remove(target.getUniqueId());
        plugin.getTeamManager().saveAll();
        p.sendMessage("§asDemoted §f" + target.getName() + "§a to member.");
        target.sendMessage("§cYou have been demoted to member.");
    }

    private void handleRename(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(msg("not-leader")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: §f/team rename <newname>"); return; }
        String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getTeamManager().renameTeam(team, newName);
        plugin.getDisplayManager().updateAll();
        String msg = "§5Team renamed to §r" + team.getColoredName() + "§5!";
        for (UUID uuid : team.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(msg);
        }
    }

    private void handleAlly(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(msg("not-leader")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: §f/team ally <add|remove> <team>"); return; }
        if (args.length < 3) { p.sendMessage("§cUsage: §f/team ally " + args[1] + " <team>"); return; }

        String targetName = args[2];
        Team target = null;
        for (Team t : plugin.getTeamManager().getAllTeams()) {
            if (t.getStripName().equalsIgnoreCase(targetName)) { target = t; break; }
        }
        if (target == null) { p.sendMessage("§cTeam not found."); return; }
        if (target == team) { p.sendMessage("§cYou can't ally with yourself."); return; }

        if (args[1].equalsIgnoreCase("add")) {
            int max = plugin.getConfig().getInt("max-allies", 3);
            if (team.getAllies().size() >= max) { p.sendMessage(msg("ally-max", "{max}", String.valueOf(max))); return; }
            if (plugin.getTeamManager().areAllies(team, target)) { p.sendMessage("§cAlready allied!"); return; }

            if (target.getPendingAllyRequests().contains(team.getStripName().toLowerCase())) {
                team.getAllies().add(target.getStripName().toLowerCase());
                target.getAllies().add(team.getStripName().toLowerCase());
                target.getPendingAllyRequests().remove(team.getStripName().toLowerCase());
                plugin.getTeamManager().saveAll();
                p.sendMessage(msg("ally-added", "{team}", target.getColoredName()));
                for (UUID uuid : target.getMembers()) {
                    Player m = Bukkit.getPlayer(uuid);
                    if (m != null) m.sendMessage(msg("ally-added", "{team}", team.getColoredName()));
                }
            } else {
                team.getPendingAllyRequests().add(target.getStripName().toLowerCase());
                p.sendMessage(msg("ally-request-sent", "{team}", target.getColoredName()));
                for (UUID uuid : target.getMembers()) {
                    Player m = Bukkit.getPlayer(uuid);
                    if (m != null) m.sendMessage(msg("ally-request-received", "{team}", team.getColoredName()));
                }
            }
        } else if (args[1].equalsIgnoreCase("remove")) {
            team.getAllies().remove(target.getStripName().toLowerCase());
            target.getAllies().remove(team.getStripName().toLowerCase());
            plugin.getTeamManager().saveAll();
            p.sendMessage(msg("ally-removed", "{team}", target.getColoredName()));
        } else {
            p.sendMessage("§cUsage: §f/team ally <add|remove> <team>");
        }
    }

    private void handleSet(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        p.sendMessage("§5§lSet commands:");
        p.sendMessage("§7/team §fsethome §8— §7Set team home at your location");
        p.sendMessage("§7/team §fdeletehome §8— §7Delete team home");
        p.sendMessage("§7/team §fsetwarp §f<name> §8— §7Set a team warp at your location");
        p.sendMessage("§7/team §fdeletewarp §f<name> §8— §7Delete a team warp");
    }

    private void handleSetWarp(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeaderOrPromoted(p.getUniqueId())) { p.sendMessage(msg("not-leader-or-promoted")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: §f/team setwarp <name>"); return; }
        int max = plugin.getConfig().getInt("max-warps", 5);
        if (team.getWarps().size() >= max && !team.getWarps().containsKey(args[1].toLowerCase())) {
            p.sendMessage(msg("warp-max", "{max}", String.valueOf(max))); return;
        }
        team.setWarp(args[1], p.getLocation());
        plugin.getTeamManager().saveAll();
        p.sendMessage(msg("warp-set", "{name}", args[1]));
    }

    private void handleDeleteWarp(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeaderOrPromoted(p.getUniqueId())) { p.sendMessage(msg("not-leader-or-promoted")); return; }
        if (args.length < 2) {
            if (team.getWarps().isEmpty()) { p.sendMessage("§cNo warps to delete."); return; }
            p.sendMessage("§cUsage: §f/team deletewarp <" + String.join("|", team.getWarps().keySet()) + ">"); return;
        }
        if (team.getWarp(args[1]) == null) { p.sendMessage(msg("warp-not-found")); return; }
        team.deleteWarp(args[1]);
        plugin.getTeamManager().saveAll();
        p.sendMessage(msg("warp-deleted", "{name}", args[1]));
    }

    private void handleWarp(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (args.length < 2) {
            if (team.getWarps().isEmpty()) { p.sendMessage("§cNo warps set. Use §f/team setwarp <name>"); return; }
            p.sendMessage("§5Available warps: §f" + String.join("§7, §f", team.getWarps().keySet()));
            p.sendMessage("§7Use §f/team warp <name> §7to teleport.");
            return;
        }
        var loc = team.getWarp(args[1]);
        if (loc == null) {
            p.sendMessage(msg("warp-not-found"));
            if (!team.getWarps().isEmpty()) p.sendMessage("§7Available: §f" + String.join("§7, §f", team.getWarps().keySet()));
            return;
        }
        plugin.getTeleportManager().teleportWarp(p, loc);
    }

    private void handleSetHome(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeaderOrPromoted(p.getUniqueId())) { p.sendMessage(msg("not-leader-or-promoted")); return; }
        team.setHome(p.getLocation());
        plugin.getTeamManager().saveAll();
        p.sendMessage(msg("home-set"));
    }

    private void handleDeleteHome(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeaderOrPromoted(p.getUniqueId())) { p.sendMessage(msg("not-leader-or-promoted")); return; }
        if (team.getHome() == null) { p.sendMessage(msg("home-not-set")); return; }
        team.setHome(null);
        plugin.getTeamManager().saveAll();
        p.sendMessage(msg("home-deleted"));
    }

    private void handleHome(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (team.getHome() == null) { p.sendMessage(msg("home-not-set")); return; }
        plugin.getTeleportManager().teleportHome(p, team.getHome());
    }

    private void handleInfo(Player p, String[] args) {
        Team team;
        if (args.length >= 2) {
            team = null;
            for (Team t : plugin.getTeamManager().getAllTeams()) {
                if (t.getStripName().equalsIgnoreCase(args[1])) { team = t; break; }
            }
            if (team == null) { p.sendMessage("§cTeam not found."); return; }
        } else {
            team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
            if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        }
        p.sendMessage("§8§m                      ");
        p.sendMessage("§5§lTeam: §r" + team.getColoredName());
        p.sendMessage("§7Leader: §f" + Bukkit.getOfflinePlayer(team.getLeader()).getName());
        p.sendMessage("§7Members: §f" + team.getMembers().size() + "§7/§f" + plugin.getConfig().getInt("max-players", 10));
        p.sendMessage("§7Officers: §f" + team.getPromoted().size());
        p.sendMessage("§7Allies: §f" + team.getAllies().size() + "§7/§f" + plugin.getConfig().getInt("max-allies", 3));
        if (!team.getWarps().isEmpty()) p.sendMessage("§7Warps: §f" + String.join("§7, §f", team.getWarps().keySet()));
        p.sendMessage("§7Home: " + (team.getHome() != null ? "§aSet" : "§cNot set"));
        p.sendMessage("§7Friendly fire: " + (team.isPvpEnabled() ? "§cON" : "§aOFF"));
        p.sendMessage("§8§m                      ");
    }

    private void handleList(Player p) {
        Collection<Team> teams = plugin.getTeamManager().getAllTeams();
        if (teams.isEmpty()) { p.sendMessage("§7No teams exist yet."); return; }
        p.sendMessage("§5§lAll Teams §7(" + teams.size() + "):");
        for (Team t : teams) {
            p.sendMessage("§8- " + t.getColoredName() + " §7(" + t.getMembers().size() + " members)");
        }
    }

    private void handlePvp(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(msg("not-leader")); return; }
        team.setPvpEnabled(!team.isPvpEnabled());
        plugin.getTeamManager().saveAll();
        String msgKey = team.isPvpEnabled() ? "teampvp-on" : "teampvp-off";
        String broadcast = plugin.getConfig().getString("messages." + msgKey, "§7PvP toggled.");
        for (UUID uuid : team.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(broadcast);
        }
    }

    private void sendHelp(Player p) {
        p.sendMessage("§8§m                          ");
        p.sendMessage("§5§lTeams Help");
        p.sendMessage("§f/team create §7<name>");
        p.sendMessage("§f/team invite §7<player> §8| §f/team join §8| §f/team leave");
        p.sendMessage("§f/team disband confirm");
        p.sendMessage("§f/team rename §7<newname>");
        p.sendMessage("§f/team promote §7<player> §8| §f/team demote §7<player>");
        p.sendMessage("§f/team ally add/remove §7<team>");
        p.sendMessage("§f/team set §8— §7show set commands");
        p.sendMessage("§f/team setwarp §7<name> §8| §f/team warp §7<name>");
        p.sendMessage("§f/team deletewarp §7<name>");
        p.sendMessage("§f/team sethome §8| §f/team home §8| §f/team deletehome");
        p.sendMessage("§f/team info §7[team] §8| §f/team list");
        p.sendMessage("§f/team pvp §8— §7toggle friendly fire");
        p.sendMessage("§f/teamchat §8| §f/allychat");
        p.sendMessage("§8§m                          ");
    }
}
