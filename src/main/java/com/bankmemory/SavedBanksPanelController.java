package com.bankmemory;

import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.DataStoreUpdateListener;
import com.bankmemory.data.PluginDataStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class SavedBanksPanelController {

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ItemManager itemManager;
    @Inject
    private PluginDataStore dataStore;

    private BankSavesTopPanel topPanel;
    private ImageIcon casketIcon;
    private ImageIcon notedCasketIcon;
    private final AtomicBoolean workingToOpenBank = new AtomicBoolean();
    private DataStoreListener dataStoreListener;

    public void startUp(BankSavesTopPanel topPanel) {
        assert SwingUtilities.isEventDispatchThread();

        this.topPanel = topPanel;
        topPanel.setBanksListInteractionListener(new BanksListInteractionListenerImpl());
        casketIcon = new ImageIcon(itemManager.getImage(405));
        notedCasketIcon = new ImageIcon(itemManager.getImage(406));

        topPanel.displayBanksListPanel();
        updateCurrentBanksList();

        dataStoreListener = new DataStoreListener();
        dataStore.addListener(dataStoreListener);
    }

    // Gets called on EDT and on game client thread
    private void updateCurrentBanksList() {
        List<BanksListEntry> saves = new ArrayList<>();
        TreeMap<String, String> displayNameMap = dataStore.getCurrentDisplayNameMap();

        for (BankSave save : dataStore.getCurrentBanksList()) {
            String displayName = displayNameMap.getOrDefault(save.getUserName(), save.getUserName());
            saves.add(new BanksListEntry(
                    save.getId(), casketIcon, "Current bank", displayName, save.getDateTimeString()));
        }
        for (BankSave save : dataStore.getNamedBanksList()) {
            String displayName = displayNameMap.getOrDefault(save.getUserName(), save.getUserName());
            saves.add(new BanksListEntry(
                    save.getId(), notedCasketIcon, save.getSaveName(), displayName, save.getDateTimeString()));
        }

        Runnable updateList = () -> topPanel.updateBanksList(saves);
        if (SwingUtilities.isEventDispatchThread()) {
            updateList.run();
        } else {
            SwingUtilities.invokeLater(updateList);
        }
    }

    private void openSavedBank(BanksListEntry selected) {
        assert client.isClientThread();

        Optional<BankSave> save = dataStore.getBankSaveWithId(selected.getSaveId());
        if (!save.isPresent()) {
            log.error("Selected missing bank save: {}", selected);
            workingToOpenBank.set(false);
            return;
        }
        BankSave foundSave = save.get();

        List<String> itemNames = new ArrayList<>();
        List<AsyncBufferedImage> itemIcons = new ArrayList<>();

        for (BankItem i : foundSave.getItemData()) {
            itemNames.add(itemManager.getItemComposition(i.getItemId()).getName());
            itemIcons.add(itemManager.getImage(i.getItemId(), i.getQuantity(), i.getQuantity() > 1));
        }
        SwingUtilities.invokeLater(() -> {
            workingToOpenBank.set(false);
            topPanel.displaySavedBankData(selected.getSaveName(), itemNames, itemIcons, foundSave.getDateTimeString());
        });
    }

    public void shutDown() {
        dataStore.removeListener(dataStoreListener);
    }

    private class BanksListInteractionListenerImpl implements BanksListInteractionListener {
        @Override
        public void selectedToOpen(BanksListEntry save) {
            if (workingToOpenBank.get()) {
                return;
            }
            workingToOpenBank.set(true);
            clientThread.invokeLater(() -> openSavedBank(save));
        }

        @Override
        public void selectedToDelete(BanksListEntry save) {
            dataStore.deleteBankSaveWithId(save.getSaveId());
        }

        @Override
        public void saveBankAs(BanksListEntry save, String saveName) {
            Optional<BankSave> existingSave = dataStore.getBankSaveWithId(save.getSaveId());
            if (existingSave.isPresent()) {
                dataStore.saveAsNamedBank(saveName, existingSave.get());
            } else {
                log.error("Tried to 'Save As' missing bank save: {}", save);
            }
        }
    }

    private class DataStoreListener implements DataStoreUpdateListener {
        @Override
        public void currentBanksListChanged() {
            updateCurrentBanksList();
        }

        @Override
        public void namedBanksListChanged() {
            updateCurrentBanksList();
        }

        @Override
        public void displayNameMapUpdated() {
            updateCurrentBanksList();
        }
    }
}
