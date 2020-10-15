package com.bankmemory;

public interface BanksListInteractionListener {
    void selectedToOpen(BanksListEntry save);

    void selectedToDelete(BanksListEntry save);
}
