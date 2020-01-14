package com.bankmemory;

import com.google.common.collect.ImmutableList;
import lombok.Value;

@Value
class BankSave {
	private final String userName;
	private final String timeString;
	private final ImmutableList<Item> bankData;

	@Value
	static class Item {
		private final int itemId;
		private final int quantity;
	}
}
