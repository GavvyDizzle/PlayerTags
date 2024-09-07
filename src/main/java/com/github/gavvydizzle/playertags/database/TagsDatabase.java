package com.github.gavvydizzle.playertags.database;

import com.github.gavvydizzle.playertags.player.LoadedPlayer;
import com.github.mittenmc.serverutils.UUIDConverter;
import com.github.mittenmc.serverutils.database.Database;
import com.github.mittenmc.serverutils.database.DatabaseConnectionPool;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class TagsDatabase extends Database {

    private final static String tableName = "selected_tags";
    private final static String LOAD_PLAYER = "SELECT tagID FROM " + tableName + " WHERE uuid = ?";
    private final static String SAVE_PLAYER = "INSERT INTO " + tableName + "(uuid, tagID) VALUES(?,?) ON DUPLICATE KEY UPDATE tagID=VALUES(tagID)";
    private final static String DELETE_PLAYER = "DELETE FROM " + tableName + " WHERE uuid = ?";

    public TagsDatabase(DatabaseConnectionPool pool) {
        super(pool);
    }

    public LoadedPlayer load(OfflinePlayer player) {
        try (Connection connection = pool.getConnection();
             PreparedStatement stmt = connection.prepareStatement(LOAD_PLAYER)) {

            stmt.setBytes(1, UUIDConverter.convert(player.getUniqueId()));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new LoadedPlayer(player, rs.getString("tagID"));
            }
            else {
                return new LoadedPlayer(player, null);
            }
        } catch (SQLException e) {
            logSQLError("Failed to load player data for " + player.getName(), e);
        }

        return null;
    }

    public void save(LoadedPlayer loadedPlayer) {

        String tagID = loadedPlayer.getSelectedTagID();
        String query = tagID != null ? SAVE_PLAYER : DELETE_PLAYER;

        try (Connection connection = pool.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setBytes(1, UUIDConverter.convert(loadedPlayer.getUniqueId()));
            if (tagID != null) {
                stmt.setString(2, tagID);
            }
            stmt.execute();
        } catch (SQLException e) {
            logSQLError("Failed to save player data for " + loadedPlayer.getName(), e);
        }
    }

    public void save(Collection<LoadedPlayer> loadedPlayers) {
        try (Connection connection = pool.getConnection();
             PreparedStatement saveStatement = connection.prepareStatement(SAVE_PLAYER);
             PreparedStatement deleteStatement = connection.prepareStatement(DELETE_PLAYER);) {

            for (LoadedPlayer loadedPlayer : loadedPlayers) {
                String tagID = loadedPlayer.getSelectedTagID();
                if (tagID != null) {
                    saveStatement.setBytes(1, UUIDConverter.convert(loadedPlayer.getUniqueId()));
                    saveStatement.setString(2, tagID);
                    saveStatement.addBatch();
                }
                else {
                    deleteStatement.setBytes(1, UUIDConverter.convert(loadedPlayer.getUniqueId()));
                    deleteStatement.addBatch();
                }
            }

            saveStatement.executeBatch();
            deleteStatement.executeBatch();
        } catch (SQLException e) {
            logSQLError("Failed to save player data", e);
        }
    }
}
