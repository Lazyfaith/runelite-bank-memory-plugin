package com.bankmemory;

import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.BankWorldType;
import com.bankmemory.data.PluginDataStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

public class CurrentBankPanelController {
    @Inject private Client client;
    @Inject private ItemManager itemManager;
    @Inject private PluginDataStore dataStore;

    private BankViewPanel panel;

    @Nullable private BankSave latestDisplayedData = null;

    public void startUp(BankViewPanel panel) {
        assert client.isClientThread();

        this.panel = panel;

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
        BankWorldType worldType = BankWorldType.forWorld(client.getWorldType());
        Optional<BankSave> existingSave = dataStore.getDataForCurrentBank(worldType, client.getUsername());
        if (existingSave.isPresent()) {
            BankSave save = existingSave.get();
            if (latestDisplayedData != null) {
                boolean userNameDifferent = latestDisplayedData.getUserName().equalsIgnoreCase(save.getUserName());
                boolean worldTypeDifferent = latestDisplayedData.getWorldType() != save.getWorldType();
                if (userNameDifferent || worldTypeDifferent) {
                    SwingUtilities.invokeLater(panel::reset);
                }
            }
            handleBankSave(existingSave.get());
        } else {
            latestDisplayedData = null;
            SwingUtilities.invokeLater(panel::displayNoDataMessage);
        }
    }

    public void handleBankSave(BankSave newSave) {
        assert client.isClientThread();

        dataStore.saveAsCurrentBank(newSave);

        boolean isDataNew = isItemDataNew(newSave);
        List<ItemListEntry> items = new ArrayList<>();
        if (isDataNew) {
            // Get all the data we need for the UI on this thread (the game thread)
            // Doing it on the EDT seems to cause random crashes & NPEs
            for (BankItem i : newSave.getItemData()) {
                ItemComposition ic = itemManager.getItemComposition(i.getItemId());
                AsyncBufferedImage icon = itemManager.getImage(i.getItemId(), i.getQuantity(), i.getQuantity() > 1);
                int geValue = itemManager.getItemPrice(i.getItemId()) * i.getQuantity();
                int haValue = ic.getHaPrice() * i.getQuantity();
                items.add(new ItemListEntry(ic.getName(), i.getQuantity(), icon, geValue, haValue));
            }
        }
        SwingUtilities.invokeLater(() -> {
            panel.updateTimeDisplay(newSave.getDateTimeString());
            if (isDataNew) {
                panel.displayItemListings(items, true);
            }
        });
        latestDisplayedData = newSave;
    }

    private boolean isItemDataNew(BankSave newSave) {
        return latestDisplayedData == null || !latestDisplayedData.getItemData().equals(newSave.getItemData());
    }
}
