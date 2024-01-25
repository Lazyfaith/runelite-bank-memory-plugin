package com.bankmemory.ui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.Text;

public final class ComboBoxListRenderer<T> extends JLabel implements ListCellRenderer<T> {

	@Override
	public Component getListCellRendererComponent(JList<? extends T> list, T o, int index, boolean isSelected,
			boolean cellHasFocus) {
		if (isSelected) {
			setBackground(ColorScheme.DARK_GRAY_COLOR);
			setForeground(Color.WHITE);
		} else {
			setBackground(list.getBackground());
			setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}

		setBorder(new EmptyBorder(5, 5, 5, 0));

		String text;
		if (o instanceof Enum<?>) {
			text = Text.titleCase((Enum<?>) o);
		} else {
			text = o.toString();
		}

		setText(text);

		return this;
	}
}