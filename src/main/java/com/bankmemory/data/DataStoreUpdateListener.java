package com.bankmemory.data;

public interface DataStoreUpdateListener {
    void currentBanksListChanged();

    void namedBanksListChanged();

    void displayNameMapUpdated();
}
