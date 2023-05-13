package com.github.gavvydizzle.playertags.tag;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class Tag implements Comparable<Tag> {

    private final String id, permission;
    private final ItemStack lockedItem, unlockedItem;

    public Tag(@NotNull String id, @NotNull ItemStack unlockedItem, @NotNull ItemStack lockedItem) {
        this.id = id;
        this.permission = "playertags.tag." + id;
        this.unlockedItem = unlockedItem;
        this.lockedItem = lockedItem;
    }

    /**
     * Implementations may change this value over time so this value should not be saved.
     * @return That String to display
     */
    public abstract String getTag();

    @NotNull
    public String getId() {
        return id;
    }

    public boolean hasPermission(Player player) {
        return player.hasPermission(permission);
    }

    /**
     * Gets the correct menu item for this player depending on their permissions
     * @param player The player
     * @return The locked or unlocked ItemStack depending on permissions
     */
    @NotNull
    public ItemStack getMenuItem(Player player) {
        return hasPermission(player) ? unlockedItem : lockedItem;
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
