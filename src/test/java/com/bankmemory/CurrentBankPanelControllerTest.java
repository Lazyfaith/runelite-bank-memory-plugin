package com.bankmemory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CurrentBankPanelControllerTest {
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
    private BankViewPanel panel;

    @Inject
    private CurrentBankPanelController currentBankPanelController;

    @Before
    public void before() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
        when(client.isClientThread()).thenReturn(true);
        ItemComposition coins = mockItemComposition(0, "Coins");
        ItemComposition burntLobster = mockItemComposition(2, "Burnt lobster");
        when(itemManager.getItemComposition(0)).thenReturn(coins);
        when(itemManager.getItemComposition(2)).thenReturn(burntLobster);
    }

    @Test
    public void testStartup_ifNotLoggedIn_displayNoData() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);

        currentBankPanelController.startUp(panel);

        waitForEdtQueueToEmpty();
        verify(panel).displayNoDataMessage();
    }

    @Test
    public void testStartup_ifLoggedInButNoDataForAccount_displayNoData() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getUsername()).thenReturn("MrSam");
        LinkedHashMap<String, BankSave> saveData = new LinkedHashMap<>();
        saveData.put("SomeOtherAccount", new BankSave("SomeOtherAccount", "Monday",
                ImmutableList.of(new BankSave.Item(1, 1))));
        when(dataStore.loadSavedBanks()).thenReturn(saveData);

        currentBankPanelController.startUp(panel);

        waitForEdtQueueToEmpty();
        verify(panel).displayNoDataMessage();
    }

    @Test
    public void testStartup_ifLoggedInAndDataAvailable_displayAccountData() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getUsername()).thenReturn("MrSam");
        LinkedHashMap<String, BankSave> saveData = new LinkedHashMap<>();
        saveData.put("MrSam", new BankSave("MrSam", "Tuesday",
                ImmutableList.of(new BankSave.Item(0, 100), new BankSave.Item(2, 666))));
        saveData.put("SomeOtherAccount", new BankSave("SomeOtherAccount", "Monday",
                ImmutableList.of(new BankSave.Item(0, 1))));
        when(dataStore.loadSavedBanks()).thenReturn(saveData);

        currentBankPanelController.startUp(panel);

        waitForEdtQueueToEmpty();
        verify(panel).updateTimeDisplay("Tuesday");
        verify(panel).displayItemListings(eq(list("Coins", "Burnt lobster")), any());
    }

    @Test
    public void testHandleBankSave_ifItemDataHasNotChangedThenOnlyUpdateTime() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getUsername()).thenReturn("MrSam");
        when(dataStore.loadSavedBanks()).thenReturn(new LinkedHashMap<>());
        BankSave mondaySave = new BankSave("MrSam", "Monday",
                ImmutableList.of(new BankSave.Item(0, 100), new BankSave.Item(2, 666)));
        BankSave tuesdaySave = new BankSave(mondaySave.getUserName(), "Tuesday", mondaySave.getBankData());
        currentBankPanelController.startUp(panel);

        verify(panel, never()).updateTimeDisplay(any());
        verify(panel, never()).displayItemListings(any(), any());

        currentBankPanelController.handleBankSave(mondaySave);

        waitForEdtQueueToEmpty();
        verify(panel).updateTimeDisplay("Monday");
        verify(panel, times(1)).displayItemListings(eq(list("Coins", "Burnt lobster")), any());

        currentBankPanelController.handleBankSave(tuesdaySave);

        waitForEdtQueueToEmpty();
        verify(panel).updateTimeDisplay("Tuesday");
        verify(panel, times(1)).displayItemListings(any(), any());
    }

    private static ItemComposition mockItemComposition(int id, String name) {
        ItemComposition item = mock(ItemComposition.class);
        when(item.getName()).thenReturn(name);
        return item;
    }

    private static void waitForEdtQueueToEmpty() throws Exception {
        SwingUtilities.invokeAndWait(() -> { /* Do nothing */ });
    }

    private static List<String> list(String... strings) {
        return List.of(strings);
    }
}