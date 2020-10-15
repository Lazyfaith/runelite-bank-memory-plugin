package com.bankmemory;

import javax.swing.ImageIcon;
import lombok.Value;

@Value
public class BanksListEntry {
    long saveId;
    ImageIcon icon;
    String saveName;
    String dateTime;
}
