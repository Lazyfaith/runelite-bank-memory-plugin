package com.bankmemory;

import java.awt.Dimension;
import java.awt.Graphics2D;

import com.bankmemory.data.AccountIdentifier;
import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.BankWorldType;
import com.bankmemory.data.PluginDataStore;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import java.util.Optional;

public class BankMemoryItemOverlay extends Overlay {
    private final Client client;
    private final ItemManager itemManager;
    private final BankMemoryConfig config;
    private final TooltipManager tooltipManager;
    private final PluginDataStore dataStore;

    @Inject
    BankMemoryItemOverlay(Client client, ItemManager itemManager, BankMemoryConfig config, TooltipManager tooltipManager, PluginDataStore dataStore) {
        setPosition(OverlayPosition.TOOLTIP);
        setPriority(0f);
        this.client = client;
        this.itemManager = itemManager;
        this.config = config;
        this.tooltipManager = tooltipManager;
        this.dataStore = dataStore;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showTooltips()) {
            return null;
        }

        MenuEntry[] menuEntries = client.getMenuEntries();

        if (menuEntries.length < 1) {
            return null;
        }

        MenuEntry menuEntry = menuEntries[menuEntries.length - 1];
        int widgetId = menuEntry.getParam1();

        if (widgetId != ComponentID.INVENTORY_CONTAINER) {
            return null;
        }

        int index = menuEntry.getParam0();


        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        Item item = inventory.getItem(index);
        if (item == null) {
            return null;
        }

        String itemCountTooltipText = null;

        BankWorldType worldType = BankWorldType.forWorld(client.getWorldType());
        String accountIdentifier = AccountIdentifier.fromAccountHash(client.getAccountHash());
        Optional<BankSave> existingSave = dataStore.getDataForCurrentBank(worldType, accountIdentifier);

        int itemCanonId = itemManager.canonicalize(item.getId());
        if (existingSave.isPresent()) {
            for (BankItem bankItem : existingSave.get().getItemData()) {
                if (bankItem.getItemId() == itemCanonId) {
                    itemCountTooltipText = "Banked: " + bankItem.getQuantity();
                    break;
                }
            }
        }

        if (itemCountTooltipText != null) {
            tooltipManager.add(new Tooltip(itemCountTooltipText));
        }
        return null;
    }
}
