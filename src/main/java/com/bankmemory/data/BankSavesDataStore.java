package com.bankmemory.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class BankSavesDataStore {
    private static final String PLUGIN_BASE_GROUP = "bankMemory";
    private static final String CURRENT_LIST_KEY = "currentList";

    private final ConfigManager configManager;
    private final ItemDataParser itemDataParser;
    private final List<BankSave> currentBankList;

    @Inject
    private BankSavesDataStore(ConfigManager configManager, ItemDataParser itemDataParser) {
        this.configManager = configManager;
        this.itemDataParser = itemDataParser;
        currentBankList = loadCurrentBankList();
    }

    private List<BankSave> loadCurrentBankList() {
        String listJsonString = configManager.getConfiguration(PLUGIN_BASE_GROUP, CURRENT_LIST_KEY);
        if (listJsonString == null) {
            // Never set before
            return new ArrayList<>();
        }

        Gson gson = buildGson();
        Type collectionType = new TypeToken<List<BankSave>>(){}.getType();
        try {
            List<BankSave> list = gson.fromJson(listJsonString, collectionType);
            return list == null ? new ArrayList<>() : list;
        } catch (JsonParseException ex) {
            log.error("Current bank list json invalid. All is lost", ex);
            configManager.unsetConfiguration(PLUGIN_BASE_GROUP, CURRENT_LIST_KEY);
            return new ArrayList<>();
        }
    }

    private Gson buildGson() {
        Type itemDataListType = new TypeToken<ImmutableList<BankItem>>(){}.getType();
        return new GsonBuilder()
                .registerTypeAdapter(itemDataListType, itemDataParser)
                .create();
    }

    public Optional<BankSave> getDataForCurrentBank(String login) {
        if (Strings.isNullOrEmpty(login)) {
            return Optional.empty();
        }
        return currentBankList.stream()
                .filter(s -> s.getUserName().equalsIgnoreCase(login))
                .findAny();
    }

    public void saveAsCurrentBank(BankSave newSave) {
        // Check if there is a current bank for existing login and remove it
        currentBankList.stream()
                .filter(s -> s.getUserName().equalsIgnoreCase(newSave.getUserName()))
                .findAny()
                .ifPresent(currentBankList::remove);

        // Save new current bank at top of list
        currentBankList.add(0, newSave);
        Gson gson = buildGson();
        configManager.setConfiguration(PLUGIN_BASE_GROUP, CURRENT_LIST_KEY, gson.toJson(currentBankList));
    }
}
