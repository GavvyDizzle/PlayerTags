package com.github.gavvydizzle.playertags.tag;

import com.github.gavvydizzle.playertags.PlayerTags;
import com.github.gavvydizzle.playertags.database.TagsDatabase;
import com.github.gavvydizzle.playertags.gui.InventoryManager;
import com.github.gavvydizzle.playertags.player.LoadedPlayer;
import com.github.gavvydizzle.playertags.player.PlayerManager;
import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.ConfigUtils;
import com.github.mittenmc.serverutils.ItemStackUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TagsManager implements Listener {

    private final PlayerTags instance;
    private final TagsDatabase tagsDatabase;
    private final PlayerManager playerManager;
    private final InventoryManager inventoryManager;

    private final List<Tag> tagsList;
    private final Map<String, Tag> tagsMap;

    public TagsManager(PlayerTags instance, TagsDatabase tagsDatabase, PlayerManager playerManager, InventoryManager inventoryManager) {
        this.instance = instance;
        this.tagsDatabase = tagsDatabase;
        this.playerManager = playerManager;
        this.inventoryManager = inventoryManager;

        tagsList = new ArrayList<>();
        tagsMap = new HashMap<>();

        reload();
    }

    public void reload() {
        FileConfiguration config = PlayerTags.getInstance().getConfigManager().get("tags");
        if (config == null) return;

        tagsList.clear();
        tagsMap.clear();

        if (!config.isConfigurationSection("tags")) config.createSection("tags");
        ConfigurationSection tagsSection = config.getConfigurationSection("tags");
        assert tagsSection != null;

        for (String key : tagsSection.getKeys(false)) {
            ConfigurationSection section = tagsSection.getConfigurationSection(key);
            if (section == null) continue;

            section.addDefault("type", TagType.STATIC.name());
            section.addDefault("hidden", false);

            TagType tagType = TagType.get(section.getString("type"));
            if (tagType == null) {
                instance.getLogger().warning("Invalid type provided for tag " + key + ". Valid types are STATIC and ANIMATED");
                continue;
            }

            switch (tagType) {
                case STATIC -> section.addDefault("tag", "&eplaceholder");
                case ANIMATED -> {
                    section.addDefault("interval", "100");
                    section.addDefault("tags", List.of("&evalue1", "&bvalue2"));
                }
            }

            section.addDefault("item.material", Material.PAPER.name());
            section.addDefault("item.lockedMaterial", Material.GRAY_DYE.name());
            section.addDefault("item.name", "&eTODO Name");
            section.addDefault("item.lockedName", "&cTODO Locked Name");
            section.addDefault("item.lore", List.of());

            boolean hidden = section.getBoolean("hidden");
            int customModelData = section.getInt("item.customModelID");

            ItemStack lockedItem = new ItemStack(ConfigUtils.getMaterial(section.getString("item.lockedMaterial"), Material.GRAY_DYE));
            ItemMeta meta = lockedItem.getItemMeta();
            assert meta != null;
            meta.setDisplayName(Colors.conv(section.getString("item.lockedName")));
            meta.setLore(Colors.conv(section.getStringList("item.lore")));
            if (customModelData > 0) meta.setCustomModelData(customModelData);
            lockedItem.setItemMeta(meta);

            ItemStack unlockedItem = lockedItem.clone();
            unlockedItem.setType(ConfigUtils.getMaterial(section.getString("item.material"), Material.PAPER));
            meta = unlockedItem.getItemMeta();
            assert meta != null;
            meta.setDisplayName(Colors.conv(section.getString("item.name")));
            unlockedItem.setItemMeta(meta);
            ItemStackUtils.addAllItemFlags(unlockedItem);

            switch (tagType) {
                case STATIC -> {
                    String tag = Colors.conv(section.getString("tag"));
                    if (tag.trim().isEmpty()) {
                        instance.getLogger().warning("No tag provided for STATIC tag " + key + ". You must set " + section.getCurrentPath() + ".tag");
                        continue;
                    }

                    StaticTag staticTag = new StaticTag(key.toLowerCase(), unlockedItem, lockedItem, hidden, tag);
                    tagsMap.put(staticTag.getId(), staticTag);
                    tagsList.add(staticTag);
                }
                case ANIMATED -> {
                    List<String> tags = Colors.conv(section.getStringList(".tags"));
                    tags.removeIf(str -> str.trim().isEmpty());

                    if (tags.isEmpty()) {
                        instance.getLogger().warning("No tags provided for ANIMATED tag " + key + ". You must set " + section.getCurrentPath() + ".tags");
                        continue;
                    }

                    int interval = section.getInt("interval");
                    if (interval < 100) {
                        instance.getLogger().warning("Invalid interval provided for ANIMATED tag " + key + ". You must set " + section.getCurrentPath() + ".interval to a number >= 100");
                        continue;
                    }

                    AnimatedTag animatedTag = new AnimatedTag(key.toLowerCase(), unlockedItem, lockedItem, hidden, tags, interval);
                    tagsMap.put(animatedTag.getId(), animatedTag);
                    tagsList.add(animatedTag);
                }
            }
        }

        updatePlayerTagsAfterReload();
        inventoryManager.setMenuTags(tagsList.stream().filter(tag -> !tag.isHidden()).toList());
    }

    /**
     * Attempts to re-link tags to the newly created objects after a reload
     */
    private void updatePlayerTagsAfterReload() {
        for (LoadedPlayer lp : playerManager.getAllPlayerData()) {
            String oldTagID = lp.getSelectedTagID();
            if (oldTagID == null) continue;

            lp.selectTag(getTagByID(oldTagID), false);
        }
    }

    /**
     * Updates the player's tag in memory and pushes the changed to the database.
     * If the new tag is null, the database entry will be deleted.
     * @param lp The player
     * @param tag The new tag
     */
    public void updateTag(LoadedPlayer lp, @Nullable Tag tag) {
        lp.selectTag(tag);
        // This DB call is the same as #save from PlayerManager.
        // This could change if more data gets added to DB in the future.
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> tagsDatabase.save(lp));
    }

    /**
     * Updates the player's tag in memory and pushes the changed to the database.
     * If the new tag is null, the database entry will be deleted.
     * @param lp The player
     * @param tagID The new tag's ID
     */
    public void updateTag(LoadedPlayer lp, @Nullable String tagID) {
        updateTag(lp, getTagByID(tagID));
    }

    /**
     * @param player The player
     * @return The player's tag or null
     */
    @Nullable
    public Tag getSelectedTag(Player player) {
        LoadedPlayer lp = playerManager.getPlayerData(player);
        if (lp == null) return null;

        return lp.getSelectedTag();
    }

    /**
     * @param player The player
     * @return The player's tag String or null
     */
    @Nullable
    public String getSelectedTagValue(Player player) {
        Tag tag = getSelectedTag(player);
        if (tag == null) return null;

        return tag.getValue();
    }

    @Nullable
    public Tag getTagByID(String id) {
        return tagsMap.get(id);
    }

    public Collection<String> getTagIDs() {
        return tagsMap.keySet();
    }

    /**
     * @return The number of hidden tags
     */
    public int getNumHiddenTags() {
        int n = 0;
        for (Tag tag : tagsMap.values()) {
            if (tag.isHidden()) n++;
        }
        return n;
    }
}
