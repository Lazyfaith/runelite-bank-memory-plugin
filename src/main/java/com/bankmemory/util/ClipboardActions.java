package com.bankmemory.util;

import com.bankmemory.BankMemoryConfig;
import com.bankmemory.SortMode;
import com.bankmemory.data.BankItem;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

public class ClipboardActions
{
    private ClipboardActions()
    {
    }

    public static void copyItemDataAsTsvToClipboardOnClientThread(
            ClientThread clientThread, ItemManager itemManager, BankMemoryConfig config, List<BankItem> itemData)
    {

        Objects.requireNonNull(clientThread);
        Objects.requireNonNull(itemManager);
        Objects.requireNonNull(config);
        Objects.requireNonNull(itemData);


        assert SwingUtilities.isEventDispatchThread();

        clientThread.invokeLater(() -> {
            var itemsToDisplay = itemData.stream()
                    .filter(i -> itemManager.getItemPrice(i.getItemId()) * i.getQuantity() >= config.minValue())
                    .collect(Collectors.toList());

            if (config.sortMode() == SortMode.VALUE)
            {
                sortByGeValueDescending(itemsToDisplay, itemManager);
            }
                StringBuilder sb = new StringBuilder();


                sb.append("Item id\tItem name\tItem quantity").append(System.lineSeparator());

                itemsToDisplay.forEach(i -> sb
                        .append(i.getItemId()).append('\t')
                        .append(itemManager.getItemComposition(i.getItemId()).getName()).append('\t')
                        .append(i.getQuantity()).append(System.lineSeparator()));

                StringSelection stringSelection = new StringSelection(sb.toString());

                // Bad to pass in the StringSelection as the ClipboardOwner?
                // Idk, maybe! But its implementation of that interface is NOOP and that's good with me
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
        });
    }


    private static void sortByGeValueDescending(List<BankItem> itemsToDisplay, ItemManager itemManager)
    {
        itemsToDisplay.sort((item1, item2) -> {
            // Sort by geValue in descending order, use absolute values because removed items are displayed as negatives
            return Integer.compare(
                    Math.abs(itemManager.getItemPrice(item2.getItemId()) * item2.getQuantity()),
                    Math.abs(itemManager.getItemPrice(item1.getItemId()) * item1.getQuantity())
            );
        });
    }
}

