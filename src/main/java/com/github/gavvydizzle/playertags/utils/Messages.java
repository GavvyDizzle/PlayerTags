package com.github.gavvydizzle.playertags.utils;

import com.github.gavvydizzle.playertags.configs.MessagesConfig;
import com.github.mittenmc.serverutils.Colors;
import org.bukkit.configuration.file.FileConfiguration;

public class Messages {

    // Errors
    public static String selectedTag, alreadySelectedTag, deselectedTag, deselectedNullTag, adminSetTag, adminRemoveTag, noTagPermission;

    public static void reloadMessages() {
        FileConfiguration config = MessagesConfig.get();
        config.options().copyDefaults(true);

        config.addDefault("selectedTag", "&aSelected tag {id}");
        config.addDefault("alreadySelectedTag", "&eYou already have this tag selected");
        config.addDefault("deselectedTag", "&eDeselected tag {id}");
        config.addDefault("deselectedNullTag", "&cYou don't have a tag selected");
        config.addDefault("adminSetTag", "&aAn admin set your tag to {id}");
        config.addDefault("adminRemoveTag", "&aAn admin removed your tag");
        config.addDefault("noTagPermission", "&cYou don't have permission to select this tag");

        MessagesConfig.save();

        selectedTag = Colors.conv(config.getString("selectedTag"));
        alreadySelectedTag = Colors.conv(config.getString("alreadySelectedTag"));
        deselectedTag = Colors.conv(config.getString("deselectedTag"));
        deselectedNullTag = Colors.conv(config.getString("deselectedNullTag"));
        adminSetTag = Colors.conv(config.getString("adminSetTag"));
        adminRemoveTag = Colors.conv(config.getString("adminRemoveTag"));
        noTagPermission = Colors.conv(config.getString("noTagPermission"));
    }
}
