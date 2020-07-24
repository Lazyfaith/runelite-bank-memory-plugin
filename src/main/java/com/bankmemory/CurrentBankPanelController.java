package com.bankmemory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

public class CurrentBankPanelController {
    @Inject
    private Client client;
    @Inject
    private ItemManager itemManager;
    @Inject
    private BankSavesDataStore dataStore;
    @Inject
    private BankMemoryPluginPanel panel;

    // Saves stored in chronological order, with most recent saves at the end
    private LinkedHashMap<String, BankSave> existingSavesByUserName;

    @Nullable
    private BankSave latestDisplayedData = null;

    public void startUp() {
        assert client.isClientThread();

        existingSavesByUserName = dataStore.loadSavedBanks();

        if (client.getGameState() == GameState.LOGGED_IN) {
            updateDisplayForCurrentAccount();
        } else {
            SwingUtilities.invokeLater(panel::displayNoDataMessage);
        }
    }

    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        assert client.isClientThread();

        if (gameStateChanged.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        updateDisplayForCurrentAccount();
    }

    private void updateDisplayForCurrentAccount() {
        String currentUsername = client.getUsername();
        BankSave existingSave = existingSavesByUserName.get(currentUsername);
        if (existingSave != null) {
            if (latestDisplayedData != null && !latestDisplayedData.getUserName().equals(currentUsername)) {
                SwingUtilities.invokeLater(panel::reset);
            }
            handleBankSave(existingSave);
        } else {
            latestDisplayedData = null;
            SwingUtilities.invokeLater(panel::displayNoDataMessage);
        }
    }

    public void handleBankSave(BankSave newSave) {
        assert client.isClientThread();

        existingSavesByUserName.remove(newSave.getUserName());
        existingSavesByUserName.put(newSave.getUserName(), newSave);
        dataStore.saveBanks(existingSavesByUserName);

        boolean isDataNew = isItemDataNew(newSave);
        List<String> names = new ArrayList<>();
        List<AsyncBufferedImage> icons = new ArrayList<>();
        if (isDataNew) {
            // Get all the data we need for the UI on this thread (the game thread)
            // Doing it on the EDT seems to cause random crashes & NPEs
            for (BankSave.Item i : newSave.getBankData()) {
                names.add(itemManager.getItemComposition(i.getItemId()).getName());
                icons.add(itemManager.getImage(i.getItemId(), i.getQuantity(), i.getQuantity() > 1));
            }
        }
        SwingUtilities.invokeLater(() -> {
            panel.updateTimeDisplay(newSave.getTimeString());
            if (isDataNew) {
                assert names.size() == newSave.getBankData().size();
                assert icons.size() == newSave.getBankData().size();
                panel.displayItemListings(names, icons);
            }
        });
        latestDisplayedData = newSave;
    }

    private boolean isItemDataNew(BankSave newSave) {
        return latestDisplayedData == null || !latestDisplayedData.getBankData().equals(newSave.getBankData());
    }
}
