package com.bankmemory;

import com.bankmemory.bankview.BankViewPanel;
import com.bankmemory.data.AccountIdentifier;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.BankWorldType;
import com.bankmemory.data.PluginDataStore;
import com.bankmemory.util.Constants;
import com.google.inject.Provides;
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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
        name = Constants.BANK_MEMORY,
        description = "A searchable record of what's in your bank"
)
public class BankMemoryPlugin extends Plugin {
    private static final String ICON = "bank_memory_icon.png";

    public static final String CONFIG_GROUP = "bankmemory";

    @Inject private ClientToolbar clientToolbar;
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager itemManager;
    @Inject private PluginDataStore dataStore;

    @Inject private BankMemoryConfig config;

    @Inject private BankMemoryItemOverlay itemOverlay;

    @Inject private OverlayManager overlayManager;

    private CurrentBankPanelController currentBankPanelController;
    private CurrentSeedVaultPanelController currentSeedVaultPanelController;
    private SavedBanksPanelController savedBanksPanelController;
    private SavedSeedVaultPanelController savedSeedVaultPanelController;
    private BankDiffPanelController diffPanelController;
    private SeedVaultDiffPanelController seedVaultDiffPanelController;
    private NavigationButton navButton;
    private boolean displayNameRegistered = false;

    @Provides
    BankMemoryConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BankMemoryConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        assert SwingUtilities.isEventDispatchThread();

        // Doing it here ensures it's created on the EDT + the instance is created after the client is all set up
        // (The latter is important because otherwise lots of L&F values won't be set right and it'll look weird)
        BankMemoryPluginPanel pluginPanel = injector.getInstance(BankMemoryPluginPanel.class);

        BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), ICON);
        if (icon == null) {
            // Fallback for test environments or unexpected resource resolution issues
            icon = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
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

        currentSeedVaultPanelController = injector.getInstance(CurrentSeedVaultPanelController.class);
        BankViewPanel currentSeedVaultView = pluginPanel.getCurrentSeedVaultViewPanel();
        clientThread.invokeLater(() -> currentSeedVaultPanelController.startUp(currentSeedVaultView));

        savedBanksPanelController = injector.getInstance(SavedBanksPanelController.class);
        savedBanksPanelController.startUp(pluginPanel.getSavedBanksTopPanel());
        savedSeedVaultPanelController = injector.getInstance(SavedSeedVaultPanelController.class);
        savedSeedVaultPanelController.startUp(pluginPanel.getSavedSeedVaultTopPanel());
        
        diffPanelController = injector.getInstance(BankDiffPanelController.class);
        diffPanelController.startUp(pluginPanel.getSavedBanksTopPanel().getDiffPanel());
        
        seedVaultDiffPanelController = injector.getInstance(SeedVaultDiffPanelController.class);
        seedVaultDiffPanelController.startUp(pluginPanel.getSavedSeedVaultTopPanel().getDiffPanel());

        overlayManager.add(itemOverlay);
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
        savedBanksPanelController.shutDown();
        savedSeedVaultPanelController.shutDown();
        diffPanelController.shutDown();
        seedVaultDiffPanelController.shutDown();
        currentBankPanelController = null;
        currentSeedVaultPanelController = null;
        savedBanksPanelController = null;
        savedSeedVaultPanelController = null;
        diffPanelController = null;
        seedVaultDiffPanelController = null;
        overlayManager.remove(itemOverlay);
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
            String accountIdentifier = AccountIdentifier.fromAccountHash(client.getAccountHash());
            Player player = client.getLocalPlayer();
            String charName = player == null ? null : player.getName();
            if (accountIdentifier != null && charName != null) {
                displayNameRegistered = true;
                dataStore.registerDisplayNameForAccountId(accountIdentifier, charName);
            }
        }
    }

    @Subscribe
    @SuppressWarnings("deprecation")
    public void onItemContainerChanged(ItemContainerChanged event) {
        int containerId = event.getContainerId();
        BankWorldType worldType = BankWorldType.forWorld(client.getWorldType());
        String accountIdentifier = AccountIdentifier.fromAccountHash(client.getAccountHash());

        if (containerId == InventoryID.BANK.getId()) {
            ItemContainer bank = event.getItemContainer();
            dataStore.saveAsCurrentBank(BankSave.fromCurrentBank(worldType, accountIdentifier, bank, itemManager));
        }
        if (containerId == InventoryID.SEED_VAULT.getId()) {
            ItemContainer vault = event.getItemContainer();
            dataStore.saveAsCurrentSeedVault(BankSave.fromCurrentBank(worldType, accountIdentifier, vault, itemManager));
        }
    }
}
