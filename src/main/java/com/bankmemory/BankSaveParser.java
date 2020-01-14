package com.bankmemory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

class BankSaveParser
{
	String toSaveString(List<BankSave> saves)
	{
		StringBuilder sb = new StringBuilder();
		for (BankSave save : saves)
		{
			sb.append(save.getUserName()).append(";");
			sb.append(save.getTimeString()).append(";");
			save.getBankData().forEach(i -> sb.append(i.getItemId()).append(",").append(i.getQuantity()).append(","));
			sb.append("\n");
		}
		return sb.toString();
	}

	List<BankSave> parseSaveString(String string)
	{
		try
		{
			return doParse(string);
		} catch (IllegalArgumentException ex)
		{
			return new ArrayList<>();
		}
	}

	/**
	 * @throws IllegalArgumentException if the String format is incorrect
	 * @throws NumberFormatException (more specifically) if there are non-integers in the item IDs/quantities section
	 */
	private static List<BankSave> doParse(String string) throws IllegalArgumentException
	{
		if (Strings.isNullOrEmpty(string))
		{
			return new ArrayList<>();
		}
		List<BankSave> parsedSaves = new ArrayList<>();

		String[] lines = string.split("\n");

		for (String line : lines)
		{
			String[] parts = line.split(";");

			dataAssert(parts.length == 3);

			List<Integer> numbers = new ArrayList<>();
			for (String num : parts[2].split(","))
			{
				numbers.add(Integer.parseInt(num));
			}

			dataAssert(numbers.size() % 2 == 0);
			ImmutableList.Builder<BankSave.Item> items = ImmutableList.builder();
			for (int i = 0; i < numbers.size(); i += 2)
			{
				items.add(new BankSave.Item(numbers.get(i), numbers.get(i + 1)));
			}
			parsedSaves.add(new BankSave(parts[0], parts[1], items.build()));
		}

		return parsedSaves;
	}

	private static void dataAssert(boolean result) throws IllegalArgumentException
	{
		if (!result)
		{
			throw new IllegalArgumentException();
		}
	}
}
