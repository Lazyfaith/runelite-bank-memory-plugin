package com.bankmemory;

import com.bankmemory.data.BankSave;
import com.bankmemory.data.PluginDataStore;
import com.bankmemory.util.Constants;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
        name = Constants.BANK_MEMORY,
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
    @Inject
    private PluginDataStore dataStore;

    private CurrentBankPanelController currentBankPanelController;

    private NavigationButton navButton;
    private boolean displayNameRegistered = false;

    @Override
    protected void startUp() throws Exception {
        assert SwingUtilities.isEventDispatchThread();

        // Doing it here ensures it's created on the EDT
        BankMemoryPluginPanel pluginPanel = injector.getInstance(BankMemoryPluginPanel.class);

        BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), ICON);
        navButton = NavigationButton.builder()
                .tooltip(Constants.BANK_MEMORY)
                .icon(icon)
                .priority(7)
                .panel(pluginPanel)
                .build();

        clientToolbar.addNavigation(navButton);

        currentBankPanelController = injector.getInstance(CurrentBankPanelController.class);
        BankViewPanel currentBankView = pluginPanel.getCurrentBankViewPanel();
        clientThread.invokeLater(() -> currentBankPanelController.startUp(currentBankView));

        SavedBanksPanelController savedBanksPanelController = injector.getInstance(SavedBanksPanelController.class);
        savedBanksPanelController.startUp(pluginPanel.getSavedBanksTopPanel());
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        currentBankPanelController.onGameStateChanged(gameStateChanged);
        if (gameStateChanged.getGameState() != GameState.LOGGED_IN) {
            displayNameRegistered = false;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!displayNameRegistered) {
            Player player = client.getLocalPlayer();
            String charName = player == null ? null : player.getName();
            if (charName != null) {
                displayNameRegistered = true;
                dataStore.registerDisplayNameForLogin(client.getUsername(), charName);
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.BANK.getId()) {
            return;
        }
        ItemContainer bank = event.getItemContainer();
        currentBankPanelController.handleBankSave(BankSave.fromCurrentBank(client.getUsername(), bank, itemManager));
    }
}
