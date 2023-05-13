package com.github.gavvydizzle.playertags.tag;

import com.github.gavvydizzle.playertags.configs.MenusConfig;
import com.github.gavvydizzle.playertags.utils.Messages;
import com.github.gavvydizzle.playertags.utils.Sounds;
import com.github.mittenmc.serverutils.ColoredItems;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.ConfigUtils;
import com.github.mittenmc.serverutils.Numbers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TagsMenu {

    private static final int ITEMS_PER_PAGE = 45;

    private final TagsManager tagsManager;
    private String inventoryName;
    private final HashMap<UUID, MenuViewer> playersInInventory;

    private ArrayList<Tag> tagsList;
    private int maxPage = 1;
    private final ItemStack air = new ItemStack(Material.AIR);

    private final int inventorySize = 54;
    private int pageDownSlot, pageInfoSlot, pageUpSlot, deselectRowSlot;
    private ItemStack previousPageItem, pageInfoItem, nextPageItem, pageRowFiller, deselectItem_withSelection, deselectItem_noSelection;

    public TagsMenu(TagsManager tagsManager) {
        this.tagsManager = tagsManager;
        playersInInventory = new HashMap<>();

        reload();
    }

    /**
     * Reloads the tags list and max page.
     * This should be called after the TagsManager reloads
     */
    protected void reloadTagsList() {
        tagsList = tagsManager.getTagsList();
        maxPage = (tagsList.size() / 45) + 1;
    }

    public void reload() {
        FileConfiguration config = MenusConfig.get();
        config.options().copyDefaults(true);

        config.addDefault("menu.name", "Tags");
        config.addDefault("menu.filler", ColoredItems.GRAY.name());

        config.addDefault("menu.rowSlots.pageInfo", 4);
        config.addDefault("menu.rowSlots.pageDown", 3);
        config.addDefault("menu.rowSlots.pageUp", 5);
        config.addDefault("menu.rowSlots.deselect", 0);

        config.addDefault("menu.items.pageInfo.material", Material.OAK_SIGN.name());
        config.addDefault("menu.items.pageInfo.name", "&ePage: {curr}/{max}");

        config.addDefault("menu.items.pageDown.material", Material.PAPER.name());
        config.addDefault("menu.items.pageDown.name", "&ePage Down");
        config.addDefault("menu.items.pageDown.lore", Arrays.asList(
                "&7Click         &c-1",
                "&7Shift-click  &cPage 1"
        ));

        config.addDefault("menu.items.pageUp.material", Material.PAPER.name());
        config.addDefault("menu.items.pageUp.name", "&ePage Up");
        config.addDefault("menu.items.pageUp.lore", Arrays.asList(
                "&7Click         &a+1",
                "&7Shift-click  &aMax"
        ));

        config.addDefault("menu.items.deselect.material", Material.BUCKET.name());
        config.addDefault("menu.items.deselect.name", "&cDeselect");
        config.addDefault("menu.items.deselect.lore_with_tag", Arrays.asList(
                "&7Click here to deselect your tag",
                "&7Current tag: &e{id}"
        ));
        config.addDefault("menu.items.deselect.lore_no_tag", Collections.singletonList("&7Click here to deselect your tag"));

        MenusConfig.save();

        inventoryName = Colors.conv(config.getString("menu.name"));
        pageRowFiller = ColoredItems.getGlassByName(config.getString("menu.filler"));

        pageInfoSlot = inventorySize - 9 + Numbers.constrain(config.getInt("menu.rowSlots.pageInfo"), 0, 8);
        pageDownSlot = inventorySize - 9 + Numbers.constrain(config.getInt("menu.rowSlots.pageDown"), 0, 8);
        pageUpSlot =   inventorySize - 9 + Numbers.constrain(config.getInt("menu.rowSlots.pageUp"), 0, 8);
        deselectRowSlot = inventorySize - 9 + Numbers.constrain(config.getInt("menu.rowSlots.deselect"), 0, 8);

        pageInfoItem = new ItemStack(ConfigUtils.getMaterial(config.getString("menu.items.pageInfo.material"), Material.OAK_SIGN));
        ItemMeta meta = pageInfoItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv(config.getString("menu.items.pageInfo.name")));
        pageInfoItem.setItemMeta(meta);

        previousPageItem = new ItemStack(ConfigUtils.getMaterial(config.getString("menu.items.pageDown.material"), Material.PAPER));
        meta = previousPageItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv(config.getString("menu.items.pageDown.name")));
        meta.setLore(Colors.conv(config.getStringList("menu.items.pageDown.lore")));
        previousPageItem.setItemMeta(meta);

        nextPageItem = new ItemStack(ConfigUtils.getMaterial(config.getString("menu.items.pageUp.material"), Material.PAPER));
        meta = nextPageItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv(config.getString("menu.items.pageUp.name")));
        meta.setLore(Colors.conv(config.getStringList("menu.items.pageUp.lore")));
        nextPageItem.setItemMeta(meta);

        deselectItem_withSelection = new ItemStack(ConfigUtils.getMaterial(config.getString("menu.items.deselect.material"), Material.BUCKET));
        meta = deselectItem_withSelection.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv(config.getString("menu.items.deselect.name")));
        meta.setLore(Colors.conv(config.getStringList("menu.items.deselect.lore_with_tag")));
        deselectItem_withSelection.setItemMeta(meta);

        deselectItem_noSelection = deselectItem_withSelection.clone();
        meta = deselectItem_noSelection.getItemMeta();
        assert meta != null;
        meta.setLore(Colors.conv(config.getStringList("menu.items.deselect.lore_no_tag")));
        deselectItem_noSelection.setItemMeta(meta);
    }

    /**
     * Opens this inventory for the player
     * @param player The player
     * @param page The page to open to
     */
    public void openInventory(Player player, int page) {
        if (tagsManager.isLoadingTags()) {
            player.sendMessage(ChatColor.RED + "Cannot open tags menu. Please wait until the tags are done loading");
            return;
        }

        // Keep the page within existing pages
        page = Numbers.constrain(page, 1, maxPage);

        Inventory inventory = Bukkit.createInventory(player, inventorySize, inventoryName);
        openPage(player, inventory, page);

        for (int i = ITEMS_PER_PAGE; i < inventorySize; i++) {
            inventory.setItem(i, pageRowFiller);
        }
        inventory.setItem(deselectRowSlot, getDeselectItem(player));
        inventory.setItem(pageDownSlot, previousPageItem);
        inventory.setItem(pageInfoSlot, getPageItem(page));
        inventory.setItem(pageUpSlot, nextPageItem);

        player.openInventory(inventory);
        playersInInventory.put(player.getUniqueId(), new MenuViewer(tagsManager.getSelectedTag(player), page));
    }

    /**
     * To be called when the player closes any inventory.
     * If the player's tag selection has updated, then it will be pushed to the database.
     * @param player The player
     */
    public void closeInventory(Player player) {
        MenuViewer menuViewer = playersInInventory.remove(player.getUniqueId());
        if (menuViewer == null) return;

        if (menuViewer.didTagChange()) {
            tagsManager.updateTag(player, menuViewer.getNewTag());
        }
    }

    /**
     * Handles a click to any inventory assuming it the player's top inventory
     * @param e The original click event
     */
    public void handleClick(InventoryClickEvent e) {
        if (!playersInInventory.containsKey(e.getWhoClicked().getUniqueId())) return;
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        MenuViewer menuViewer = playersInInventory.get(player.getUniqueId());
        Inventory topInv = e.getView().getTopInventory();
        int slot = e.getSlot();

        int page = menuViewer.getPage();

        if (slot <  ITEMS_PER_PAGE) {
            Tag tag = getClickedTag(page, slot);
            if (tag != null) {
                // If tags are the same
                if (tag == menuViewer.getNewTag()) {
                    player.sendMessage(Messages.alreadySelectedTag);
                    Sounds.generalFailSound.playSound(player);
                }
                else if (!tag.hasPermission(player)) {
                    player.sendMessage(Messages.noTagPermission);
                    Sounds.generalFailSound.playSound(player);
                }
                else {
                    menuViewer.setNewTag(tag);
                    topInv.setItem(deselectRowSlot, getDeselectItem(player));
                    player.sendMessage(Messages.selectedTag.replace("{id}", tag.getId()));
                    Sounds.generalClickSound.playSound(player);
                }
            }
        }
        else if (slot == pageUpSlot) {
            if (page < maxPage) {
                if (e.getClick().isShiftClick()) {
                    menuViewer.setPage(maxPage);
                }
                else {
                    menuViewer.setPage(page + 1);
                }

                openPage(player, topInv, menuViewer.getPage());
                topInv.setItem(pageInfoSlot, getPageItem(menuViewer.getPage()));
                Sounds.pageTurnSound.playSound(player);
            }
            else {
                Sounds.generalFailSound.playSound(player);
            }
        }
        else if (slot == pageDownSlot) {
            if (page > 1) {
                if (e.getClick().isShiftClick()) {
                    menuViewer.setPage(1);
                }
                else {
                    menuViewer.setPage(page - 1);
                }

                openPage(player, topInv, menuViewer.getPage());
                topInv.setItem(pageInfoSlot, getPageItem(menuViewer.getPage()));
                Sounds.pageTurnSound.playSound(player);
            }
            else {
                Sounds.generalFailSound.playSound(player);
            }
        }
        else if (slot == deselectRowSlot) {
            if (menuViewer.getNewTag() != null) {
                player.sendMessage(Messages.deselectedTag.replace("{id}", menuViewer.getNewTag().getId()));
                menuViewer.setNewTag(null);
                topInv.setItem(deselectRowSlot, getDeselectItem(player));
                Sounds.deselectTagSound.playSound(player);
            }
            else {
                player.sendMessage(Messages.deselectedNullTag);
                Sounds.generalFailSound.playSound(player);
            }
        }
    }

    /**
     * @param player The player
     * @return The deselect item with placeholders replaced
     */
    private ItemStack getDeselectItem(Player player) {
        Tag tag;
        if (playersInInventory.containsKey(player.getUniqueId())) {
            tag = playersInInventory.get(player.getUniqueId()).getNewTag();
        }
        else {
            tag = tagsManager.getSelectedTag(player);
        }

        if (tag != null) {
            ItemStack itemStack = deselectItem_withSelection.clone();
            ItemMeta meta = itemStack.getItemMeta();
            assert meta != null;
            if (meta.hasLore()) {
                ArrayList<String> lore = new ArrayList<>();
                for (String s : Objects.requireNonNull(meta.getLore())) {
                    lore.add(s.replace("{id}", tag.getId()));
                }
                meta.setLore(lore);
            }
            itemStack.setItemMeta(meta);
            return itemStack;
        }
        else {
            return deselectItem_noSelection;
        }
    }

    /**
     * @param page The page
     * @param slot The slot
     * @return The tag the player clicked or null
     */
    @Nullable
    private Tag getClickedTag(int page, int slot) {
        if (slot < ITEMS_PER_PAGE) {
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            if (index < tagsList.size()) {
                return tagsList.get(index);
            }
        }
        return null;
    }

    /**
     * Reads the contents of the menu from this page and updates the inventory
     * @param inventory The inventory to update
     * @param newPage The page
     */
    private void openPage(Player player, Inventory inventory, int newPage) {
        // If on the max page, set unfilled slots with air
        if (newPage == maxPage) {
            for (int i = (newPage - 1) * ITEMS_PER_PAGE; i < tagsList.size(); i++) {
                inventory.setItem(i % 45, tagsList.get(i).getMenuItem(player));
            }
            for (int i = tagsList.size() % ITEMS_PER_PAGE; i < ITEMS_PER_PAGE; i++) {
                inventory.setItem(i, air);
            }
        }
        else {
            for (int i = (newPage - 1) * ITEMS_PER_PAGE; i < newPage * ITEMS_PER_PAGE; i++) {
                inventory.setItem(i % 45, tagsList.get(i).getMenuItem(player));
            }
        }
    }

    private ItemStack getPageItem(int page) {
        ItemStack pageInfo = pageInfoItem.clone();
        ItemMeta meta = pageInfo.getItemMeta();
        assert meta != null;
        meta.setDisplayName(meta.getDisplayName().replace("{curr}", String.valueOf(page)).replace("{max}", String.valueOf(maxPage)));
        pageInfo.setItemMeta(meta);
        return pageInfo;
    }

    /**
     * Forces all players to close this inventory
     */
    public void closeAllInventories() {
        for (UUID uuid : playersInInventory.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
                MenuViewer menuViewer = playersInInventory.get(uuid);
                if (menuViewer.didTagChange()) {
                    tagsManager.updateTag(player, menuViewer.getNewTag());
                }
            }
        }
        playersInInventory.clear();
    }

    /**
     * Updates all unsaved player tags on server shutdown
     */
    public void forceUpdateSelectedTags() {
        HashMap<UUID, Tag> map = new HashMap<>();

        for (Map.Entry<UUID, MenuViewer> entry : playersInInventory.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;

            if (entry.getValue().didTagChange()) {
                map.put(entry.getKey(), entry.getValue().getNewTag());
            }
        }

        tagsManager.getData().savePlayerTags(map);
    }

    private static class MenuViewer {

        @Nullable private final Tag initialTag;
        @Nullable private Tag newTag;
        private int page;

        public MenuViewer(@Nullable Tag current, int page) {
            initialTag = current;
            newTag = current;
            this.page = page;
        }

        /**
         * @return If the player changed their tag
         */
        public boolean didTagChange() {
            return initialTag != newTag;
        }

        /**
         * Before calling, validate that {@link #didTagChange()} returns true
         * @return The player's currently selected tag
         */
        @Nullable
        public Tag getNewTag() {
            return newTag;
        }

        /**
         * Sets the players current tag
         * @param newTag The player's newly selected tag
         */
        public void setNewTag(@Nullable Tag newTag) {
            this.newTag = newTag;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }
    }
}
