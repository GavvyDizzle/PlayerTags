# PlayerTags
A lightweight player tags plugin for PAPI

### Features
- Create tags that players can select through a GUI
- Each tag has its own permission
- Ability to create animated tags
- Saves non-loaded tags across failed plugin reloads

### Requirements
- There are a few required dependencies: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) and [ServerUtils](https://www.spigotmc.org/resources/serverutils.106515/)
  - Technically PAPI is a soft dependency, but you won't be able to display tags without it
- Currently only supports MySQL and MariaDB

### Tags - Permissions and GUI
- All tags require the permission `playertags.tag.id` where `id` is the unique identifier you use to define it in tags.yml
- `/tags [page]` Player command to open the tags menu
  - Permission: `playertags.gui`

### Admin Commands
- The base command is `tagsadmin` with the permission `playertags.admin`
- All commands require permission to use which follows the format `playertags.admin.command` where command is the name of the command
- Note: arguments with <> are required and [] are optional
- `/tagsadmin help` Opens the help menu
- `/tagsadmin reload [arg]` Reload the whole plugin or a specified part
- `/tagsadmin remove <player>` Remove a player's tag
- `/tagsadmin set <player> <tagID>` Set a player's tag regardless of permission

### Creating Tags
- This section will explain how to create both types of tags
- Tag items in the menu support customModelID if you would like to take advantage of it
- Full examples are available in `example_configs/tags.yml`
#### Static Tags
- `type: STATIC`
- These are tags that will not change. It will always display as what you put in the `tag` field

#### Animated Tags
-  `type: ANIMATED`
- These tags will cycle through the list of `tags` in the order you define them
- The `interval` defines the milliseconds before switching to the next. The smallest interval allowed is 100ms

### Placeholders
- This plugin has two placeholders
- `%playertags_tag%` Returns the player's selected tag or an empty string if one is not selected
  - Animated tags will automatically update when using this placeholder
- `%playertags_hasTag` Returns `true` if the player has a tag selected and `false` otherwise
#### How to use them
- I will give a quick example of how to make these work with the [TAB](https://github.com/NEZNAMY/TAB) plugin
  - I will be referencing conditional placeholders. Read about them [here](https://github.com/NEZNAMY/TAB/wiki/Feature-guide:-Conditional-placeholders#usage)
- To avoid unwanted spaces when using the placeholder, you could set up a conditional placeholder
  - The first condition will display the tag before the player's prefix. If they have no tag selected, no extra space will appear before their LuckPerms prefix
  - The second condition is similar, but does not use a LuckPerms prefix
```yaml
# In TAB's config.yml
conditions:
  tag: # use it with %condition:tag%
    conditions:
      - "%playertags_hasTag%=true"
    yes: "%playertags_tag% %luckperms-prefix%"
    no: "%luckperms-prefix%"
  tag_default: # use it with %condition:tag_default%
    conditions:
      - "%playertags_hasTag%=true"
    yes: "%playertags_tag%"
    no: ""
```

### Smart Saving
- If you mess up the yml syntax in tags.yml, the plugin will attempt to reassign the tags after you fix the error
- When you reload the tags, any that could not be reassigned are saved to memory
- Every time the plugin reloads, it will attempt to assign the tag back to the player
  - If the player leaves or selects a new tag before their old one is reassigned, the old one will be lost

### Notes
- Tags will appear in the menu in the order you define them in tags.yml