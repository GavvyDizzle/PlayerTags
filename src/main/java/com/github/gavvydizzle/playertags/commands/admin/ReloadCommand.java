package com.github.gavvydizzle.playertags.commands.admin;

import com.github.gavvydizzle.playertags.PlayerTags;
import com.github.gavvydizzle.playertags.commands.AdminCommandManager;
import com.github.gavvydizzle.playertags.gui.InventoryManager;
import com.github.gavvydizzle.playertags.tag.TagsManager;
import com.github.gavvydizzle.playertags.utils.Messages;
import com.github.gavvydizzle.playertags.utils.Sounds;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ReloadCommand extends SubCommand {

    private final InventoryManager inventoryManager;
    private final TagsManager tagsManager;
    private final String pluginName;

    private final List<String> argsList = List.of("messages", "menus", "tags", "sounds");

    public ReloadCommand(AdminCommandManager adminCommandManager, InventoryManager inventoryManager, TagsManager tagsManager) {
        this.inventoryManager = inventoryManager;
        this.tagsManager = tagsManager;
        pluginName = PlayerTags.getInstance().getName();

        setName("reload");
        setDescription("Reloads this plugin or a specified portion");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " reload [arg]");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "messages":
                    try {
                        reloadMessages();
                        sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded messages");
                    } catch (Exception e) {
                        PlayerTags.getInstance().getLogger().log(Level.SEVERE, "Failed to reload " + args[1].toLowerCase(), e);
                        sendErrorMessage(sender);
                    }
                    break;
                case "menus":
                    try {
                        reloadMenus();
                        sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded menus");
                    } catch (Exception e) {
                        PlayerTags.getInstance().getLogger().log(Level.SEVERE, "Failed to reload " + args[1].toLowerCase(), e);
                        sendErrorMessage(sender);
                    }
                    break;
                case "sounds":
                    try {
                        reloadSounds();
                        sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded sounds");
                    } catch (Exception e) {
                        PlayerTags.getInstance().getLogger().log(Level.SEVERE, "Failed to reload " + args[1].toLowerCase(), e);
                        sendErrorMessage(sender);
                    }
                    break;
                case "tags":
                    try {
                        reloadTags(sender);
                    } catch (Exception e) {
                        PlayerTags.getInstance().getLogger().log(Level.SEVERE, "Failed to reload " + args[1].toLowerCase(), e);
                        sendErrorMessage(sender);
                    }
                    break;
            }
        }
        else {
            try {
                reloadMenus();
                reloadMessages();
                reloadSounds();
                reloadTags(sender);
                sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded");
            } catch (Exception e) {
                PlayerTags.getInstance().getLogger().log(Level.SEVERE, "Failed to reload", e);
                sendErrorMessage(sender);
            }
        }
    }

    private void sendErrorMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Encountered an error when reloading. Check the console");
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        ArrayList<String> list = new ArrayList<>();

        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], argsList, list);
        }

        return list;
    }

    private void reloadMenus() {
        PlayerTags.getInstance().getConfigManager().reload("menus");
        inventoryManager.reload();
        PlayerTags.getInstance().getConfigManager().save("menus");
    }

    private void reloadMessages() {
        PlayerTags.getInstance().getConfigManager().reload("messages");
        Messages.reload();
        PlayerTags.getInstance().getConfigManager().save("messages");
    }

    private void reloadSounds() {
        PlayerTags.getInstance().getConfigManager().reload("sounds");
        Sounds.reload();
        PlayerTags.getInstance().getConfigManager().save("sounds");
    }

    private void reloadTags(CommandSender sender) {
        PlayerTags.getInstance().getConfigManager().reload("tags");

        int oldTagAmount = tagsManager.getTagIDs().size();
        sender.sendMessage(ChatColor.YELLOW + "[" + pluginName + "] Unloaded " + oldTagAmount + " tag(s)");

        tagsManager.reload();

        int newTagAmount = tagsManager.getTagIDs().size();
        int hiddenAmount = tagsManager.getNumHiddenTags();

        if (newTagAmount == 0 && oldTagAmount != 0) {
            sender.sendMessage(ChatColor.RED + "[" + pluginName + "] Loaded 0 tags! Check the console for the error");
        }
        else if (newTagAmount >= oldTagAmount) {
            sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] Loaded " + newTagAmount + " tag(s)");
            if (hiddenAmount != 0) {
                sender.sendMessage(ChatColor.YELLOW + "[" + pluginName + "] " + hiddenAmount + " hidden");
            }
        }
        else {
            sender.sendMessage(ChatColor.YELLOW + "[" + pluginName + "] Loaded " + newTagAmount + " tag(s)");
            if (hiddenAmount != 0) {
                sender.sendMessage(ChatColor.YELLOW + "[" + pluginName + "] " + hiddenAmount + " hidden");
            }
        }
    }

}