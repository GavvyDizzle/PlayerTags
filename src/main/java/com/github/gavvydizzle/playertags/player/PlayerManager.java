package com.github.gavvydizzle.playertags.player;

import com.github.gavvydizzle.playertags.PlayerTags;
import com.github.gavvydizzle.playertags.database.TagsDatabase;
import com.github.mittenmc.serverutils.player.PlayerDataContainer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PlayerManager extends PlayerDataContainer<LoadedPlayer> {

    private final TagsDatabase tagsDatabase;

    public PlayerManager(PlayerTags instance, TagsDatabase tagsDatabase) {
        super(instance);
        this.tagsDatabase = tagsDatabase;
    }

    @Override
    public @Nullable LoadedPlayer loadPlayerData(Player player) {
        return tagsDatabase.load(player);
    }

    @Override
    public @Nullable LoadedPlayer loadOfflinePlayerData(OfflinePlayer offlinePlayer) {
        return tagsDatabase.load(offlinePlayer);
    }

    @Override
    public void savePlayerData(LoadedPlayer loadedPlayer) {
        tagsDatabase.save(loadedPlayer);
    }

    @Override
    public void saveAllPlayerData() {
        tagsDatabase.save(super.getAllPlayerData());
    }
}
