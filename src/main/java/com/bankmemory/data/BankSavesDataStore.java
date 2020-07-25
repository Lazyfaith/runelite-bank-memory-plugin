package com.bankmemory.data;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigManager;

public class BankSavesDataStore {
    @VisibleForTesting
    static final String NO_VALUE_STORED = "For if there was no stored config data";

    // Needs to remain public else default methods (like #load) can't be called
    @ConfigGroup("bankmemory")
    public interface InternalConfig extends Config {
        @ConfigItem(
                keyName = "bankSaves",
                name = "",
                description = "",
                hidden = true
        )
        default String load() {
            return NO_VALUE_STORED;
        }

        @ConfigItem(
                keyName = "bankSaves",
                name = "",
                description = ""
        )
        void save(String bankSave);
    }

    private final InternalConfig configInstance;
    private final BankSaveParser parser;

    @Inject
    private BankSavesDataStore(ConfigManager configManager, BankSaveParser parser) {
        this.configInstance = configManager.getConfig(InternalConfig.class);
        this.parser = parser;
    }

    /** Returned object is new and safe to keep/modify. */
    public LinkedHashMap<String, BankSave> loadSavedBanks() {
        LinkedHashMap<String, BankSave> result = new LinkedHashMap<>();

        String saveString = configInstance.load();
        if (NO_VALUE_STORED.equals(saveString)) {
            return result;
        }

        parser.parseSaveString(saveString).forEach(s -> result.put(s.getUserName(), s));
        return result;
    }

    public void saveBanks(LinkedHashMap<String, BankSave> saves) {
        List<BankSave> toSave = new ArrayList<>();

        // Only store the latest 20
        int start = Math.max(0, saves.size() - 20);
        int i = 0;
        for (BankSave save : saves.values()) {
            if (i >= start) {
                toSave.add(save);
            }
        }

        configInstance.save(parser.toSaveString(toSave));
    }
}
