package com.bankmemory.data;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
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
    @Nullable String saveName;
    ImmutableList<BankItem> itemData;

    @VisibleForTesting
    public BankSave(
            String userName,
            @Nullable String saveName,
            String dateTimeString,
            ImmutableList<BankItem> itemData) {
        id = ID_BASE + idIncrementer.incrementAndGet();
        this.userName = userName;
        this.saveName = saveName;
        this.dateTimeString = dateTimeString;
        this.itemData = itemData;
    }

    public static BankSave fromCurrentBank(String userName, ItemContainer bank, ItemManager itemManager) {
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
        return new BankSave(userName, null, timeString, itemData.build());
    }

    public static BankSave snapshotFromExistingBank(String newName, BankSave existingBank) {
        Objects.requireNonNull(newName);
        return new BankSave(existingBank.userName, newName, existingBank.dateTimeString, existingBank.itemData);
    }
}
