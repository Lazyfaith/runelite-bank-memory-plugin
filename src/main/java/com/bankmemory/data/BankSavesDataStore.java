package com.bankmemory.data;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
@Singleton
public class BankSavesDataStore {
    private static final String PLUGIN_BASE_GROUP = "bankMemory";
    private static final String CURRENT_LIST_KEY = "currentList";

    private final Object dataLock = new Object();
    private final ConfigManager configManager;
    private final ItemDataParser itemDataParser;
    private final List<BankSave> currentBankList;
    private final BlockingQueue<ConfigWrite> configWritesQueue = new LinkedBlockingQueue<>();
    private final List<StoredBanksUpdateListener> listeners = new ArrayList<>();

    @Inject
    private BankSavesDataStore(ConfigManager configManager, ItemDataParser itemDataParser) {
        this.configManager = configManager;
        this.itemDataParser = itemDataParser;
        currentBankList = loadCurrentBankList();
        Thread configWriter = new Thread(new ConfigWriter(), "Bank Memory config writer");
        configWriter.setDaemon(true);
        configWriter.start();
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

    public void addListener(StoredBanksUpdateListener listener) {
        synchronized (dataLock) {
            listeners.add(listener);
        }
    }

    public Optional<BankSave> getDataForCurrentBank(String login) {
        if (Strings.isNullOrEmpty(login)) {
            return Optional.empty();
        }
        synchronized (dataLock) {
            return currentBankList.stream()
                    .filter(s -> s.getUserName().equalsIgnoreCase(login))
                    .findAny();
        }
    }

    public List<BankSave> getCurrentBanksList() {
        synchronized (dataLock) {
            return new ArrayList<>(currentBankList);
        }
    }

    public Optional<BankSave> getBankSaveWithId(long id) {
        synchronized (dataLock) {
            return currentBankList.stream()
                    .filter(s -> s.getId() == id)
                    .findFirst();
        }
    }

    public void saveAsCurrentBank(BankSave newSave) {
        List<StoredBanksUpdateListener> listenersCopy;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);
            saveAsCurrentBankImpl(newSave);
        }
        listenersCopy.forEach(StoredBanksUpdateListener::currentBanksListChanged);
    }

    private void saveAsCurrentBankImpl(BankSave newSave) {
        // Check if there is a current bank for existing login and remove it
        currentBankList.stream()
                .filter(s -> s.getUserName().equalsIgnoreCase(newSave.getUserName()))
                .findAny()
                .ifPresent(currentBankList::remove);

        // Save new current bank at top of list
        currentBankList.add(0, newSave);
        ConfigWrite configWrite = new ConfigWrite(
                PLUGIN_BASE_GROUP, CURRENT_LIST_KEY, new ArrayList<>(currentBankList));
        try {
            log.debug("Scheduling write for {}.{}", configWrite.configGroup, configWrite.configKey);
            configWritesQueue.put(configWrite);
        } catch (InterruptedException ex) {
            log.error("Unexpected interrupt whilst schedule config write. Data not being written", ex);
        }
    }

    @AllArgsConstructor
    private static class ConfigWrite {
        final String configGroup;
        final String configKey;
        final Object data;
    }

    // NB: technically there's a race condition because it's possible that the client could shut down before the call
    // to the ConfigManager is made. However, us trying to react to a client shutdown by writing all the remaining
    // writes won't work since the ConfigManager is the first thing to react to a client shutdown and won't perform any
    // new writes once it has.
    // Not very likely to happen anyway since it basically requires user to do something in game (to trigger plugin) and
    // then super quickly (like <10ms) close the client.
    private class ConfigWriter implements Runnable {
        @Override
        public void run() {
            Gson gson = buildGson();
            while (!Thread.interrupted()) {
                ConfigWrite write;
                try {
                    write = configWritesQueue.take();
                    log.debug("Got write for {}.{}", write.configGroup, write.configKey);
                } catch (InterruptedException ex) {
                    log.warn("ConfigWriter thread interrupted", ex);
                    break;
                }
                configManager.setConfiguration(write.configGroup, write.configKey, gson.toJson(write.data));
            }
        }
    }
}
