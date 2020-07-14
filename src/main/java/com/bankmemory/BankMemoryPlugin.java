package com.bankmemory;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@PluginDescriptor(
        name = "Bank Memory",
        description = "A searchable record of what's in your bank"
)
public class BankMemoryPlugin extends Plugin {
    private static final String ICON = "bank_memory_icon.png";

    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private Client client;
    @Inject
    private ItemManager itemManager;
    @Inject
    private BankSavesDataStore dataStore;

    private BankMemoryPanel panel;
    private NavigationButton navButton;

    // Saves stored in chronological order, with most recent saves at the end
    private LinkedHashMap<String, BankSave> existingSavesByUserName;

    @Nullable
    private BankSave latestDisplayedData = null;

    @Override
    protected void startUp() throws Exception {
        existingSavesByUserName = dataStore.loadSavedBanks();
        panel = injector.getInstance(BankMemoryPanel.class);

        BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), ICON);
        navButton = NavigationButton.builder()
                .tooltip("Bank Memory")
                .icon(icon)
                .priority(7)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        if (client.getGameState() == GameState.LOGGED_IN) {
            updateDisplayForCurrentAccount();
        } else {
            panel.displayNoDataMessage();
        }
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

    @Override
    protected void shutDown() {
        latestDisplayedData = null;
        panel.reset();
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        updateDisplayForCurrentAccount();
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        // Apparently the event to listen to for opening the bank/changing bank contents
        if (!"setBankTitle".equals(event.getEventName())) {
            return;
        }
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null) {
            return;
        }

        handleBankSave(BankSave.fromBank(bank, client, itemManager));
    }

    private void handleBankSave(BankSave newSave) {
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
