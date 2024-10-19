package com.github.gavvydizzle.playertags;

import com.github.gavvydizzle.playertags.commands.AdminCommandManager;
import com.github.gavvydizzle.playertags.commands.PlayerCommandManager;
import com.github.gavvydizzle.playertags.database.TagsDatabase;
import com.github.gavvydizzle.playertags.gui.InventoryManager;
import com.github.gavvydizzle.playertags.papi.MyExpansion;
import com.github.gavvydizzle.playertags.player.PlayerManager;
import com.github.gavvydizzle.playertags.tag.TagsManager;
import com.github.gavvydizzle.playertags.utils.Messages;
import com.github.gavvydizzle.playertags.utils.Sounds;
import com.github.mittenmc.serverutils.ConfigManager;
import com.github.mittenmc.serverutils.ServerUtilsPlugin;
import com.github.mittenmc.serverutils.database.DatabaseConnectionPool;
import lombok.Getter;

import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public final class PlayerTags extends ServerUtilsPlugin {

    @Getter private static PlayerTags instance;
    @Getter private ConfigManager configManager;
    @Getter private PlayerManager playerManager;
    @Getter private TagsManager tagsManager;
    @Getter private InventoryManager inventoryManager;

    private DatabaseConnectionPool databaseConnectionPool;
    private TagsDatabase database;

    @Override
    public void onLoad() {
        databaseConnectionPool = new DatabaseConnectionPool(this);
        if (!databaseConnectionPool.testConnection()) {
            getLogger().severe("Unable to connect to database. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        database = new TagsDatabase(databaseConnectionPool);
    }

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);
        configManager.registerFiles(Set.of("messages", "menus", "sounds", "tags"));

        playerManager = new PlayerManager(this, database);
        inventoryManager = new InventoryManager(this, playerManager);
        tagsManager = new TagsManager(this, database, playerManager, inventoryManager);

        getServer().getPluginManager().registerEvents(tagsManager, this);

        new AdminCommandManager(getCommand("tagsadmin"), playerManager, inventoryManager, tagsManager);

        try {
            Objects.requireNonNull(getCommand("tags")).setExecutor(new PlayerCommandManager(inventoryManager));
        } catch (NullPointerException e) {
            getLogger().severe("The player command name was changed in the plugin.yml file. Please make it \"tags\" and restart the server. You can change the aliases but NOT the command name.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Messages.reload();
        Sounds.reload();

        configManager.saveAll();

        try {
            new MyExpansion(tagsManager).register();
        }
        catch (Exception e) {
            getLogger().warning("PlaceholderAPI is not detected. Placeholders will not be enabled!");
        }
    }

    @Override
    public void onDisable() {
        if (inventoryManager != null) {
            inventoryManager.closeAllMenus();
        }

        if (databaseConnectionPool != null) {
            if (playerManager != null) {
                try {
                    playerManager.saveAllPlayerData();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to save player data on server shutdown. Data since the last auto save will be lost for online players.", e);
                }
            }

            databaseConnectionPool.close();
        }
    }
}
