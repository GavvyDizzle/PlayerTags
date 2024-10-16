package com.github.gavvydizzle.playertags.player;

import com.github.gavvydizzle.playertags.tag.Tag;
import com.github.mittenmc.serverutils.player.profile.PlayerProfile;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class LoadedPlayer extends PlayerProfile {

    @Nullable private String selectedTagID;
    @Nullable private Tag selectedTag;

    public LoadedPlayer(@NotNull Player player, @Nullable String selectedTagID) {
        super(player);
        this.selectedTagID = selectedTagID;
    }

    public LoadedPlayer(@NotNull OfflinePlayer offlinePlayer, @Nullable String selectedTagID) {
        super(offlinePlayer);
        this.selectedTagID = selectedTagID;
    }

    public void selectTag(@Nullable Tag tag) {
        selectedTagID = tag == null ? null : tag.getId();
        selectedTag = tag;
    }

    public void selectTag(@Nullable Tag tag, boolean updateTagID) {
        if (updateTagID) selectedTagID = tag == null ? null : tag.getId();
        selectedTag = tag;
    }

    public void deselectTag() {
        selectedTagID = null;
        selectedTag = null;
    }

    public boolean hasTagSelected() {
        return selectedTagID != null;
    }

    public boolean isSelectedTagInvalid() {
        return selectedTagID != null && selectedTag == null;
    }
}
