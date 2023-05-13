package com.github.gavvydizzle.playertags.storage;

import com.github.gavvydizzle.playertags.tag.Tag;
import com.github.mittenmc.serverutils.UUIDConverter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData extends PluginDataHolder {

    private final static String tableName = "selected_tags";
    private final static String LOAD_PLAYER = "SELECT tagID FROM " + tableName + " WHERE uuid = ?";
    private final static String SAVE_PLAYER = "INSERT INTO " + tableName + "(uuid, tagID) VALUES(?,?) ON DUPLICATE KEY UPDATE tagID=VALUES(tagID)";
    private final static String DELETE_PLAYER = "DELETE FROM " + tableName + " WHERE uuid = ?";


    public PlayerData(Plugin plugin, DataSource source) {
        super(plugin, source);
    }

    /**
     * Retrieves the player's tag from the database
     * @param player The player
     * @return The saved tag or null
     */
    @Nullable
    public String getPlayerTagID(Player player) {
        Connection conn;
        try {
            conn = conn();
        }
        catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return null;
        }

        try {
            PreparedStatement stmt = conn.prepareStatement(LOAD_PLAYER);
            stmt.setBytes(1, UUIDConverter.convert(player.getUniqueId()));
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("tagID");
            }
            else {
                return null; // The player has no selected tag
            }
        } catch (SQLException e) {
            logSQLError("Could not retrieve player's tagID", e);
            return null;
        }
    }

    /**
     * Saves a player's tag to the database
     * @param player The player
     * @param tag The tag
     */
    public void savePlayerTag(Player player, @Nullable Tag tag) {
        Connection conn;
        try {
            conn = conn();
        }
        catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return;
        }

        try {
            PreparedStatement stmt;
            if (tag != null) {
                stmt = conn.prepareStatement(SAVE_PLAYER);
                stmt.setBytes(1, UUIDConverter.convert(player.getUniqueId()));
                stmt.setString(2, tag.getId());
            }
            else {
                stmt = conn.prepareStatement(DELETE_PLAYER);
                stmt.setBytes(1, UUIDConverter.convert(player.getUniqueId()));
            }
            stmt.execute();
        } catch (SQLException e) {
            logSQLError("Could not save player's tagID", e);
        }
    }

    /**
     * Saves a list of player tags to the database
     * @param map The map of UUID and Tag
     */
    public void savePlayerTags(HashMap<UUID, Tag> map) {
        Connection conn;
        try {
            conn = conn();
        }
        catch (SQLException e) {
            logSQLError("Could not connect to the database", e);
            return;
        }

        try {
            PreparedStatement saveStatement = conn.prepareStatement(SAVE_PLAYER);
            PreparedStatement deleteStatement = conn.prepareStatement(DELETE_PLAYER);

            for (Map.Entry<UUID, Tag> entry : map.entrySet()) {
                Tag tag = entry.getValue();
                if (tag != null) {
                    saveStatement.setBytes(1, UUIDConverter.convert(entry.getKey()));
                    saveStatement.setString(2, tag.getId());
                    saveStatement.addBatch();
                }
                else {
                    deleteStatement.setBytes(1, UUIDConverter.convert(entry.getKey()));
                    deleteStatement.addBatch();
                }
            }

            saveStatement.executeBatch();
            deleteStatement.executeBatch();
        } catch (SQLException e) {
            logSQLError("Failed to save unsaved player tags on shutdown", e);
        }
    }
}
