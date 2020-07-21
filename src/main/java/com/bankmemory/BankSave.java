package com.bankmemory;

import com.google.common.collect.ImmutableList;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

@Value
class BankSave {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss, d MMM");

    private final String userName;
    private final String timeString;
    private final ImmutableList<Item> bankData;

    @Value
    static class Item {
        private final int itemId;
        private final int quantity;
    }

    public static BankSave fromBank(ItemContainer bank, Client client, ItemManager itemManager) {
        Objects.requireNonNull(bank);
        net.runelite.api.Item[] contents = bank.getItems();
        ImmutableList.Builder<BankSave.Item> bankData = ImmutableList.builder();

        for (net.runelite.api.Item item : contents) {
            int idInBank = item.getId();
            int canonId = itemManager.canonicalize(idInBank);
            if (idInBank != canonId) {
                // It's just a placeholder
                continue;
            }

            bankData.add(new BankSave.Item(canonId, item.getQuantity()));
        }
        String timeString = FORMATTER.format(ZonedDateTime.now());
        return new BankSave(client.getUsername(), timeString, bankData.build());
    }
}
