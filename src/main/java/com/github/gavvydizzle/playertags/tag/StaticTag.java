package com.github.gavvydizzle.playertags.tag;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class StaticTag extends Tag {

    private final String tag;

    public StaticTag(@NotNull String id, @NotNull ItemStack unlockedItem, @NotNull ItemStack lockedItem, @NotNull String tag) {
        super(id, unlockedItem, lockedItem);
        this.tag = tag;
    }

    @Override
    @NotNull
    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "StaticTag:" + getId();
    }
}
