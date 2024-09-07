package com.github.gavvydizzle.playertags.tag;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class StaticTag extends Tag {

    private final String tag;

    public StaticTag(@NotNull String id, @NotNull ItemStack unlockedItem, @NotNull ItemStack lockedItem, boolean hidden, @NotNull String tag) {
        super(id, unlockedItem, lockedItem, hidden);
        this.tag = tag;
    }

    @Override
    @NotNull
    public String getValue() {
        return tag;
    }

    @Override
    public String toString() {
        return "StaticTag:" + getId();
    }
}
