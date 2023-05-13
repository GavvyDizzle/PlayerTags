package com.github.gavvydizzle.playertags.commands.admin;

import com.github.gavvydizzle.playertags.commands.AdminCommandManager;
import com.github.gavvydizzle.playertags.tag.Tag;
import com.github.gavvydizzle.playertags.tag.TagsManager;
import com.github.gavvydizzle.playertags.utils.Messages;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class SetTagCommand extends SubCommand {

    private final TagsManager tagsManager;

    public SetTagCommand(AdminCommandManager adminCommandManager, TagsManager tagsManager) {
        this.tagsManager = tagsManager;

        setName("set");
        setDescription("Set a player's tag");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " set <player> <tagID>");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Invalid player: " + args[1]);
            return;
        }

        Tag tag = tagsManager.getTagByID(args[2]);
        if (tag == null) {
            sender.sendMessage(ChatColor.RED + "Invalid tag ID: " + args[2]);
            return;
        }

        Tag selectedTag = tagsManager.getSelectedTag(player);
        if (selectedTag != null && selectedTag.getId().equals(tag.getId())) {
            sender.sendMessage(ChatColor.RED + "The player already has this tag selected");
            return;
        }

        tagsManager.updateTag(player, tag);
        player.sendMessage(Messages.adminSetTag.replace("{id}", tag.getId()));
        if (sender != player) sender.sendMessage(ChatColor.GREEN + "Successfully updated " + player.getName() + "'s tag to " + tag.getId());
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        ArrayList<String> list = new ArrayList<>();

        if (args.length == 2) {
            return null;
        }
        else if (args.length == 3) {
            StringUtil.copyPartialMatches(args[2], tagsManager.getTagIDs(), list);
        }

        return list;
    }
}