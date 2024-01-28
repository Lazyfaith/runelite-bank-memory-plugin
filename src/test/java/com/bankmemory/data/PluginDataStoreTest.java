package com.bankmemory.data;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PluginDataStoreTest {

    @Mock @Bind private ConfigReaderWriter configReaderWriter;

    @Mock private DataStoreUpdateListener listener;

    @Before
    public void before() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);

        when(configReaderWriter.readCurrentBanks()).thenReturn(new ArrayList<>());
        when(configReaderWriter.readBankSnapshots()).thenReturn(new ArrayList<>());
        when(configReaderWriter.readNameMap()).thenReturn(new HashMap<>());
    }

    @Test
    public void testRegisterDisplayNameForLogin_ifAccountIdIsNew_savesNewNameMapAndCallsListeners() {
        String accountId = AccountIdentifier.ACCOUNT_HASH_ID_PREFIX + "123";
        PluginDataStore pluginDataStore = createPluginDataStore();

        pluginDataStore.registerDisplayNameForLogin(accountId, "CoolUsername");

        verify(configReaderWriter).writeNameMap(Map.of(accountId, "CoolUsername"));
        verify(listener).displayNameMapUpdated();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testRegisterDisplayNameForLogin_ifUsernameRegisteredForAccountIdAlready_doesNothing() {
        String accountId = AccountIdentifier.ACCOUNT_HASH_ID_PREFIX + "123";
        when(configReaderWriter.readNameMap()).thenReturn(Map.of(accountId, "CoolUsername"));
        PluginDataStore pluginDataStore = createPluginDataStore();

        pluginDataStore.registerDisplayNameForLogin(accountId, "CoolUsername");

        verify(configReaderWriter, never()).writeNameMap(any());
        verifyNoInteractions(listener);
    }

    @Test
    public void testRegisterDisplayNameForLogin_ifGivenUsernameIsDifferentToRegisteredOne_savesNewNameMapAndCallsListeners() {
        String accountId = AccountIdentifier.ACCOUNT_HASH_ID_PREFIX + "123";
        when(configReaderWriter.readNameMap()).thenReturn(Map.of(accountId, "OldUsername"));
        PluginDataStore pluginDataStore = createPluginDataStore();

        pluginDataStore.registerDisplayNameForLogin(accountId, "NewUsername");

        verify(configReaderWriter).writeNameMap(Map.of(accountId, "NewUsername"));
        verify(listener).displayNameMapUpdated();
        verifyNoMoreInteractions(listener);
    }

    private PluginDataStore createPluginDataStore() {
        PluginDataStore pluginDataStore = new PluginDataStore(configReaderWriter);
        pluginDataStore.addListener(listener);
        return pluginDataStore;
    }
}