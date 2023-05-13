package com.github.gavvydizzle.playertags;

import com.github.gavvydizzle.playertags.commands.AdminCommandManager;
import com.github.gavvydizzle.playertags.commands.PlayerCommandManager;
import com.github.gavvydizzle.playertags.papi.MyExpansion;
import com.github.gavvydizzle.playertags.storage.Configuration;
import com.github.gavvydizzle.playertags.storage.DataSourceProvider;
import com.github.gavvydizzle.playertags.storage.DbSetup;
import com.github.gavvydizzle.playertags.storage.PlayerData;
import com.github.gavvydizzle.playertags.tag.TagsManager;
import com.github.gavvydizzle.playertags.utils.Messages;
import com.github.gavvydizzle.playertags.utils.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

public final class PlayerTags extends JavaPlugin {

    private static PlayerTags instance;
    private TagsManager tagsManager;

    private DataSource dataSource;
    private boolean mySQLSuccessful;

    @Override
    public void onLoad() {
        generateDefaultConfig();
        Configuration configuration = new Configuration(this);
        mySQLSuccessful = true;

        try {
            dataSource = DataSourceProvider.initMySQLDataSource(this, configuration.getDatabase());
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Could not establish database connection", e);
            mySQLSuccessful = false;
        }

        try {
            DbSetup.initDb(this, dataSource);
        } catch (SQLException | IOException e) {
            getLogger().log(Level.SEVERE, "Could not init database.", e);
            mySQLSuccessful = false;
        }
    }

    @Override
    public void onEnable() {
        if (!mySQLSuccessful) {
            getLogger().severe("Database connection failed. Disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;
        PlayerData data = new PlayerData(this, dataSource);

        tagsManager = new TagsManager(this, data);
        getServer().getPluginManager().registerEvents(tagsManager, this);

        try {
            new AdminCommandManager(Objects.requireNonNull(getCommand("tagsadmin")), tagsManager);
        } catch (NullPointerException e) {
            getLogger().severe("The admin command name was changed in the plugin.yml file. Please make it \"tagsadmin\" and restart the server. You can change the aliases but NOT the command name.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            Objects.requireNonNull(getCommand("tags")).setExecutor(new PlayerCommandManager(tagsManager));
        } catch (NullPointerException e) {
            getLogger().severe("The player command name was changed in the plugin.yml file. Please make it \"tags\" and restart the server. You can change the aliases but NOT the command name.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Messages.reloadMessages();
        Sounds.reload();

        try {
            new MyExpansion(tagsManager).register();
        }
        catch (Exception e) {
            Bukkit.getLogger().warning("Without PlaceholderAPI you are unable to use placeholders!");
        }
    }

    @Override
    public void onDisable() {
        if (tagsManager != null && tagsManager.getTagsMenu() != null) {
            tagsManager.getTagsMenu().forceUpdateSelectedTags();
        }
    }

    private void generateDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.addDefault("database.host", "TODO");
        config.addDefault("database.port", 3306);
        config.addDefault("database.user", "TODO");
        config.addDefault("database.password", "TODO");
        config.addDefault("database.database", "TODO");
        saveConfig();
    }

    public static PlayerTags getInstance() {
        return instance;
    }
}
