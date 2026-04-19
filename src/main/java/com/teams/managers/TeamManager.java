package com.teams.managers;

import com.teams.TeamsPlugin;
import com.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamManager {

    private final TeamsPlugin plugin;
    private final Map<String, Team> teams = new HashMap<>(); // stripName -> Team
    private final Map<UUID, String> playerTeam = new HashMap<>(); // uuid -> stripName
    private final Map<UUID, String> pendingInvites = new HashMap<>(); // uuid -> teamStripName
    private final Map<UUID, Long> inviteTimestamps = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public TeamManager(TeamsPlugin plugin) {
        this.plugin = plugin;
        loadData();
        // Expire invites task
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkInviteExpiry, 20L, 20L);
        // Auto-save
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, 6000L, 6000L);
    }

    public Team getTeam(String stripName) { return teams.get(stripName.toLowerCase()); }
    public Team getPlayerTeam(UUID uuid) {
        String name = playerTeam.get(uuid);
        if (name == null) return null;
        return teams.get(name);
    }

    public boolean isInTeam(UUID uuid) { return playerTeam.containsKey(uuid); }

    public Team createTeam(String name, UUID leader) {
        Team team = new Team(name, leader);
        teams.put(team.getStripName().toLowerCase(), team);
        playerTeam.put(leader, team.getStripName().toLowerCase());
        saveAll();
        return team;
    }

    public void disbandTeam(Team team) {
        for (UUID uuid : team.getMembers()) playerTeam.remove(uuid);
        // Remove from all ally lists
        for (Team t : teams.values()) t.getAllies().remove(team.getStripName().toLowerCase());
        teams.remove(team.getStripName().toLowerCase());
        saveAll();
    }

    public void addMember(Team team, UUID uuid) {
        team.addMember(uuid);
        playerTeam.put(uuid, team.getStripName().toLowerCase());
        saveAll();
    }

    public void removeMember(Team team, UUID uuid) {
        team.removeMember(uuid);
        playerTeam.remove(uuid);
        saveAll();
    }

    public void sendInvite(UUID target, Team team) {
        pendingInvites.put(target, team.getStripName().toLowerCase());
        inviteTimestamps.put(target, System.currentTimeMillis());
    }

    public Team getPendingInvite(UUID uuid) {
        String name = pendingInvites.get(uuid);
        if (name == null) return null;
        return teams.get(name);
    }

    public void clearInvite(UUID uuid) {
        pendingInvites.remove(uuid);
        inviteTimestamps.remove(uuid);
    }

    private void checkInviteExpiry() {
        int expiry = plugin.getConfig().getInt("invite-expiry", 120) * 1000;
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : inviteTimestamps.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() >= expiry) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            Team team = getPendingInvite(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && team != null) {
                String msg = plugin.getConfig().getString("messages.invite-expired", "§cInvite expired.")
                    .replace("{team}", team.getColoredName());
                p.sendMessage(msg);
            }
            clearInvite(uuid);
        }
    }

    public boolean areAllies(Team a, Team b) {
        return a.getAllies().contains(b.getStripName().toLowerCase()) &&
               b.getAllies().contains(a.getStripName().toLowerCase());
    }

    public Collection<Team> getAllTeams() { return teams.values(); }

    public void saveAll() {
        dataConfig.set("teams", null);
        for (Team team : teams.values()) {
            String base = "teams." + team.getStripName().toLowerCase() + ".";
            dataConfig.set(base + "name", team.getName());
            dataConfig.set(base + "leader", team.getLeader().toString());
            dataConfig.set(base + "pvp", team.isPvpEnabled());

            List<String> members = new ArrayList<>();
            for (UUID u : team.getMembers()) members.add(u.toString());
            dataConfig.set(base + "members", members);

            List<String> promoted = new ArrayList<>();
            for (UUID u : team.getPromoted()) promoted.add(u.toString());
            dataConfig.set(base + "promoted", promoted);

            dataConfig.set(base + "allies", new ArrayList<>(team.getAllies()));

            if (team.getHome() != null) {
                dataConfig.set(base + "home", serializeLoc(team.getHome()));
            }

            for (Map.Entry<String, Location> warp : team.getWarps().entrySet()) {
                dataConfig.set(base + "warps." + warp.getKey(), serializeLoc(warp.getValue()));
            }
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.isConfigurationSection("teams")) return;

        for (String key : dataConfig.getConfigurationSection("teams").getKeys(false)) {
            String base = "teams." + key + ".";
            String name = dataConfig.getString(base + "name", key);
            UUID leader = UUID.fromString(dataConfig.getString(base + "leader"));
            Team team = new Team(name, leader);
            team.setPvpEnabled(dataConfig.getBoolean(base + "pvp", false));

            for (String m : dataConfig.getStringList(base + "members"))
                team.getMembers().add(UUID.fromString(m));
            for (String p : dataConfig.getStringList(base + "promoted"))
                team.getPromoted().add(UUID.fromString(p));
            for (String a : dataConfig.getStringList(base + "allies"))
                team.getAllies().add(a);

            if (dataConfig.isConfigurationSection(base + "home"))
                team.setHome(deserializeLoc(dataConfig.getConfigurationSection(base + "home")));

            if (dataConfig.isConfigurationSection(base + "warps")) {
                for (String wName : dataConfig.getConfigurationSection(base + "warps").getKeys(false)) {
                    Location loc = deserializeLoc(dataConfig.getConfigurationSection(base + "warps." + wName));
                    if (loc != null) team.setWarp(wName, loc);
                }
            }

            teams.put(key, team);
            for (UUID m : team.getMembers()) playerTeam.put(m, key);
        }
    }

    private Map<String, Object> serializeLoc(Location loc) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", loc.getWorld().getName());
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        map.put("yaw", loc.getYaw());
        map.put("pitch", loc.getPitch());
        return map;
    }

    private Location deserializeLoc(org.bukkit.configuration.ConfigurationSection sec) {
        if (sec == null) return null;
        var world = Bukkit.getWorld(sec.getString("world", "world"));
        if (world == null) return null;
        return new Location(world, sec.getDouble("x"), sec.getDouble("y"), sec.getDouble("z"),
            (float) sec.getDouble("yaw"), (float) sec.getDouble("pitch"));
    }
}
