package com.bankmemory;

import lombok.Value;
import net.runelite.client.util.AsyncBufferedImage;

@Value
class ItemListEntry {
    private final String itemName;
    private final AsyncBufferedImage image;
}
