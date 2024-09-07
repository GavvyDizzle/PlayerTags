package com.github.gavvydizzle.playertags.commands;

import com.github.gavvydizzle.playertags.commands.admin.ReloadCommand;
import com.github.gavvydizzle.playertags.commands.admin.RemoveTagCommand;
import com.github.gavvydizzle.playertags.commands.admin.SetTagCommand;
import com.github.gavvydizzle.playertags.gui.InventoryManager;
import com.github.gavvydizzle.playertags.player.PlayerManager;
import com.github.gavvydizzle.playertags.tag.TagsManager;
import com.github.mittenmc.serverutils.CommandManager;
import com.github.mittenmc.serverutils.command.HelpCommand;
import org.bukkit.command.PluginCommand;

public class AdminCommandManager extends CommandManager {

    public AdminCommandManager(PluginCommand command, PlayerManager playerManager, InventoryManager inventoryManager, TagsManager tagsManager) {
        super(command);

        registerCommand(new HelpCommand.HelpCommandBuilder(this).build());
        registerCommand(new ReloadCommand(this, inventoryManager, tagsManager));
        registerCommand(new RemoveTagCommand(this, playerManager, tagsManager));
        registerCommand(new SetTagCommand(this, playerManager, tagsManager));
    }
}