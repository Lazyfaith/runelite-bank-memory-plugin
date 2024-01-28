package com.bankmemory.data;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class PluginDataStore {

    private final Object dataLock = new Object();
    private final Map<String, String> nameMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<BankSave> currentBankList;
    private final List<BankSave> snapshotBanksList;
    private final ConfigReaderWriter configReaderWriter;
    private final List<DataStoreUpdateListener> listeners = new ArrayList<>();

    @Inject
    private PluginDataStore(ConfigReaderWriter configReaderWriter) {
        this.configReaderWriter = configReaderWriter;
        currentBankList = this.configReaderWriter.readCurrentBanks();
        snapshotBanksList = this.configReaderWriter.readBankSnapshots();
        nameMap.putAll(this.configReaderWriter.readNameMap());
    }

    public void registerDisplayNameForLogin(String accountIdentifier, String displayName) {
        List<DataStoreUpdateListener> listenersCopy;
        boolean changed;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);
            String oldValue = nameMap.put(accountIdentifier, displayName);
            changed = !Objects.equals(oldValue, displayName);
            if (changed) {
                configReaderWriter.writeNameMap(nameMap);
            }
        }
        if (changed) {
            listenersCopy.forEach(DataStoreUpdateListener::displayNameMapUpdated);
        }
    }

    public DisplayNameMapper getDisplayNameMapper() {
        synchronized (dataLock) {
            return new DisplayNameMapper(nameMap);
        }
    }

    public void addListener(DataStoreUpdateListener listener) {
        synchronized (dataLock) {
            listeners.add(listener);
        }
    }

    public void removeListener(DataStoreUpdateListener listener) {
        synchronized (dataLock) {
            listeners.remove(listener);
        }
    }

    public Optional<BankSave> getDataForCurrentBank(BankWorldType worldType, String accountIdentifier) {
        if (Strings.isNullOrEmpty(accountIdentifier)) {
            return Optional.empty();
        }
        synchronized (dataLock) {
            return currentBankList.stream()
                    .filter(s -> s.getWorldType() == worldType && s.getAccountIdentifier().equalsIgnoreCase(accountIdentifier))
                    .findAny();
        }
    }

    public List<BankSave> getCurrentBanksList() {
        synchronized (dataLock) {
            return new ArrayList<>(currentBankList);
        }
    }

    public List<BankSave> getSnapshotBanksList() {
        synchronized (dataLock) {
            return new ArrayList<>(snapshotBanksList);
        }
    }

    public Optional<BankSave> getBankSaveWithId(long id) {
        synchronized (dataLock) {
            return Stream.concat(currentBankList.stream(), snapshotBanksList.stream())
                    .filter(s -> s.getId() == id)
                    .findFirst();
        }
    }

    public void saveAsCurrentBank(BankSave newSave) {
        List<DataStoreUpdateListener> listenersCopy;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);
            saveAsCurrentBankImpl(newSave);
        }
        listenersCopy.forEach(DataStoreUpdateListener::currentBanksListChanged);
    }

    private void saveAsCurrentBankImpl(BankSave newSave) {
        // Check if there is a current bank for existing login and remove it
        currentBankList.stream()
                .filter(s -> s.getAccountIdentifier().equalsIgnoreCase(newSave.getAccountIdentifier() )
                        && s.getWorldType() == newSave.getWorldType())
                .findAny()
                .ifPresent(currentBankList::remove);

        // Save new current bank at top of list
        currentBankList.add(0, newSave);
        configReaderWriter.writeCurrentBanks(currentBankList);
    }

    public void saveAsSnapshotBank(String newName, BankSave existingSave) {
        List<DataStoreUpdateListener> listenersCopy;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);
            snapshotBanksList.add(0, BankSave.snapshotFromExistingBank(newName, existingSave));
            configReaderWriter.writeBankSnapshots(snapshotBanksList);
        }
        listenersCopy.forEach(DataStoreUpdateListener::snapshotBanksListChanged);
    }

    public void deleteBankSaveWithId(long saveId) {
        List<DataStoreUpdateListener> listenersCopy;
        boolean changed = false;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);

            if (PluginDataStore.removeBankSaveWithIdFromList(saveId, currentBankList)) {
                configReaderWriter.writeCurrentBanks(currentBankList);
                changed = true;
            }
            if (PluginDataStore.removeBankSaveWithIdFromList(saveId, snapshotBanksList)) {
                configReaderWriter.writeBankSnapshots(snapshotBanksList);
                changed = true;
            }
        }
        if (changed) {
            listenersCopy.forEach(DataStoreUpdateListener::currentBanksListChanged);
        } else {
            log.error("Tried deleting missing bank save: {}", saveId);
        }
    }

    private static boolean removeBankSaveWithIdFromList(long id, List<BankSave> saveList) {
        Optional<BankSave> save = saveList.stream()
                .filter(s -> s.getId() == id)
                .findFirst();
        if (save.isPresent()) {
            saveList.remove(save.get());
            return true;
        }
        return false;
    }
}
