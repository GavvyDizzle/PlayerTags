package com.github.gavvydizzle.playertags.tag;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AnimatedTag extends Tag {

    private final ArrayList<String> tags;
    private final int updateIntervalMillis, updateCycleMillis;

    public AnimatedTag(@NotNull String id, @NotNull ItemStack unlockedItem, @NotNull ItemStack lockedItem, boolean hidden, @NotNull ArrayList<String> tags, int updateIntervalMillis) {
        super(id, unlockedItem, lockedItem, hidden);
        this.tags = tags;
        this.updateIntervalMillis = Math.max(updateIntervalMillis, 100); // Minimum interval of 100ms
        this.updateCycleMillis = tags.size() * this.updateIntervalMillis;
    }

    @Override
    @NotNull
    public String getValue() {
        return tags.get((int) ((System.currentTimeMillis() % updateCycleMillis) / updateIntervalMillis));
    }

    @Override
    public String toString() {
        return "StaticTag:" + getId();
    }
}