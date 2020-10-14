package com.bankmemory.data;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Value;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

@Value
public class BankSave {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss, d MMM uuuu");
    private static final long ID_BASE = System.currentTimeMillis();
    private static final AtomicInteger idIncrementer = new AtomicInteger();

    long id;
    String dateTimeString;
    String userName;
    ImmutableList<BankItem> itemData;

    @VisibleForTesting
    public BankSave(String userName, String dateTimeString, ImmutableList<BankItem> itemData) {
        id = ID_BASE + idIncrementer.incrementAndGet();
        this.userName = userName;
        this.dateTimeString = dateTimeString;
        this.itemData = itemData;
    }

    public static BankSave fromBank(String userName, ItemContainer bank, ItemManager itemManager) {
        Objects.requireNonNull(bank);
        net.runelite.api.Item[] contents = bank.getItems();
        ImmutableList.Builder<BankItem> itemData = ImmutableList.builder();

        for (net.runelite.api.Item item : contents) {
            int idInBank = item.getId();
            int canonId = itemManager.canonicalize(idInBank);
            if (idInBank != canonId) {
                // It's just a placeholder
                continue;
            }

            itemData.add(new BankItem(canonId, item.getQuantity()));
        }
        String timeString = DATE_FORMATTER.format(ZonedDateTime.now());
        return new BankSave(userName, timeString, itemData.build());
    }
}
