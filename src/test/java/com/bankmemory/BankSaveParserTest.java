package com.bankmemory;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@RunWith(Parameterized.class)
public class BankSaveParserTest
{
	@Parameter(value = 0)
	public String saveString;
	@Parameter(value = 1)
	public List<BankSave> bankSaves;

	@Parameters
	public static Object[][] validData()
	{
		String oneSaveStr = "name goes here;date goes here;10,1,20,1,5,7,\n";
		List<BankSave> oneSaveData = list(
				new BankSave("name goes here", "date goes here", list(item(10, 1), item(20, 1), item(5, 7)))
		);

		String nSavesStr= ""
				+ "user1;date1;101,1,102,1,103,1,500,1,\n"
				+ "user2;date2;201,2,202,2,\n"
				+ "user3;date3;301,3,302,3,303,3,304,3,\n";
		List<BankSave> nSavesData = list(
				new BankSave("user1", "date1", list(item(101, 1), item(102, 1), item(103, 1), item(500, 1))),
				new BankSave("user2", "date2", list(item(201, 2), item(202, 2))),
				new BankSave("user3", "date3", list(item(301, 3), item(302, 3), item(303, 3), item(304, 3)))
		);
		return new Object[][] {
				{ "", new ArrayList<>()},
				{ oneSaveStr, oneSaveData},
				{ nSavesStr, nSavesData }
		};
	}

	private static <T> ImmutableList<T> list(T... items)
	{
		return ImmutableList.copyOf(items);
	}

	private static BankSave.Item item(int id, int quantity)
	{
		return new BankSave.Item(id, quantity);
	}

	@Test
	public void testParseSaveStringAndToSaveStringMethods_givenValidData()
	{
		BankSaveParser parser = new BankSaveParser();

		assertThat(parser.parseSaveString(saveString), is(bankSaves));
		assertThat(parser.toSaveString(bankSaves), is(saveString));
		assertThat(parser.toSaveString(parser.parseSaveString(saveString)), is(saveString));
		assertThat(parser.parseSaveString(parser.toSaveString(bankSaves)), is(bankSaves));
	}

	@Test
	public void testParseSaveString_givenInvalidData()
	{
		String tooFewFields = "name;date";
		String tooManyFields = "name;date;10,10,5,5;extra";
		String nonNumericCharactersInItemSegment = "name;date;10,10,spade,5";
		BankSaveParser parser = new BankSaveParser();

		assertThat(parser.parseSaveString(null), is(empty()));
		assertThat(parser.parseSaveString(tooFewFields), is(empty()));
		assertThat(parser.parseSaveString(tooManyFields), is(empty()));
		assertThat(parser.parseSaveString(nonNumericCharactersInItemSegment), is(empty()));
	}
}