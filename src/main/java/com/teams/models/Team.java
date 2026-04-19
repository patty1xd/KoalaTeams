package com.teams.models;

import org.bukkit.Location;
import java.util.*;

public class Team {

    private String name;
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> promoted = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> pendingAllyRequests = new HashSet<>();
    private final Map<String, Location> warps = new HashMap<>();
    private Location home;
    private boolean pvpEnabled = false;

    public Team(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        members.add(leader);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public Set<UUID> getMembers() { return members; }
    public Set<UUID> getPromoted() { return promoted; }
    public Set<String> getAllies() { return allies; }
    public Set<String> getPendingAllyRequests() { return pendingAllyRequests; }
    public Map<String, Location> getWarps() { return warps; }
    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvp) { this.pvpEnabled = pvp; }

    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public boolean isPromoted(UUID uuid) { return promoted.contains(uuid); }
    public boolean isLeaderOrPromoted(UUID uuid) { return isLeader(uuid) || isPromoted(uuid); }

    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); promoted.remove(uuid); }
    public void promote(UUID uuid) { promoted.add(uuid); }

    public void setWarp(String name, Location loc) { warps.put(name.toLowerCase(), loc); }
    public void deleteWarp(String name) { warps.remove(name.toLowerCase()); }
    public Location getWarp(String name) { return warps.get(name.toLowerCase()); }

    public String getColoredName() { return name.replace("&", "§"); }
    public String getStripName() {
        return org.bukkit.ChatColor.stripColor(name.replace("&", "§"));
    }
}
