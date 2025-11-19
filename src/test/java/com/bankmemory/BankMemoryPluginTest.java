package com.bankmemory;

import com.bankmemory.data.PluginDataStore;
import com.bankmemory.BankDiffPanel;
import com.bankmemory.BankMemoryItemOverlay;
import com.bankmemory.BankSavesTopPanel;
import com.bankmemory.SeedVaultSavesTopPanel;
import com.bankmemory.CurrentSeedVaultPanelController;
import com.bankmemory.SavedSeedVaultPanelController;
import com.bankmemory.SeedVaultDiffPanelController;
import com.bankmemory.bankview.BankViewPanel;
import com.bankmemory.bankview.BankViewPanel;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BankMemoryPluginTest {
    @Mock @Bind private ClientToolbar clientToolbar;
    @Mock @Bind private Client client;
    @Mock @Bind private ClientThread clientThread;
    @Mock @Bind private ItemManager itemManager;
    @Mock @Bind private PluginDataStore pluginDataStore;
    @Mock @Bind private BankMemoryConfig bankMemoryConfig;
    @Mock @Bind private OverlayManager overlayManager;
    @Mock @Bind private BankMemoryItemOverlay itemOverlay;
    @Mock private CurrentBankPanelController currentBankPanelController;
    @Mock private CurrentSeedVaultPanelController currentSeedVaultPanelController;
    @Mock private SavedBanksPanelController savedBanksPanelController;
    @Mock private SavedSeedVaultPanelController savedSeedVaultPanelController;
    @Mock private BankDiffPanelController bankDiffPanelController;
    @Mock private SeedVaultDiffPanelController seedVaultDiffPanelController;
    @Mock private BankMemoryPluginPanel pluginPanel;
    @Mock private Injector pluginInjector;
    @Mock private BankSavesTopPanel bankSavesTopPanel;
    @Mock private SeedVaultSavesTopPanel seedVaultSavesTopPanel;
    @Mock private BankDiffPanel banksDiffPanel;
    @Mock private BankDiffPanel seedVaultDiffPanel;
    @Mock private BankViewPanel currentSeedVaultViewPanel;
    @Mock private BankViewPanel currentBankViewPanel;

    @Inject private TestBankMemoryPlugin bankMemoryPlugin;

    @Before
    public void before() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
        bankMemoryPlugin.setInjector(pluginInjector);
        when(pluginInjector.getInstance(BankMemoryPluginPanel.class)).thenReturn(pluginPanel);
        when(pluginInjector.getInstance(CurrentBankPanelController.class)).thenReturn(currentBankPanelController);
        when(pluginInjector.getInstance(CurrentSeedVaultPanelController.class)).thenReturn(currentSeedVaultPanelController);
        when(pluginInjector.getInstance(SavedBanksPanelController.class)).thenReturn(savedBanksPanelController);
        when(pluginInjector.getInstance(SavedSeedVaultPanelController.class)).thenReturn(savedSeedVaultPanelController);
        when(pluginInjector.getInstance(BankDiffPanelController.class)).thenReturn(bankDiffPanelController);
        when(pluginInjector.getInstance(SeedVaultDiffPanelController.class)).thenReturn(seedVaultDiffPanelController);

        // Panels returned from pluginPanel
        when(pluginPanel.getSavedBanksTopPanel()).thenReturn(bankSavesTopPanel);
        when(pluginPanel.getSavedSeedVaultTopPanel()).thenReturn(seedVaultSavesTopPanel);
        when(pluginPanel.getCurrentSeedVaultViewPanel()).thenReturn(currentSeedVaultViewPanel);
        when(pluginPanel.getCurrentBankViewPanel()).thenReturn(currentBankViewPanel);

        // Diff panels from top panels
        when(bankSavesTopPanel.getDiffPanel()).thenReturn(banksDiffPanel);
        when(seedVaultSavesTopPanel.getDiffPanel()).thenReturn(seedVaultDiffPanel);
    }

    @Test
    public void testStartup_startsCurrentBankControllerOnClientThread() throws Exception {
        ArgumentCaptor<Runnable> ac = ArgumentCaptor.forClass(Runnable.class);

        SwingUtilities.invokeAndWait(noCatch(bankMemoryPlugin::startUp));

        verify(clientThread, atLeastOnce()).invokeLater(ac.capture());
        verify(currentBankPanelController, never()).startUp(any());
        for (Runnable r : ac.getAllValues()) {
            r.run();
        }
        verify(currentBankPanelController).startUp(pluginPanel.getCurrentBankViewPanel());
    }

    private static Runnable noCatch(ThrowingRunnable throwingRunnable) {
        return () -> {
            try {
                throwingRunnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static class TestBankMemoryPlugin extends BankMemoryPlugin {
        void setInjector(Injector injector) {
            this.injector = injector;
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}