package com.bankmemory;

import com.bankmemory.bankview.BankViewPanel;
import com.bankmemory.bankview.ItemListEntry;
import com.bankmemory.data.AbstractDataStoreUpdateListener;
import com.bankmemory.data.AccountIdentifier;
import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.BankWorldType;
import com.bankmemory.data.PluginDataStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class CurrentSeedVaultPanelController {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager itemManager;
    @Inject private PluginDataStore dataStore;

    private BankViewPanel panel;

    @Nullable private BankSave latestDisplayedData = null;

    public void startUp(BankViewPanel panel) {
        assert client.isClientThread();

        this.panel = panel;
        SwingUtilities.invokeLater(this::setPopupMenuActionOnBankView);

        DataStoreListener dataStoreListener = new DataStoreListener();
        dataStore.addListener(dataStoreListener);

        if (client.getGameState() == GameState.LOGGED_IN) {
            updateDisplayForCurrentAccount();
        } else {
            SwingUtilities.invokeLater(() -> {
                panel.setNoDataMessage("Seed Vault Memory",
                        "Log in to a character and open a seed vault to populate vault data for that character.");
                panel.displayNoDataMessage();
            });
        }
    }

    private void setPopupMenuActionOnBankView() {
        this.panel.setItemListPopupMenuAction(new CopyItemsToClipboardAction(clientThread, itemManager) {
            @Nullable
            @Override
            public BankSave getBankItemData() {
                if (latestDisplayedData == null) {
                    log.error("Tried to copy CSV data to clipboard before any current seed vault shown");
                }
                return latestDisplayedData;
            }
        });
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
        String accountIdentifier = AccountIdentifier.fromAccountHash(client.getAccountHash());
        Optional<BankSave> existingSave = dataStore.getDataForCurrentSeedVault(worldType, accountIdentifier);
        if (existingSave.isPresent()) {
            viewSave(existingSave.get());
        } else {
            latestDisplayedData = null;
            SwingUtilities.invokeLater(() -> {
                panel.setNoDataMessage("Seed Vault Memory",
                        "Open a seed vault to populate vault data for this character.");
                panel.displayNoDataMessage();
            });
        }
    }

    private void viewSave(BankSave save) {
        assert client.isClientThread();

        dataStore.currentSeedVaultViewed(save.getId());

        boolean shouldReset = isIdentityDifferentToLastDisplayed(save);
        boolean shouldUpdateItemsDisplay = shouldReset || isItemDataNew(save);
        List<ItemListEntry> items = new ArrayList<>();
        if (shouldUpdateItemsDisplay) {
            for (BankItem i : save.getItemData()) {
                ItemComposition ic = itemManager.getItemComposition(i.getItemId());
                AsyncBufferedImage icon = itemManager.getImage(i.getItemId(), i.getQuantity(), i.getQuantity() > 1);
                int geValue = itemManager.getItemPrice(i.getItemId()) * i.getQuantity();
                int haValue = ic.getHaPrice() * i.getQuantity();
                items.add(new ItemListEntry(ic.getName(), i.getQuantity(), icon, geValue, haValue));
            }
            // Sort: seeds first (alphabetical), saplings last (alphabetical)
            items.sort(seedVaultComparator());
        }
        SwingUtilities.invokeLater(() -> {
            if (shouldReset) {
                panel.reset();
            }
            panel.updateTimeDisplay(save.getDateTimeString());
            if (shouldUpdateItemsDisplay) {
                panel.displayItemListings(items, true);
            }
        });
        latestDisplayedData = save;
    }

    private boolean isIdentityDifferentToLastDisplayed(BankSave newSave) {
        if (latestDisplayedData == null) {
            return true;
        }
        boolean accountIdentifiersSame = latestDisplayedData.getAccountIdentifier().equalsIgnoreCase(newSave.getAccountIdentifier());
        boolean worldTypesSame = latestDisplayedData.getWorldType() == newSave.getWorldType();
        return !accountIdentifiersSame || !worldTypesSame;
    }

    private boolean isItemDataNew(BankSave newSave) {
        return latestDisplayedData == null || !latestDisplayedData.getItemData().equals(newSave.getItemData());
    }

    private static Comparator<ItemListEntry> seedVaultComparator() {
        return Comparator
                .comparing((ItemListEntry e) -> isSapling(e.getItemName()) ? 1 : 0)
                .thenComparing(e -> e.getItemName().toLowerCase());
    }

    private static boolean isSapling(String name) {
        return name != null && name.toLowerCase().contains("sapling");
    }

    private class DataStoreListener extends AbstractDataStoreUpdateListener {
        @Override
        public void currentSeedVaultsListChanged() {
            updateDisplayForCurrentAccount();
        }
    }
}
