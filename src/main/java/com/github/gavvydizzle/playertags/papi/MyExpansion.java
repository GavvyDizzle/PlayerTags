package com.github.gavvydizzle.playertags.papi;

import com.github.gavvydizzle.playertags.tag.TagsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class MyExpansion extends PlaceholderExpansion {

    private final TagsManager tagsManager;

    public MyExpansion(TagsManager tagsManager) {
        this.tagsManager = tagsManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playertags";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GavvyDizzle";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player.getPlayer() == null) return "Player must be online";

        String tagString = tagsManager.getSelectedTagValue(player.getPlayer());

        if (params.equalsIgnoreCase("tag")) {
            return tagString == null ? "" : tagString;
        }
        else if (params.equalsIgnoreCase("hasTag")) {
            return tagString == null ? "false" : "true";
        }

        return null;
    }
}
