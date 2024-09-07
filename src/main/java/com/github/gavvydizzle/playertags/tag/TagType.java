package com.github.gavvydizzle.playertags.tag;

import org.jetbrains.annotations.Nullable;

public enum TagType {
    ANIMATED,
    STATIC;

    @Nullable
    public static TagType get(String str) {
        for (TagType type : TagType.values()) {
            if (str.equalsIgnoreCase(type.name())) {
                return type;
            }
        }
        return null;
    }
}
