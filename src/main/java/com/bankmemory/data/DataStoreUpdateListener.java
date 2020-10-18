package com.bankmemory.data;

public interface DataStoreUpdateListener {
    void currentBanksListChanged();

    void snapshotBanksListChanged();

    void displayNameMapUpdated();
}
