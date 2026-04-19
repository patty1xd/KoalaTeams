package com.teams.commands;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamCommand implements CommandExecutor {

    private final TeamsPlugin plugin;

    public TeamCommand(TeamsPlugin plugin) { this.plugin = plugin; }

    private String msg(String key) { return plugin.getConfig().getString("messages." + key, "§cMessage not configured: " + key); }
    private String msg(String key, String... replacements) {
        String m = msg(key);
        for (int i = 0; i < replacements.length - 1; i += 2) m = m.replace(replacements[i], replacements[i+1]);
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
            case "ally" -> handleAlly(p, args);
            case "setwarp" -> handleSetWarp(p, args);
            case "deletewarp" -> handleDeleteWarp(p, args);
            case "warp" -> handleWarp(p, args);
            case "sethome" -> handleSetHome(p);
            case "deletehome" -> handleDeleteHome(p);
            case "home" -> handleHome(p);
            case "info" -> handleInfo(p, args);
            case "list" -> handleList(p);
            default -> sendHelp(p);
        }
        return true;
    }

    private void handleCreate(Player p, String[] args) {
        if (plugin.getTeamManager().isInTeam(p.getUniqueId())) { p.sendMessage(msg("already-in-team")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: /team create <name>"); return; }
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Team team = plugin.getTeamManager().createTeam(name, p.getUniqueId());
        plugin.getDisplayManager().updatePlayer(p);
        p.sendMessage(msg("team-created", "{team}", team.getColoredName()));
    }

    private void handleDisband(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(msg("not-leader")); return; }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            p.sendMessage(msg("disband-confirm")); return;
        }
        // Notify all members
        for (java.util.UUID uuid : team.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(msg("team-disbanded", "{team}", team.getColoredName()));
                plugin.getDisplayManager().clearPlayer(member);
            }
        }
        plugin.getTeamManager().disbandTeam(team);
    }

    private void handleInvite(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeaderOrPromoted(p.getUniqueId())) { p.sendMessage(msg("not-leader-or-promoted")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: /team invite <player>"); return; }
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
        for (java.util.UUID uuid : team.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && !m.equals(p)) m.sendMessage(msg("player-joined", "{player}", p.getName()));
        }
    }

    private void handleLeave(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (team.isLeader(p.getUniqueId()) && team.getMembers().size() > 1) {
            p.sendMessage("§cYou are the leader! Transfer leadership or disband the team first.");
            return;
        }
        if (team.getMembers().size() == 1) {
            plugin.getTeamManager().disbandTeam(team);
        } else {
            plugin.getTeamManager().removeMember(team, p.getUniqueId());
            for (java.util.UUID uuid : team.getMembers()) {
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
        if (args.length < 2) { p.sendMessage("§cUsage: /team promote <player>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { p.sendMessage(msg("player-not-found")); return; }
        if (!team.isMember(target.getUniqueId())) { p.sendMessage("§cThat player is not in your team."); return; }
        team.promote(target.getUniqueId());
        plugin.getTeamManager().saveAll();
        p.sendMessage(msg("promoted", "{player}", target.getName()));
        target.sendMessage(msg("you-promoted"));
    }

    private void handleAlly(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeader(p.getUniqueId())) { p.sendMessage(msg("not-leader")); return; }
        if (args.length < 3) { p.sendMessage("§cUsage: /team ally <add|remove> <team>"); return; }

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

            // Check if already allied
            if (plugin.getTeamManager().areAllies(team, target)) { p.sendMessage("§cAlready allied!"); return; }

            // Check if target already sent a request
            if (target.getPendingAllyRequests().contains(team.getStripName().toLowerCase())) {
                // Accept
                team.getAllies().add(target.getStripName().toLowerCase());
                target.getAllies().add(team.getStripName().toLowerCase());
                target.getPendingAllyRequests().remove(team.getStripName().toLowerCase());
                plugin.getTeamManager().saveAll();
                p.sendMessage(msg("ally-added", "{team}", target.getColoredName()));
                // Notify target team
                for (java.util.UUID uuid : target.getMembers()) {
                    Player m = Bukkit.getPlayer(uuid);
                    if (m != null) m.sendMessage(msg("ally-added", "{team}", team.getColoredName()));
                }
            } else {
                // Send request
                team.getPendingAllyRequests().add(target.getStripName().toLowerCase());
                p.sendMessage(msg("ally-request-sent", "{team}", target.getColoredName()));
                for (java.util.UUID uuid : target.getMembers()) {
                    Player m = Bukkit.getPlayer(uuid);
                    if (m != null) m.sendMessage(msg("ally-request-received", "{team}", team.getColoredName()));
                }
            }
        } else if (args[1].equalsIgnoreCase("remove")) {
            team.getAllies().remove(target.getStripName().toLowerCase());
            target.getAllies().remove(team.getStripName().toLowerCase());
            plugin.getTeamManager().saveAll();
            p.sendMessage(msg("ally-removed", "{team}", target.getColoredName()));
        }
    }

    private void handleSetWarp(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (!team.isLeaderOrPromoted(p.getUniqueId())) { p.sendMessage(msg("not-leader-or-promoted")); return; }
        if (args.length < 2) { p.sendMessage("§cUsage: /team setwarp <name>"); return; }
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
        if (args.length < 2) { p.sendMessage("§cUsage: /team deletewarp <name>"); return; }
        if (team.getWarp(args[1]) == null) { p.sendMessage(msg("warp-not-found")); return; }
        team.deleteWarp(args[1]);
        plugin.getTeamManager().saveAll();
        p.sendMessage(msg("warp-deleted", "{name}", args[1]));
    }

    private void handleWarp(Player p, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage(msg("not-in-team")); return; }
        if (args.length < 2) {
            if (team.getWarps().isEmpty()) { p.sendMessage("§cNo warps set."); return; }
            p.sendMessage("§6Warps: §f" + String.join("§7, §f", team.getWarps().keySet())); return;
        }
        var loc = team.getWarp(args[1]);
        if (loc == null) { p.sendMessage(msg("warp-not-found")); return; }
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
        p.sendMessage("§8§m                    ");
        p.sendMessage("§5§lTeam: §r" + team.getColoredName());
        p.sendMessage("§7Leader: §f" + Bukkit.getOfflinePlayer(team.getLeader()).getName());
        p.sendMessage("§7Members: §f" + team.getMembers().size() + "§7/§f" + plugin.getConfig().getInt("max-players", 10));
        p.sendMessage("§7Allies: §f" + team.getAllies().size());
        p.sendMessage("§7Warps: §f" + String.join("§7, §f", team.getWarps().keySet()));
        p.sendMessage("§7PvP: " + (team.isPvpEnabled() ? "§cON" : "§aOFF"));
        p.sendMessage("§8§m                    ");
    }

    private void handleList(Player p) {
        p.sendMessage("§5§lTeams:");
        for (Team t : plugin.getTeamManager().getAllTeams()) {
            p.sendMessage("§8- " + t.getColoredName() + " §7(" + t.getMembers().size() + " members)");
        }
    }

    private void sendHelp(Player p) {
        p.sendMessage("§8§m                          ");
        p.sendMessage("§5§lTeams Help");
        p.sendMessage("§7/team create §f<name>");
        p.sendMessage("§7/team invite §f<player>");
        p.sendMessage("§7/team join §f| §7/team leave");
        p.sendMessage("§7/team disband confirm");
        p.sendMessage("§7/team promote §f<player>");
        p.sendMessage("§7/team ally add/remove §f<team>");
        p.sendMessage("§7/team setwarp §f<name> §7| §7/team warp §f<name>");
        p.sendMessage("§7/team deletewarp §f<name>");
        p.sendMessage("§7/team sethome §7| §7/team home");
        p.sendMessage("§7/team deletehome");
        p.sendMessage("§7/team info §f[team] §7| §7/team list");
        p.sendMessage("§7/teamchat §7| §7/allychat §7| §7/teampvp");
        p.sendMessage("§8§m                          ");
    }
}
