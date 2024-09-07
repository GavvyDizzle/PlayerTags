package com.github.gavvydizzle.playertags.commands.admin;

import com.github.gavvydizzle.playertags.commands.AdminCommandManager;
import com.github.gavvydizzle.playertags.player.LoadedPlayer;
import com.github.gavvydizzle.playertags.player.PlayerManager;
import com.github.gavvydizzle.playertags.tag.Tag;
import com.github.gavvydizzle.playertags.tag.TagsManager;
import com.github.gavvydizzle.playertags.utils.Messages;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RemoveTagCommand extends SubCommand {

    private final PlayerManager playerManager;
    private final TagsManager tagsManager;

    public RemoveTagCommand(AdminCommandManager adminCommandManager, PlayerManager playerManager, TagsManager tagsManager) {
        this.playerManager = playerManager;
        this.tagsManager = tagsManager;

        setName("remove");
        setDescription("Remove a player's tag");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " remove <player>");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Invalid player: " + args[1]);
            return;
        }

        LoadedPlayer lp = playerManager.getPlayerData(player);
        if (lp == null) {
            sender.sendMessage(ChatColor.RED + "Player data not loaded: " + args[1]);
            return;
        }

        Tag selectedTag = tagsManager.getSelectedTag(player);
        if (selectedTag == null) {
            sender.sendMessage(ChatColor.RED + "The player does not have a tag selected");
            return;
        }

        tagsManager.updateTag(lp, (Tag) null);
        player.sendMessage(Messages.adminRemoveTag);
        if (sender != player) sender.sendMessage(ChatColor.GREEN + "Successfully removed " + player.getName() + "'s tag");
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return null;
        }
        return new ArrayList<>();
    }
}