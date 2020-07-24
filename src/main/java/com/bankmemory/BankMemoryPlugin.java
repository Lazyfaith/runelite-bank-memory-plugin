package com.bankmemory;

import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

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
    private ClientThread clientThread;
    @Inject
    private ItemManager itemManager;

    private CurrentBankPanelController currentBankPanelController;

    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception {
        assert SwingUtilities.isEventDispatchThread();

        // Doing it here ensures it's created on the EDT
        BankMemoryPluginPanel panel = injector.getInstance(BankMemoryPluginPanel.class);

        BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), ICON);
        navButton = NavigationButton.builder()
                .tooltip("Bank Memory")
                .icon(icon)
                .priority(7)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        currentBankPanelController = injector.getInstance(CurrentBankPanelController.class);
        clientThread.invokeLater(currentBankPanelController::startUp);
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        currentBankPanelController.onGameStateChanged(gameStateChanged);
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        // Apparently the event to listen to for opening the bank/changing bank contents
        if ("setBankTitle".equals(event.getEventName())) {
            return;
        }
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null) {
            return;
        }

        currentBankPanelController.handleBankSave(BankSave.fromBank(bank, client, itemManager));
    }
}
