package com.bankmemory;

import com.bankmemory.data.BankSave;
import lombok.Value;

@Value
public class BankDiffListOption {
    enum Type {
        CURRENT, SNAPSHOT
    }

    String listTest;
    Type bankType;
    BankSave save;
}
