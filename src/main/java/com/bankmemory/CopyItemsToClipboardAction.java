package com.bankmemory;

import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.util.ClipboardActions;
import com.bankmemory.util.Constants;
import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

public abstract class CopyItemsToClipboardAction extends AbstractAction {

    private final ClientThread clientThread;
    private final ItemManager itemManager;
    private final BankMemoryConfig config;

    CopyItemsToClipboardAction(ClientThread clientThread, ItemManager itemManager,BankMemoryConfig config) {
        super(Constants.ACTION_COPY_ITEM_DATA_TO_CLIPBOARD);
        this.clientThread = Objects.requireNonNull(clientThread);
        this.itemManager = Objects.requireNonNull(itemManager);
        this.config = config;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BankSave data = getBankItemData();
        if (data == null) {
            return;
        }
        ClipboardActions.copyItemDataAsTsvToClipboardOnClientThread(clientThread, itemManager, config, data.getItemData());
    }

    @Nullable
    public abstract BankSave getBankItemData();
}
