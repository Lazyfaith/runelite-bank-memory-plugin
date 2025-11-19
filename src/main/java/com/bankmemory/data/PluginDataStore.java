package com.bankmemory.data;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class PluginDataStore {

    private final Object dataLock = new Object();
    private final Map<String, String> nameMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<BankSave> currentBankList;
    private final List<BankSave> snapshotBanksList;
    private final List<BankSave> currentSeedVaultList;
    private final List<BankSave> snapshotSeedVaultsList;
    private final ConfigReaderWriter configReaderWriter;
    private final List<DataStoreUpdateListener> listeners = new ArrayList<>();

    @Inject
    @VisibleForTesting
    PluginDataStore(ConfigReaderWriter configReaderWriter) {
        this.configReaderWriter = configReaderWriter;
        currentBankList = this.configReaderWriter.readCurrentBanks();
        snapshotBanksList = this.configReaderWriter.readBankSnapshots();
        currentSeedVaultList = this.configReaderWriter.readCurrentSeedVaults();
        snapshotSeedVaultsList = this.configReaderWriter.readSeedVaultSnapshots();
        nameMap.putAll(this.configReaderWriter.readNameMap());
    }

    public void registerDisplayNameForAccountId(String accountIdentifier, String displayName) {
        List<DataStoreUpdateListener> listenersCopy;
        boolean nameMapChanged;
        boolean savesChanged = false;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);
            Set<Map.Entry<String, String>> oldNameMapEntries = nameMap.entrySet();

            // Registering new display name for account ID
            String oldValue = nameMap.put(accountIdentifier, displayName);
            nameMapChanged = !Objects.equals(oldValue, displayName);
            if (nameMapChanged) {
                configReaderWriter.writeNameMap(nameMap);
            }

            // Claim existing bank saves for new style account ID if necessary
            if (nameMapChanged) {
                Optional<String> oldStyleIdRegisteredForDisplayName = oldNameMapEntries.stream()
                        .filter(e -> !e.getKey().startsWith(AccountIdentifier.ACCOUNT_HASH_ID_PREFIX))
                        .filter(e -> e.getValue().equals(displayName))
                        .findFirst()
                        .map(Map.Entry::getKey);
                if (oldStyleIdRegisteredForDisplayName.isPresent()) {
                    savesChanged = this.claimExistingSavesForNewAccountId(oldStyleIdRegisteredForDisplayName.get(), accountIdentifier);
                }
            }
        }
        if (nameMapChanged) {
            listenersCopy.forEach(DataStoreUpdateListener::displayNameMapUpdated);
        }
        if (savesChanged) {
            listenersCopy.forEach(DataStoreUpdateListener::currentBanksListChanged);
            listenersCopy.forEach(DataStoreUpdateListener::snapshotBanksListChanged);
        }
    }

    private boolean claimExistingSavesForNewAccountId(String oldAccountId, String newAccountId) {
        boolean currentBankSavesChanged = false;
        boolean snapshotBankSavesChanged = false;
        boolean currentSeedVaultSavesChanged = false;
        boolean snapshotSeedVaultSavesChanged = false;

        synchronized (dataLock) {
            for (int i = 0; i < this.currentBankList.size(); i++) {
                BankSave existingSave = this.currentBankList.get(i);
                if (existingSave.getAccountIdentifier().equals(oldAccountId)) {
                    BankSave reclaimedSave = BankSave.withNewAccountId(newAccountId, existingSave);
                    this.currentBankList.set(i, reclaimedSave);
                    currentBankSavesChanged = true;
                }
            }
            for (int i = 0; i < this.snapshotBanksList.size(); i++) {
                BankSave existingSave = this.snapshotBanksList.get(i);
                if (existingSave.getAccountIdentifier().equals(oldAccountId)) {
                    BankSave reclaimedSave = BankSave.withNewAccountId(newAccountId, existingSave);
                    this.snapshotBanksList.set(i, reclaimedSave);
                    snapshotBankSavesChanged = true;
                }
            }

            for (int i = 0; i < this.currentSeedVaultList.size(); i++) {
                BankSave existingSave = this.currentSeedVaultList.get(i);
                if (existingSave.getAccountIdentifier().equals(oldAccountId)) {
                    BankSave reclaimedSave = BankSave.withNewAccountId(newAccountId, existingSave);
                    this.currentSeedVaultList.set(i, reclaimedSave);
                    currentSeedVaultSavesChanged = true;
                }
            }
            for (int i = 0; i < this.snapshotSeedVaultsList.size(); i++) {
                BankSave existingSave = this.snapshotSeedVaultsList.get(i);
                if (existingSave.getAccountIdentifier().equals(oldAccountId)) {
                    BankSave reclaimedSave = BankSave.withNewAccountId(newAccountId, existingSave);
                    this.snapshotSeedVaultsList.set(i, reclaimedSave);
                    snapshotSeedVaultSavesChanged = true;
                }
            }

            if (currentBankSavesChanged) {
                this.configReaderWriter.writeCurrentBanks(this.currentBankList);
            }
            if (snapshotBankSavesChanged) {
                this.configReaderWriter.writeBankSnapshots(this.snapshotBanksList);
            }
            if (currentSeedVaultSavesChanged) {
                this.configReaderWriter.writeCurrentSeedVaults(this.currentSeedVaultList);
            }
            if (snapshotSeedVaultSavesChanged) {
                this.configReaderWriter.writeSeedVaultSnapshots(this.snapshotSeedVaultsList);
            }
        }

        return currentBankSavesChanged || snapshotBankSavesChanged || currentSeedVaultSavesChanged || snapshotSeedVaultSavesChanged;
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

    public List<BankSave> getCurrentSeedVaultsList() {
        synchronized (dataLock) {
            return new ArrayList<>(currentSeedVaultList);
        }
    }

    public List<BankSave> getSnapshotSeedVaultsList() {
        synchronized (dataLock) {
            return new ArrayList<>(snapshotSeedVaultsList);
        }
    }

    public Optional<BankSave> getBankSaveWithId(long id) {
        synchronized (dataLock) {
            return Stream.of(currentBankList.stream(), snapshotBanksList.stream(), currentSeedVaultList.stream(), snapshotSeedVaultsList.stream())
                    .flatMap(s -> s)
                    .filter(s -> s.getId() == id)
                    .findFirst();
        }
    }

    public void currentBankViewed(long saveId) {
        List<DataStoreUpdateListener> listenersCopy;
        boolean changed = false;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);

            Optional<BankSave> save = this.currentBankList.stream().filter(s -> s.getId() == saveId).findFirst();
            if (save.isPresent()) {
                BankSave foundSave = save.get();
                this.currentBankList.remove(foundSave);
                this.currentBankList.add(0, foundSave);
                configReaderWriter.writeCurrentBanks(currentBankList);
                changed = true;
            } else {
                log.error("Claimed to view current bank with id " + saveId + " which wasn't found in list");
            }
        }
        if (changed) {
            listenersCopy.forEach(DataStoreUpdateListener::currentBanksListOrderChanged);
        }
    }

    public void currentSeedVaultViewed(long saveId) {
        List<DataStoreUpdateListener> listenersCopy;
        boolean changed = false;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);

            Optional<BankSave> save = this.currentSeedVaultList.stream().filter(s -> s.getId() == saveId).findFirst();
            if (save.isPresent()) {
                BankSave foundSave = save.get();
                this.currentSeedVaultList.remove(foundSave);
                this.currentSeedVaultList.add(0, foundSave);
                configReaderWriter.writeCurrentSeedVaults(currentSeedVaultList);
                changed = true;
            } else {
                log.error("Claimed to view current seed vault with id " + saveId + " which wasn't found in list");
            }
        }
        if (changed) {
            listenersCopy.forEach(DataStoreUpdateListener::currentSeedVaultsListOrderChanged);
        }
    }

    public Optional<BankSave> getDataForCurrentSeedVault(BankWorldType worldType, String accountIdentifier) {
        if (Strings.isNullOrEmpty(accountIdentifier)) {
            return Optional.empty();
        }
        synchronized (dataLock) {
            return currentSeedVaultList.stream()
                    .filter(s -> s.getWorldType() == worldType && s.getAccountIdentifier().equalsIgnoreCase(accountIdentifier))
                    .findAny();
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

    public void saveAsCurrentSeedVault(BankSave newSave) {
    List<DataStoreUpdateListener> listenersCopy;
    synchronized (dataLock) {
        listenersCopy = new ArrayList<>(listeners);
        // remove existing for account/world
        currentSeedVaultList.stream()
            .filter(s -> s.getAccountIdentifier().equalsIgnoreCase(newSave.getAccountIdentifier())
                && s.getWorldType() == newSave.getWorldType())
            .findAny()
            .ifPresent(currentSeedVaultList::remove);
        currentSeedVaultList.add(0, newSave);
        configReaderWriter.writeCurrentSeedVaults(currentSeedVaultList);
    }
    listenersCopy.forEach(DataStoreUpdateListener::currentSeedVaultsListChanged);
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

    public void saveAsSnapshotSeedVault(String newName, BankSave existingSave) {
        List<DataStoreUpdateListener> listenersCopy;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);
            snapshotSeedVaultsList.add(0, BankSave.snapshotFromExistingBank(newName, existingSave));
            configReaderWriter.writeSeedVaultSnapshots(snapshotSeedVaultsList);
        }
        listenersCopy.forEach(DataStoreUpdateListener::snapshotSeedVaultsListChanged);
    }

    public void deleteBankSaveWithId(long saveId) {
        List<DataStoreUpdateListener> listenersCopy;
        boolean currentBanksChanged = false;
        boolean snapshotBanksChanged = false;
        boolean currentSeedVaultsChanged = false;
        boolean snapshotSeedVaultsChanged = false;
        synchronized (dataLock) {
            listenersCopy = new ArrayList<>(listeners);

            if (PluginDataStore.removeBankSaveWithIdFromList(saveId, currentBankList)) {
                configReaderWriter.writeCurrentBanks(currentBankList);
                currentBanksChanged = true;
            }
            if (PluginDataStore.removeBankSaveWithIdFromList(saveId, snapshotBanksList)) {
                configReaderWriter.writeBankSnapshots(snapshotBanksList);
                snapshotBanksChanged = true;
            }
            if (PluginDataStore.removeBankSaveWithIdFromList(saveId, currentSeedVaultList)) {
                configReaderWriter.writeCurrentSeedVaults(currentSeedVaultList);
                currentSeedVaultsChanged = true;
            }
            if (PluginDataStore.removeBankSaveWithIdFromList(saveId, snapshotSeedVaultsList)) {
                configReaderWriter.writeSeedVaultSnapshots(snapshotSeedVaultsList);
                snapshotSeedVaultsChanged = true;
            }
        }
        if (currentBanksChanged) {
            listenersCopy.forEach(DataStoreUpdateListener::currentBanksListChanged);
        } else if (snapshotBanksChanged) {
            listenersCopy.forEach(DataStoreUpdateListener::snapshotBanksListChanged);
        } else if (currentSeedVaultsChanged) {
            listenersCopy.forEach(DataStoreUpdateListener::currentSeedVaultsListChanged);
        } else if (snapshotSeedVaultsChanged) {
            listenersCopy.forEach(DataStoreUpdateListener::snapshotSeedVaultsListChanged);
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
