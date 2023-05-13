package com.github.gavvydizzle.playertags.commands.admin;

import com.github.gavvydizzle.playertags.PlayerTags;
import com.github.gavvydizzle.playertags.commands.AdminCommandManager;
import com.github.gavvydizzle.playertags.configs.*;
import com.github.gavvydizzle.playertags.tag.TagsManager;
import com.github.gavvydizzle.playertags.utils.Messages;
import com.github.gavvydizzle.playertags.utils.Sounds;
import com.github.mittenmc.serverutils.RepeatingTask;
import com.github.mittenmc.serverutils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends SubCommand {

    private final AdminCommandManager adminCommandManager;
    private final TagsManager tagsManager;
    private final ArrayList<String> argsList;
    private final String pluginName;

    public ReloadCommand(AdminCommandManager adminCommandManager, TagsManager tagsManager) {
        this.adminCommandManager = adminCommandManager;
        this.tagsManager = tagsManager;
        pluginName = PlayerTags.getInstance().getName();

        argsList = new ArrayList<>();
        argsList.add("commands");
        argsList.add("gui");
        argsList.add("messages");
        argsList.add("tags");
        argsList.add("sounds");

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
                case "commands":
                    try {
                        reloadCommands();
                        sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded commands");
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendErrorMessage(sender);
                    }
                    break;
                case "gui":
                    try {
                        reloadGUI();
                        sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded shop GUIs");
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendErrorMessage(sender);
                    }
                    break;
                case "messages":
                    try {
                        reloadMessages();
                        sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded all messages");
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendErrorMessage(sender);
                    }
                    break;
                case "sounds":
                    try {
                        reloadSounds();
                        sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded all sounds");
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendErrorMessage(sender);
                    }
                    break;
                case "tags":
                    try {
                        reloadTags(sender);
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendErrorMessage(sender);
                    }
                    break;
            }
        }
        else {
            try {
                reloadCommands();
                reloadGUI();
                reloadMessages();
                reloadSounds();
                reloadTags(sender);
                sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] " + "Successfully reloaded");
            } catch (Exception e) {
                e.printStackTrace();
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

    private void reloadCommands() {
        CommandsConfig.reload();
        adminCommandManager.reload();
    }

    private void reloadGUI() {
        MenusConfig.reload();
        tagsManager.getTagsMenu().reload();
    }

    private void reloadMessages() {
        MessagesConfig.reload();
        Messages.reloadMessages();
    }

    private void reloadSounds() {
        SoundsConfig.reload();
        Sounds.reload();
    }

    private void reloadTags(CommandSender sender) {
        final int oldNum = tagsManager.getTagIDs().size();
        sender.sendMessage(ChatColor.YELLOW + "[" + pluginName + "] Unloaded " + oldNum + " tags");

        TagsConfig.reload();
        tagsManager.reload();

        new RepeatingTask(PlayerTags.getInstance(), 4, 4) {
            int tries = 0;

            @Override
            public void run() {
                if (tries >= 5) {
                    sender.sendMessage(ChatColor.RED + "[" + pluginName + "] Failed to reload tags");
                    cancel();
                    return;
                }

                if (!tagsManager.isLoadingTags()) {
                    int newNum = tagsManager.getTagIDs().size();

                    if (newNum == 0 && oldNum != 0) {
                        sender.sendMessage(ChatColor.RED + "[" + pluginName + "] Loaded 0 tags! Check the console for the error");
                    }
                    else if (newNum >= oldNum) {
                        sender.sendMessage(ChatColor.GREEN + "[" + pluginName + "] Loaded " + newNum + " tags");
                    }
                    else {
                        sender.sendMessage(ChatColor.YELLOW + "[" + pluginName + "] Loaded " + newNum + " tags");
                    }
                    cancel();
                }

                tries++;
            }
        };
    }

}