package com.github.gavvydizzle.playertags.tag;

import com.github.mittenmc.serverutils.gui.pages.ItemGenerator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Tag implements Comparable<Tag>, ItemGenerator {

    private static final String PERMISSION_PREFIX = "playertags.tag.";

    private final String id, permission;
    private final ItemStack lockedItem, unlockedItem;
    private final boolean hidden;

    public Tag(@NotNull String id, @NotNull ItemStack unlockedItem, @NotNull ItemStack lockedItem, boolean hidden) {
        this.id = id;
        this.permission = PERMISSION_PREFIX + id;
        this.unlockedItem = unlockedItem;
        this.lockedItem = lockedItem;
        this.hidden = hidden;
    }

    /**
     * Implementations may change this value over time so this value should not be saved.
     * @return That String to display
     */
    public abstract String getValue();

    @NotNull
    public String getId() {
        return id;
    }

    /**
     * @param player The player
     * @return If the player has permission to use this tag
     */
    public boolean hasPermission(Player player) {
        return player.hasPermission(permission);
    }

    @Override
    public @NotNull ItemStack getMenuItem(Player player) {
        return hasPermission(player) ? unlockedItem : lockedItem;
    }

    @Override
    public @Nullable ItemStack getPlayerItem(Player player) {
        return null;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public int compareTo(@NotNull Tag o) {
        return id.compareTo(o.id);
    }

    @Override
    public String toString() {
        return "Tag:" + id;
    }
}
