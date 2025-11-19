package com.bankmemory;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BankMemoryPlugin.CONFIG_GROUP)
public interface BankMemoryConfig extends Config {
    @ConfigItem(
            position = 1,
            keyName = "itemCountTooltips",
            name = "Show tooltips",
            description = "Show count of items in bank on item tooltips"
    )
    default boolean showTooltips() {
        return true;
    }

    void setTooltips(boolean tooltips);

    @ConfigItem(
            position = 2,
            keyName = "minValue",
            name = "Minimum stack value",
            description = "Minimum item stack GE value to display"
    )
    default int minValue() {
        return 0;
    }
    @ConfigItem(
            position = 3,
            keyName = "sortMode",
            name = "Sort mode",
            description = "Mode to sort the displayed items"
    )
    default SortMode sortMode() {
        return SortMode.DEFAULT;
    }
}
