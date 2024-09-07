package com.github.gavvydizzle.playertags.gui;

import com.github.gavvydizzle.playertags.PlayerTags;
import com.github.gavvydizzle.playertags.player.LoadedPlayer;
import com.github.gavvydizzle.playertags.player.PlayerManager;
import com.github.gavvydizzle.playertags.tag.Tag;
import com.github.gavvydizzle.playertags.utils.Messages;
import com.github.gavvydizzle.playertags.utils.Sounds;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.ConfigUtils;
import com.github.mittenmc.serverutils.ItemStackUtils;
import com.github.mittenmc.serverutils.Numbers;
import com.github.mittenmc.serverutils.gui.pages.ClickableItem;
import com.github.mittenmc.serverutils.gui.pages.ItemGenerator;
import com.github.mittenmc.serverutils.gui.pages.PagesMenu;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TagSelectionMenu extends PagesMenu<Tag> {

    private static final class ViewerData {
        private final String initialTagID;
        private String currentTagID;

        public ViewerData(String initialTagID, String currentTagID) {
            this.initialTagID = initialTagID;
            this.currentTagID = currentTagID;
        }

        boolean didTagChange() {
            return !Objects.equals(initialTagID, currentTagID);
        }
    }

    private final PlayerManager playerManager;
    private int deselectItemSlot;
    private ItemStack deselectItem_withSelection, deselectItem_noSelection;

    private final Map<UUID, ViewerData> viewerDataMap;

    public TagSelectionMenu(PagesMenuBuilder<Tag> builder, PlayerManager playerManager) {
        super(builder);
        this.playerManager = playerManager;
        viewerDataMap = new HashMap<>();
    }

    public void loadConfigItems(FileConfiguration config) {
        config.addDefault("menu.items.deselect.slot", 45);
        config.addDefault("menu.items.deselect.material_no_tag", Material.BUCKET.name());
        config.addDefault("menu.items.deselect.material_has_tag", Material.AXOLOTL_BUCKET.name());
        config.addDefault("menu.items.deselect.name", "&cDeselect");
        config.addDefault("menu.items.deselect.lore_with_tag", Arrays.asList(
                "&7Click here to deselect your tag",
                "&7Current tag: &e{id}"
        ));
        config.addDefault("menu.items.deselect.lore_no_tag", Collections.singletonList("&7Click here to deselect your tag"));

        deselectItemSlot = Numbers.constrain(config.getInt("menu.items.deselect.slot"), 0, 53);

        deselectItem_withSelection = new ItemStack(ConfigUtils.getMaterial(config.getString("menu.items.deselect.material_has_tag"), Material.AXOLOTL_BUCKET));
        ItemMeta meta = deselectItem_withSelection.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv(config.getString("menu.items.deselect.name")));
        meta.setLore(Colors.conv(config.getStringList("menu.items.deselect.lore_with_tag")));
        deselectItem_withSelection.setItemMeta(meta);

        deselectItem_noSelection = deselectItem_withSelection.clone();
        deselectItem_noSelection.setType(ConfigUtils.getMaterial(config.getString("menu.items.deselect.material_no_tag"), Material.BUCKET));
        meta = deselectItem_noSelection.getItemMeta();
        assert meta != null;
        meta.setLore(Colors.conv(config.getStringList("menu.items.deselect.lore_no_tag")));
        deselectItem_noSelection.setItemMeta(meta);

        addClickableItem(deselectItemSlot, new ClickableItem<ItemGenerator>(new ItemGenerator() {
            @Override
            public @NotNull ItemStack getMenuItem(Player player) {
                return getDeselectItem(player);
            }

            @Override
            public @Nullable ItemStack getPlayerItem(Player player) {
                return null;
            }
        }) {
            @Override
            public void onClick(InventoryClickEvent inventoryClickEvent, Player player) {
                ViewerData viewerData = viewerDataMap.get(player.getUniqueId());
                if (viewerData == null) return;

                if (viewerData.currentTagID != null) {
                    player.sendMessage(Messages.deselectedTag.replace("{id}", viewerData.currentTagID));
                    viewerData.currentTagID = null;
                    player.getOpenInventory().getTopInventory().setItem(deselectItemSlot, getMenuItem(player));
                    Sounds.deselectTagSound.playSound(player);
                }
                else {
                    player.sendMessage(Messages.deselectedNullTag);
                    Sounds.generalFailSound.playSound(player);
                }
            }
        });
    }

    /**
     * @param player The player
     * @return The deselect item with placeholders replaced
     */
    @NotNull
    private ItemStack getDeselectItem(Player player) {
        ViewerData viewerData = viewerDataMap.get(player.getUniqueId());
        if (viewerData == null) return deselectItem_noSelection;

        if (viewerData.currentTagID != null) {
            ItemStack itemStack = deselectItem_withSelection.clone();
            ItemStackUtils.replacePlaceholders(itemStack, Map.of("{id}", viewerData.currentTagID));
            return itemStack;
        }
        else {
            return deselectItem_noSelection;
        }
    }

    @Override
    public void openInventory(Player player) {
        LoadedPlayer lp = playerManager.getPlayerData(player);
        if (lp != null) {
            viewerDataMap.put(player.getUniqueId(), new ViewerData(lp.getSelectedTagID(), lp.getSelectedTagID()));
        }

        super.openInventory(player);
    }

    @Override
    public void closeInventory(Player player) {
        super.closeInventory(player);

        ViewerData viewerData = viewerDataMap.remove(player.getUniqueId());

        LoadedPlayer lp = playerManager.getPlayerData(player);
        if (lp == null) return;

        if (viewerData.didTagChange()) {
            PlayerTags.getInstance().getTagsManager().updateTag(lp, viewerData.currentTagID);
        }
    }

    @Override
    public void onItemClick(InventoryClickEvent e, Player player, Tag item) {
        assert e.getClickedInventory() != null;

        ViewerData viewerData = viewerDataMap.get(player.getUniqueId());
        if (viewerData == null) return;

        if (item.getId().equals(viewerData.currentTagID)) {
            player.sendMessage(Messages.alreadySelectedTag);
            Sounds.generalFailSound.playSound(player);
        }
        else if (!item.hasPermission(player)) {
            player.sendMessage(Messages.noTagPermission);
            Sounds.generalFailSound.playSound(player);
        }
        else {
            viewerData.currentTagID = item.getId();
            e.getClickedInventory().setItem(deselectItemSlot, getDeselectItem(player));
            player.sendMessage(Messages.selectedTag.replace("{id}", item.getId()));
            Sounds.generalClickSound.playSound(player);
        }
    }
}
