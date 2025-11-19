package com.bankmemory;

import com.bankmemory.BankDiffListOption.Type;
import com.bankmemory.bankview.ItemListEntry;
import com.bankmemory.data.AbstractDataStoreUpdateListener;
import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.DataStoreUpdateListener;
import com.bankmemory.data.DisplayNameMapper;
import com.bankmemory.data.PluginDataStore;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class SeedVaultDiffPanelController {

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager itemManager;
    @Inject private PluginDataStore dataStore;
    @Inject private ItemListDiffGenerator diffGenerator;

    private BankDiffPanel diffPanel;
    private DataUpdateListener dataListener;
    private BankDiffListOption lastBeforeSelection;
    private BankDiffListOption lastAfterSelection;

    public void startUp(BankDiffPanel diffPanel) {
        this.diffPanel = diffPanel;
        dataListener = new DataUpdateListener();
        diffPanel.setInteractionListener(this::userSelectedSaves);
        diffPanel.addHierarchyListener(e -> diffPanel.resetSelectionsAndItemList());
        dataStore.addListener(dataListener);
        updateForLatestData(true);
    }

    private void updateForLatestData(boolean currentChanged) {
        assert SwingUtilities.isEventDispatchThread();

        List<BankDiffListOption> current = new ArrayList<>();
        List<BankDiffListOption> snapshots = new ArrayList<>();
        DisplayNameMapper nameMapper = dataStore.getDisplayNameMapper();

        for (BankSave save : dataStore.getCurrentSeedVaultsList()) {
            String displayName = nameMapper.map(save.getAccountIdentifier());
            current.add(new BankDiffListOption(displayName, Type.CURRENT, save));
        }
        for (BankSave save : dataStore.getSnapshotSeedVaultsList()) {
            snapshots.add(new BankDiffListOption(save.getSaveName(), Type.SNAPSHOT, save));
        }

        diffPanel.displayBankOptions(current, snapshots);

        BankDiffListOption equivalentBefore = findEquivalent(lastBeforeSelection, current, snapshots);
        BankDiffListOption equivalentAfter = findEquivalent(lastAfterSelection, current, snapshots);
        if (equivalentBefore != null && equivalentAfter != null) {
            diffPanel.setSelections(equivalentBefore, equivalentAfter);
            if (currentChanged && (equivalentBefore.getBankType() == Type.CURRENT || equivalentAfter.getBankType() == Type.CURRENT)) {
                displayDiffOfSaves(equivalentBefore, equivalentAfter, true);
            }
        }
    }

    private BankDiffListOption findEquivalent(
            BankDiffListOption old,
            List<BankDiffListOption> current,
            List<BankDiffListOption> snapshots) {
        if (old == null) {
            return null;
        }
        switch (old.getBankType()) {
            case CURRENT:
                return current.stream()
                        .filter(b -> old.getSave().getAccountIdentifier().equalsIgnoreCase(b.getSave().getAccountIdentifier()))
                        .findAny().orElse(null);
            case SNAPSHOT:
                return snapshots.stream()
                        .filter(b -> old.getSave().getId() == b.getSave().getId())
                        .findAny().orElse(null);
        }
        throw new AssertionError();
    }

    private void userSelectedSaves(BankDiffListOption before, BankDiffListOption after) {
        assert SwingUtilities.isEventDispatchThread();
        lastBeforeSelection = before;
        lastAfterSelection = after;
        displayDiffOfSaves(before, after, false);
    }

    private void displayDiffOfSaves(BankDiffListOption before, BankDiffListOption after, boolean keepListPosition) {
        assert SwingUtilities.isEventDispatchThread();

        List<BankItem> differences = diffGenerator.findDifferencesBetween(
                before.getSave().getItemData(), after.getSave().getItemData());
        clientThread.invokeLater(() -> gatherItemDataToDisplay(differences, keepListPosition));
    }

    private void gatherItemDataToDisplay(List<BankItem> differences, boolean keepListPosition) {
        List<ItemListEntry> items = new ArrayList<>();

        for (BankItem i : differences) {
            ItemComposition ic = itemManager.getItemComposition(i.getItemId());
            AsyncBufferedImage icon = itemManager.getImage(i.getItemId(), i.getQuantity(), false);
            int geValue = itemManager.getItemPrice(i.getItemId()) * i.getQuantity();
            int haValue = ic.getHaPrice() * i.getQuantity();
            items.add(new ItemListEntry(ic.getName(), i.getQuantity(), icon, geValue, haValue));
        }

        items.sort(seedVaultComparator());
        SwingUtilities.invokeLater(() -> diffPanel.displayItems(items, keepListPosition));
    }

    public void shutDown() {
        dataStore.removeListener(dataListener);
        lastBeforeSelection = null;
        lastAfterSelection = null;
    }

    private static java.util.Comparator<ItemListEntry> seedVaultComparator() {
        return java.util.Comparator
                .comparing((ItemListEntry e) -> isSapling(e.getItemName()) ? 1 : 0)
                .thenComparing(e -> e.getItemName().toLowerCase());
    }

    private static boolean isSapling(String name) {
        return name != null && name.toLowerCase().contains("sapling");
    }

    private class DataUpdateListener extends AbstractDataStoreUpdateListener {
        @Override
        public void currentSeedVaultsListChanged() {
            SwingUtilities.invokeLater(() -> updateForLatestData(true));
        }

        @Override
        public void snapshotSeedVaultsListChanged() {
            SwingUtilities.invokeLater(() -> updateForLatestData(false));
        }

        @Override
        public void displayNameMapUpdated() {
            SwingUtilities.invokeLater(() -> updateForLatestData(false));
        }
    }
}
