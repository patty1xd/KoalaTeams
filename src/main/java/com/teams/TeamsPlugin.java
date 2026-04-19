package com.teams;

import com.teams.commands.*;
import com.teams.listeners.TeamListeners;
import com.teams.managers.*;
import com.teams.placeholders.TeamsExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TeamsPlugin extends JavaPlugin {

    private static TeamsPlugin instance;
    private TeamManager teamManager;
    private TeleportManager teleportManager;
    private ChatManager chatManager;
    private DisplayManager displayManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        teamManager = new TeamManager(this);
        teleportManager = new TeleportManager(this);
        chatManager = new ChatManager(this);
        displayManager = new DisplayManager(this);

        getServer().getPluginManager().registerEvents(new TeamListeners(this), this);

        getCommand("team").setExecutor(new TeamCommand(this));
        getCommand("teamchat").setExecutor(new TeamChatCommand(this));
        getCommand("allychat").setExecutor(new AllyChatCommand(this));
        getCommand("teampvp").setExecutor(new TeamPvpCommand(this));

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TeamsExpansion(this).register();
            getLogger().info("PlaceholderAPI found! Placeholders registered.");
        }

        // Update displays for all online players (reload support)
        Bukkit.getScheduler().runTaskLater(this, () -> displayManager.updateAll(), 20L);

        getLogger().info("TeamsPlugin enabled!");
    }

    @Override
    public void onDisable() {
        if (teamManager != null) teamManager.saveAll();
        getLogger().info("TeamsPlugin disabled. Data saved.");
    }

    public static TeamsPlugin getInstance() { return instance; }
    public TeamManager getTeamManager() { return teamManager; }
    public TeleportManager getTeleportManager() { return teleportManager; }
    public ChatManager getChatManager() { return chatManager; }
    public DisplayManager getDisplayManager() { return displayManager; }
}
