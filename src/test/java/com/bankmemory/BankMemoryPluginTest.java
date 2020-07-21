package com.bankmemory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.util.LinkedHashMap;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BankMemoryPluginTest {
    @Mock
    @Bind
    private ClientToolbar clientToolbar;
    @Mock
    @Bind
    private Client client;
    @Mock
    @Bind
    private ItemManager itemManager;
    @Mock
    @Bind
    private BankSavesDataStore dataStore;
    @Mock
    private BankMemoryPluginPanel pluginPanel;
    @Mock
    private Injector pluginInjector;

    @Inject
    private TestBankMemoryPlugin bankMemoryPlugin;

    private final ImmutableList<BankSave.Item> itemSetA = ImmutableList.of(new BankSave.Item(1, 1));
    private final ImmutableList<BankSave.Item> itemSetB = ImmutableList.of(new BankSave.Item(2, 2), new BankSave.Item(3, 3));

    @Before
    public void before() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
        bankMemoryPlugin.setInjector(pluginInjector);
        when(pluginInjector.getInstance(BankMemoryPluginPanel.class)).thenReturn(pluginPanel);
    }

    // startup
    // if logged in, loads save if available
    // if logged in, displays no data if none available
    // if not logged in, displays no data
    @Test
    public void testStartup_ifNotLoggedIn_displaysNoData() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);

        bankMemoryPlugin.startUp();

        waitForEdtQueueToEmpty();
        verify(pluginPanel).displayNoDataMessage();
        verifyNoMoreInteractions(pluginPanel);
    }

    @Test
    public void testStartup_ifLoggedIn_ifDataAvailable_displaysData() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getUsername()).thenReturn("LazyFaith");
        LinkedHashMap<String, BankSave> existingSave = new LinkedHashMap<>();
        existingSave.put("LazyFaith", new BankSave("LazyFaith", "Tuesday", itemSetA));
        when(dataStore.loadSavedBanks()).thenReturn(existingSave);

        bankMemoryPlugin.startUp();

        waitForEdtQueueToEmpty();
        verify(pluginPanel).updateTimeDisplay("");
        verify(pluginPanel).displayItemListings(null, null);
    }

    @Test
    public void testStartup_ifLoggedIn_ifNoDataAvailable_displaysNoData() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getUsername()).thenReturn("LazyFaith");
        when(dataStore.loadSavedBanks()).thenReturn(new LinkedHashMap<>());

        bankMemoryPlugin.startUp();

        waitForEdtQueueToEmpty();
        verify(pluginPanel).displayNoDataMessage();
        verifyNoMoreInteractions(pluginPanel);
    }

    // shutdown
    // resets panel
    @Test
    public void testShutdown_resetsPanel() {

    }

    // onScriptCallbackEvent
    // bank returns null, does nothing
    // bank returns data, data is new, sets time and data
    // bank returns data, data is same as last displayed, only updates time

    private static void waitForEdtQueueToEmpty() throws Exception {
        SwingUtilities.invokeAndWait(() -> { /* Do nothing */ });
    }

    private static class TestBankMemoryPlugin extends BankMemoryPlugin {
        void setInjector(Injector injector) {
            this.injector = injector;
        }
    }
}