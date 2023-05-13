package com.github.gavvydizzle.playertags.tag;

import com.github.gavvydizzle.playertags.PlayerTags;
import com.github.gavvydizzle.playertags.configs.TagsConfig;
import com.github.gavvydizzle.playertags.storage.PlayerData;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TagsManager implements Listener {

    private final PlayerTags instance;
    private final PlayerData data;
    private final ArrayList<Tag> tagsList;
    private final HashMap<UUID, Tag> playerTagMap;
    private final HashMap<String, Tag> tagsMap;
    private final TagsMenu tagsMenu;

    private volatile boolean isLoadingTags;
    private final HashMap<UUID, String> lostTagsMap; // Stores tags that can't be linked after reload

    public TagsManager(PlayerTags instance, PlayerData data) {
        this.instance = instance;
        this.data = data;
        tagsList = new ArrayList<>();
        playerTagMap = new HashMap<>();
        tagsMap = new HashMap<>();
        lostTagsMap = new HashMap<>();
        this.tagsMenu = new TagsMenu(this);

        reload();
    }

    public void reload() {
        FileConfiguration config = TagsConfig.get();

        // Close all inventories before reloading
        tagsMenu.closeAllInventories();
        isLoadingTags = true;

        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            HashMap<UUID, Tag> oldPlayerTagsMap = new HashMap<>(playerTagMap);

            tagsList.clear();
            playerTagMap.clear();
            tagsMap.clear();

            if (config.getConfigurationSection("tags") == null) {
                instance.getLogger().warning("No tags are defined");
            }
            else {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("tags")).getKeys(false)) {
                    String path = "tags." + key;

                    try {
                        String type = config.getString(path + ".type");
                        if (type == null) {
                            instance.getLogger().warning("No type provided for tag " + key + ". Valid types are STATIC and ANIMATED");
                            continue;
                        }

                        int customModelData = config.getInt(path + ".item.customModelID");

                        ItemStack lockedItem = new ItemStack(ConfigUtils.getMaterial(config.getString(path + ".item.lockedMaterial"), Material.GRAY_DYE));
                        ItemMeta meta = lockedItem.getItemMeta();
                        assert meta != null;
                        meta.setDisplayName(Colors.conv(config.getString(path + ".item.lockedName")));
                        meta.setLore(Colors.conv(config.getStringList(path + ".item.lore")));
                        if (customModelData > 0) meta.setCustomModelData(customModelData);
                        lockedItem.setItemMeta(meta);

                        ItemStack unlockedItem = lockedItem.clone();
                        unlockedItem.setType(ConfigUtils.getMaterial(config.getString(path + ".item.material"), Material.PAPER));
                        meta = unlockedItem.getItemMeta();
                        assert meta != null;
                        meta.setDisplayName(Colors.conv(config.getString(path + ".item.name")));
                        unlockedItem.setItemMeta(meta);

                        if (type.equalsIgnoreCase("static")) {
                            String tag = Colors.conv(config.getString(path + ".tag"));
                            if (tag.trim().isEmpty()) {
                                instance.getLogger().warning("No tag provided for STATIC tag " + key + ". You must set " + path + ".tag");
                                continue;
                            }

                            StaticTag staticTag = new StaticTag(key.toLowerCase(), unlockedItem, lockedItem, tag);
                            tagsMap.put(staticTag.getId(), staticTag);
                            tagsList.add(staticTag);
                        } else if (type.equalsIgnoreCase("animated")) {
                            ArrayList<String> tags = (ArrayList<String>) Colors.conv(config.getStringList(path + ".tags"));
                            tags.removeIf(str -> str.trim().isEmpty());

                            if (tags.isEmpty()) {
                                instance.getLogger().warning("No tags provided for ANIMATED tag " + key + ". You must set " + path + ".tags");
                                continue;
                            }

                            int interval = config.getInt(path + ".interval");
                            if (interval < 100) {
                                instance.getLogger().warning("Invalid interval provided for ANIMATED tag " + key + ". You must set " + path + ".interval to a number >= 100");
                                continue;
                            }

                            AnimatedTag animatedTag = new AnimatedTag(key.toLowerCase(), unlockedItem, lockedItem, tags, interval);
                            tagsMap.put(animatedTag.getId(), animatedTag);
                            tagsList.add(animatedTag);
                        } else {
                            instance.getLogger().warning("Invalid type provided for tag " + key + ". Valid types are STATIC and ANIMATED");
                        }
                    }
                    catch (Exception e) {
                        instance.getLogger().severe("Failed to load tag from " + path);
                        e.printStackTrace();
                    }
                }
            }

            tagsMenu.reloadTagsList();
            updatePlayerTagsAfterReload(oldPlayerTagsMap);

            isLoadingTags = false;
        });
    }

    /**
     * Links tags back to the player when the plugin reloads.
     * If the tag does not exist anymore, it will be saved in memory and attempted every reload until the server restarts or the player disconnects.
     * @param oldPlayerTagsMap All previously loaded player tags
     */
    private void updatePlayerTagsAfterReload(HashMap<UUID, Tag> oldPlayerTagsMap) {
        // Attempt to revalidate any lost tags
        for (Map.Entry<UUID, String> entry : lostTagsMap.entrySet()) {
            if (Bukkit.getPlayer(entry.getKey()) == null) { // The player went offline, so delete their entry
                lostTagsMap.remove(entry.getKey());
                continue;
            }

            if (tagsMap.containsKey(entry.getValue())) { // Successfully linked to tag in memory
                playerTagMap.put(entry.getKey(), tagsMap.get(entry.getValue()));
                lostTagsMap.remove(entry.getKey());
            }
        }

        // Link tags on reload and add tags to the lost tags map if it cannot link
        for (Map.Entry<UUID, Tag> entry : oldPlayerTagsMap.entrySet()) {
            if (entry.getValue() == null) { // Add back null (no selection) tags
                playerTagMap.put(entry.getKey(), null);
            }
            else if (tagsMap.containsKey(entry.getValue().getId())) { // Successfully linked to tag in memory
                playerTagMap.put(entry.getKey(), tagsMap.get(entry.getValue().getId()));
            }
            else {
                lostTagsMap.put(entry.getKey(), entry.getValue().getId());
            }
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getServer().getScheduler().runTaskAsynchronously(instance, () -> {
            String savedTagID = data.getPlayerTagID(e.getPlayer());
            if (savedTagID == null) return;

            Tag tag = tagsMap.get(savedTagID);
            if (tag != null) {
                playerTagMap.put(e.getPlayer().getUniqueId(), tag);
            }
        });
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        playerTagMap.remove(e.getPlayer().getUniqueId());
        lostTagsMap.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        tagsMenu.handleClick(e);
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent e) {
        tagsMenu.closeInventory((Player) e.getPlayer());
    }

    /**
     * Updates the player's tag in memory and pushes the changed to the database.
     * If the new tag is null, the database entry will be deleted.
     * @param player The player
     * @param tag The new tag
     */
    public void updateTag(Player player, @Nullable Tag tag) {
        lostTagsMap.remove(player.getUniqueId()); // Delete lost tag if they player updates their selection
        playerTagMap.put(player.getUniqueId(), tag);
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> data.savePlayerTag(player, tag));
    }

    /**
     * @param player The player
     * @return The player's tag or null
     */
    @Nullable
    public Tag getSelectedTag(Player player) {
        return playerTagMap.get(player.getUniqueId());
    }

    /**
     * @param player The player
     * @return The player's tag String or null
     */
    @Nullable
    public String getSelectedTagString(Player player) {
        Tag tag = playerTagMap.get(player.getUniqueId());
        if (tag == null) return null;

        return tag.getTag();
    }

    /**
     * Opens the tag menu for this player on this page
     * @param player The player
     * @param page The page to open to
     */
    public void openTagsMenu(Player player, int page) {
        tagsMenu.openInventory(player, page);
    }

    @Nullable
    public Tag getTagByID(String id) {
        return tagsMap.get(id);
    }

    public Set<String> getTagIDs() {
        return tagsMap.keySet();
    }

    protected ArrayList<Tag> getTagsList() {
        return tagsList;
    }

    public TagsMenu getTagsMenu() {
        return tagsMenu;
    }

    protected PlayerData getData() {
        return data;
    }

    public boolean isLoadingTags() {
        return isLoadingTags;
    }
}
