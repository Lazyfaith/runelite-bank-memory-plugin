package com.bankmemory;

import com.bankmemory.bankview.ItemListEntry;
import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.AbstractDataStoreUpdateListener;
import com.bankmemory.data.DisplayNameMapper;
import com.bankmemory.data.PluginDataStore;
import com.bankmemory.util.ClipboardActions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class SavedSeedVaultPanelController {

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager itemManager;
    @Inject private PluginDataStore dataStore;

    private SeedVaultSavesTopPanel topPanel;
    private ImageIcon casketIcon;
    private ImageIcon notedCasketIcon;
    private final AtomicBoolean workingToOpen = new AtomicBoolean();
    private DataStoreListener dataStoreListener;
    @Nullable BankSave saveForClipboardAction;

    public void startUp(SeedVaultSavesTopPanel topPanel) {
        assert SwingUtilities.isEventDispatchThread();

        this.topPanel = topPanel;
        topPanel.setVaultsListInteractionListener(new SeedVaultsListInteractionListenerImpl());
        casketIcon = new ImageIcon(itemManager.getImage(405));
        notedCasketIcon = new ImageIcon(itemManager.getImage(406));

        topPanel.displayVaultsListPanel();
        updateCurrentVaultsList();

        setPopupMenuActionOnView();

        dataStoreListener = new DataStoreListener();
        dataStore.addListener(dataStoreListener);
    }

    private void updateCurrentVaultsList() {
        List<BanksListEntry> saves = new ArrayList<>();
        DisplayNameMapper nameMapper = dataStore.getDisplayNameMapper();

        for (BankSave save : dataStore.getCurrentSeedVaultsList()) {
            String displayName = nameMapper.map(save.getAccountIdentifier());
            saves.add(new BanksListEntry(
                    save.getId(), casketIcon, save.getWorldType(), "Current seed vault", displayName, save.getDateTimeString()));
        }
        for (BankSave save : dataStore.getSnapshotSeedVaultsList()) {
            String displayName = nameMapper.map(save.getAccountIdentifier());
            saves.add(new BanksListEntry(
                    save.getId(), notedCasketIcon, save.getWorldType(), save.getSaveName(), displayName, save.getDateTimeString()));
        }

        Runnable updateList = () -> topPanel.updateVaultsList(saves);
        if (SwingUtilities.isEventDispatchThread()) {
            updateList.run();
        } else {
            SwingUtilities.invokeLater(updateList);
        }
    }

    private void setPopupMenuActionOnView() {
        topPanel.getVaultViewPanel().setItemListPopupMenuAction(new CopyItemsToClipboardAction(clientThread, itemManager) {
            @Nullable
            @Override
            public BankSave getBankItemData() {
                if (saveForClipboardAction == null) {
                    log.error("Tried to copy CSV data to clipboard before any seed vault save has been opened");
                }
                return saveForClipboardAction;
            }
        });
    }

    private void openSaved(BanksListEntry selected) {
        assert client.isClientThread();

        Optional<BankSave> save = dataStore.getBankSaveWithId(selected.getSaveId());
        if (!save.isPresent()) {
            log.error("Selected missing seed vault save: {}", selected);
            workingToOpen.set(false);
            return;
        }
        BankSave foundSave = save.get();

        List<ItemListEntry> items = new ArrayList<>();

        for (BankItem i : foundSave.getItemData()) {
            ItemComposition ic = itemManager.getItemComposition(i.getItemId());
            AsyncBufferedImage icon = itemManager.getImage(i.getItemId(), i.getQuantity(), i.getQuantity() > 1);
            int geValue = itemManager.getItemPrice(i.getItemId()) * i.getQuantity();
            int haValue = ic.getHaPrice() * i.getQuantity();
            items.add(new ItemListEntry(ic.getName(), i.getQuantity(), icon, geValue, haValue));
        }
        items.sort(seedVaultComparator());
        SwingUtilities.invokeLater(() -> {
            workingToOpen.set(false);
            saveForClipboardAction = foundSave;
            topPanel.displaySavedVaultData(selected.getSaveName(), items, foundSave.getDateTimeString());
        });
    }

    public void shutDown() {
        dataStore.removeListener(dataStoreListener);
    }

    private class SeedVaultsListInteractionListenerImpl implements SeedVaultsListInteractionListener {
        @Override
        public void selectedToOpen(BanksListEntry save) {
            if (workingToOpen.get()) {
                return;
            }
            workingToOpen.set(true);
            clientThread.invokeLater(() -> openSaved(save));
        }

        @Override
        public void selectedToDelete(BanksListEntry save) {
            dataStore.deleteBankSaveWithId(save.getSaveId());
        }

        @Override
        public void saveSeedVaultAs(BanksListEntry save, String saveName) {
            Optional<BankSave> existingSave = dataStore.getBankSaveWithId(save.getSaveId());
            if (existingSave.isPresent()) {
                dataStore.saveAsSnapshotSeedVault(saveName, existingSave.get());
            } else {
                log.error("Tried to 'Save As' missing seed vault save: {}", save);
            }
        }

        @Override
        public void copySeedVaultSaveItemDataToClipboard(BanksListEntry save) {
            Optional<BankSave> existingSave = dataStore.getBankSaveWithId(save.getSaveId());
            if (existingSave.isPresent()) {
                ClipboardActions.copyItemDataAsTsvToClipboardOnClientThread(clientThread, itemManager, existingSave.get().getItemData());
            } else {
                log.error("Tried to copy CSV data to clipboard for missing seed vault save: {}", save);
            }
        }

        @Override
        public void openSeedVaultsDiffPanel() {
            topPanel.showVaultDiffPanel();
        }
    }

    private class DataStoreListener extends AbstractDataStoreUpdateListener {
        @Override
        public void currentSeedVaultsListChanged() {
            updateCurrentVaultsList();
        }

        @Override
        public void currentSeedVaultsListOrderChanged() {
            updateCurrentVaultsList();
        }

        @Override
        public void snapshotSeedVaultsListChanged() {
            updateCurrentVaultsList();
        }

        @Override
        public void displayNameMapUpdated() {
            updateCurrentVaultsList();
        }
    }

    private static Comparator<ItemListEntry> seedVaultComparator() {
        return Comparator
                .comparing((ItemListEntry e) -> isSapling(e.getItemName()) ? 1 : 0)
                .thenComparing(e -> e.getItemName().toLowerCase());
    }

    private static boolean isSapling(String name) {
        return name != null && name.toLowerCase().contains("sapling");
    }
}
