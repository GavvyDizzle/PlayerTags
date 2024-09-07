package com.github.gavvydizzle.playertags.gui;

import com.github.gavvydizzle.playertags.PlayerTags;
import com.github.gavvydizzle.playertags.player.PlayerManager;
import com.github.gavvydizzle.playertags.tag.Tag;
import com.github.mittenmc.serverutils.ColoredItems;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.gui.MenuManager;
import com.github.mittenmc.serverutils.gui.pages.PagesMenu;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

public class InventoryManager extends MenuManager {

    private final PlayerManager playerManager;
    private TagSelectionMenu tagSelectionMenu;

    public InventoryManager(JavaPlugin plugin, PlayerManager playerManager) {
        super(plugin);

        this.playerManager = playerManager;
        reload();
    }

    public void reload() {
        FileConfiguration config = PlayerTags.getInstance().getConfigManager().get("menus");
        if (config == null) return;

        config.addDefault("menu.name", "Tags");
        config.addDefault("menu.filler", ColoredItems.GRAY.name());

        String inventoryName = Colors.conv(config.getString("menu.name"));
        ItemStack pageRowFiller = ColoredItems.getGlassByName(config.getString("menu.filler"));

        tagSelectionMenu = new TagSelectionMenu(new PagesMenu.PagesMenuBuilder<Tag>(inventoryName, 6).pageRowFiller(pageRowFiller), playerManager);
        tagSelectionMenu.loadConfigItems(config);
    }

    public void setMenuTags(Collection<Tag> tags) {
        tagSelectionMenu.setItems(tags);
    }

    public void openTagSelectionMenu(Player player) {
        super.openMenu(player, tagSelectionMenu);
    }
}
