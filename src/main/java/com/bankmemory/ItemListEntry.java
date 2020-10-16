package com.bankmemory;

import lombok.Value;
import net.runelite.client.util.AsyncBufferedImage;

@Value
class ItemListEntry {
    String itemName;
    AsyncBufferedImage image;
}
