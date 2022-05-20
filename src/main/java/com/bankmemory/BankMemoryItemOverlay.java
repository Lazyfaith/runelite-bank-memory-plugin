package com.bankmemory;

import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.BankWorldType;
import com.bankmemory.data.PluginDataStore;
import net.runelite.api.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.Optional;

public class BankMemoryItemOverlay extends Overlay {
    private final Client client;
    private final BankMemoryConfig config;
    private final TooltipManager tooltipManager;
    private final ItemManager itemManager;
    private final PluginDataStore dataStore;

    @Inject
    BankMemoryItemOverlay(Client client, BankMemoryConfig config, TooltipManager tooltipManager, ItemManager itemManager, PluginDataStore dataStore) {
        this.itemManager = itemManager;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST);
        this.client = client;
        this.config = config;
        this.tooltipManager = tooltipManager;
        this.dataStore = dataStore;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showTooltips()) {
            return null;
        }

        final MenuEntry[] menuEntries = client.getMenuEntries();

        if (menuEntries.length - 1 < 0) {
            return null;
        }

        final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        final ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        final MenuEntry menuEntry = menuEntries[menuEntries.length - 1];
        final int widgetId = menuEntry.getParam1();

        if (widgetId != WidgetInfo.INVENTORY.getId()){
            return null;
        }

        final int index = menuEntry.getParam0();
        Item item;


        if (null == inventory.getItem(index)){
            return null;
        }
        else{
            item = inventory.getItem(index);
        }

        final StringBuilder itemCount = new StringBuilder();

        try{
            BankWorldType worldType = BankWorldType.forWorld(client.getWorldType());
            Optional<BankSave> existingSave = dataStore.getDataForCurrentBank(worldType, client.getUsername());

            if(existingSave.isPresent()) {
                for (BankItem bankItem: existingSave.get().getItemData()) {
                    if (bankItem.getItemId() == item.getId()) {
                        itemCount.append("Stored: " + bankItem.getQuantity());
                    }
                }
            }
        }
        catch (Exception e){
            // Do nothing
        }

        if (!itemCount.toString().equals("")) {
            tooltipManager.add(new Tooltip(itemCount.toString()));
        }
        return null;
    }
}
