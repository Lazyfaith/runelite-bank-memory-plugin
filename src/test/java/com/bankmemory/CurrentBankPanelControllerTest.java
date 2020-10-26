package com.bankmemory;

import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.PluginDataStore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.util.List;
import java.util.Optional;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CurrentBankPanelControllerTest {
    @Mock @Bind private Client client;
    @Mock @Bind private ItemManager itemManager;
    @Mock @Bind private PluginDataStore dataStore;
    @Mock private BankViewPanel panel;

    @Inject private CurrentBankPanelController currentBankPanelController;

    @Mock private AsyncBufferedImage coinsIcon;
    @Mock private AsyncBufferedImage burntLobsterIcon;

    @Before
    public void before() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
        when(client.isClientThread()).thenReturn(true);
        ItemComposition coins = mockItemComposition(0, "Coins");
        ItemComposition burntLobster = mockItemComposition(2, "Burnt lobster");
        when(itemManager.getItemComposition(0)).thenReturn(coins);
        when(itemManager.getItemComposition(2)).thenReturn(burntLobster);
        when(itemManager.getImage(eq(0), anyInt(), anyBoolean())).thenReturn(coinsIcon);
        when(itemManager.getImage(eq(2), anyInt(), anyBoolean())).thenReturn(burntLobsterIcon);
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
        when(dataStore.getDataForCurrentBank("MrSam")).thenReturn(Optional.empty());

        currentBankPanelController.startUp(panel);

        waitForEdtQueueToEmpty();
        verify(panel).displayNoDataMessage();
    }

    @Test
    public void testStartup_ifLoggedInAndDataAvailable_displayAccountData() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getUsername()).thenReturn("MrSam");
        when(dataStore.getDataForCurrentBank("MrSam")).thenReturn(Optional.of(
                new BankSave("MrSam", "My Bank", "Tuesday",
                        ImmutableList.of(new BankItem(0, 100), new BankItem(2, 666)))));

        currentBankPanelController.startUp(panel);

        waitForEdtQueueToEmpty();
        verify(panel).updateTimeDisplay("Tuesday");
        verify(panel).displayItemListings(eq(list(
                new ItemListEntry("Coins", 100, coinsIcon),
                new ItemListEntry("Burnt lobster", 666, burntLobsterIcon))),
                eq(true));
    }

    @Test
    public void testHandleBankSave_ifItemDataHasNotChangedThenOnlyUpdateTime() throws Exception {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getUsername()).thenReturn("MrSam");
        BankSave mondaySave = new BankSave("MrSam", "My Bank", "Monday",
                ImmutableList.of(new BankItem(0, 100), new BankItem(2, 666)));
        BankSave tuesdaySave = new BankSave("MrSam", "My Bank", "Tuesday", mondaySave.getItemData());
        currentBankPanelController.startUp(panel);

        verify(panel, never()).updateTimeDisplay(any());
        verify(panel, never()).displayItemListings(any(), anyBoolean());

        currentBankPanelController.handleBankSave(mondaySave);

        waitForEdtQueueToEmpty();
        verify(panel).updateTimeDisplay("Monday");
        verify(panel, times(1)).displayItemListings(eq(list(
                new ItemListEntry("Coins", 100, coinsIcon),
                new ItemListEntry("Burnt lobster", 666, burntLobsterIcon))),
                eq(true));

        currentBankPanelController.handleBankSave(tuesdaySave);

        waitForEdtQueueToEmpty();
        verify(panel).updateTimeDisplay("Tuesday");
        verify(panel, times(1)).displayItemListings(any(), anyBoolean());
    }

    private static ItemComposition mockItemComposition(int id, String name) {
        ItemComposition item = mock(ItemComposition.class);
        when(item.getName()).thenReturn(name);
        return item;
    }

    private static void waitForEdtQueueToEmpty() throws Exception {
        SwingUtilities.invokeAndWait(() -> { /* Do nothing */ });
    }

    private static List<ItemListEntry> list(ItemListEntry... items) {
        return Lists.newArrayList(items);
    }
}