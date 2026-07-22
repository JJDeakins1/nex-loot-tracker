package com.nexloottracker.ui;

import com.nexloottracker.NexLootTracker;
import com.nexloottracker.NexUniques;
import lombok.Getter;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class SplitChanger extends JPanel
{
	private static final double CONDENSED_ICON_SCALE = 0.9;
	private static final double FULL_ICON_SCALE = 1.75;

	@Getter
	private final NexLootTracker kill;
	private final ItemManager itemManager;
	private final NexLootTrackerPanel panel;
	private boolean locked = false;

	public SplitChanger(
		final ItemManager itemManager,
		final NexLootTracker kill,
		final NexLootTrackerPanel panel,
		final boolean condensed
	)
	{
		this.itemManager = itemManager;
		this.kill = kill;
		this.panel = panel;

		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		if (condensed)
		{
			setLayout(new BorderLayout(6, 0));
			setBorder(new EmptyBorder(3, 5, 3, 5));
			buildCondensedRow();
		}
		else
		{
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBorder(new EmptyBorder(3, 5, 5, 5));
			add(getImagePanel());
			add(getVarPanel());
		}
	}

	private void buildCondensedRow()
	{
		final NexUniques unique = NexUniques.fromName(kill.getSpecialLoot());
		final int itemId = unique != null ? unique.getItemId() : 0;
		final AsyncBufferedImage image = itemManager.getImage(itemId, 1, false);

		JLabel icon = new JLabel();
		icon.setVerticalAlignment(SwingConstants.CENTER);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setToolTipText(kill.getSpecialLoot());
		icon.setIcon(new ImageIcon(resizeImage(image, CONDENSED_ICON_SCALE)));
		image.onLoaded(() ->
		{
			icon.setIcon(new ImageIcon(resizeImage(image, CONDENSED_ICON_SCALE)));
			icon.revalidate();
			icon.repaint();
		});

		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
		left.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		left.add(icon);
		left.add(Box.createRigidArea(new Dimension(6, 0)));

		JLabel receiver = panel.textPanel(fixSpaces(kill.getSpecialLootReceiver()));
		receiver.setForeground(ColorScheme.LIGHT_GRAY_COLOR.brighter());
		receiver.setToolTipText(getAbsoluteDateText());
		left.add(receiver);

		JCheckBox ffa = new JCheckBox("FFA?");
		ffa.setFont(FontManager.getRunescapeSmallFont());
		ffa.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		ffa.setSelected(kill.isFreeForAll());
		ffa.setToolTipText("Toggle free-for-all and save immediately");
		ffa.addActionListener(e ->
		{
			kill.setFreeForAll(ffa.isSelected());
			if (ffa.isSelected())
			{
				setFfa();
			}
			else
			{
				setSplit();
			}
			panel.persistKill(kill);
		});

		add(left, BorderLayout.CENTER);
		add(ffa, BorderLayout.EAST);
	}

	private JPanel getImagePanel()
	{
		final NexUniques unique = NexUniques.fromName(kill.getSpecialLoot());
		final int itemId = unique != null ? unique.getItemId() : 0;
		final AsyncBufferedImage image = itemManager.getImage(itemId, 1, false);

		JPanel iconWrapper = new JPanel();
		iconWrapper.setLayout(new BoxLayout(iconWrapper, BoxLayout.Y_AXIS));
		iconWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel icon = new JLabel();
		icon.setIcon(new ImageIcon(resizeImage(image, FULL_ICON_SCALE)));
		icon.setVerticalAlignment(SwingConstants.CENTER);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setToolTipText(kill.getSpecialLoot());

		image.onLoaded(() ->
		{
			icon.setIcon(new ImageIcon(resizeImage(image, FULL_ICON_SCALE)));
			icon.revalidate();
			icon.repaint();
		});

		JLabel date = panel.textPanel(getDateText());
		date.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		date.setToolTipText(getAbsoluteDateText());

		iconWrapper.add(date);
		iconWrapper.add(Box.createRigidArea(new Dimension(0, 10)));
		iconWrapper.add(icon);
		iconWrapper.add(Box.createRigidArea(new Dimension(0, 10)));

		return iconWrapper;
	}

	private JPanel getVarPanel()
	{
		JPanel varPanel = new JPanel();
		varPanel.setLayout(new BoxLayout(varPanel, BoxLayout.Y_AXIS));
		varPanel.setBorder(new EmptyBorder(5, 5, 5, 0));
		varPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel splitReceivedWrapper = new JPanel(new GridLayout(0, 2));
		splitReceivedWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel splitReceivedLabel = panel.textPanel("Split Amount: ");
		splitReceivedLabel.setHorizontalAlignment(SwingConstants.LEFT);

		JTextField splitReceived = getTextField();
		splitReceived.setText(format(atLeastZero(kill.getLootSplitReceived())));
		splitReceived.setToolTipText(NumberFormat.getInstance().format(atLeastZero(kill.getLootSplitReceived())));

		splitReceived.getDocument().addDocumentListener(new SimpleDocumentListener()
		{
			@Override
			public void changed(DocumentEvent e)
			{
				if (!locked)
				{
					int value = parse(splitReceived.getText());
					if (value != kill.getLootSplitReceived() && value != -5)
					{
						kill.setLootSplitReceived(value);
						if (kill.isFreeForAll())
						{
							kill.setSpecialLootValue(value);
						}
						else
						{
							kill.setSpecialLootValue(value * kill.getTeamSize());
							setSplit();
						}
						splitReceived.setToolTipText(NumberFormat.getInstance().format(atLeastZero(kill.getLootSplitReceived())));
						panel.setUpdateButton(true);
					}
				}
			}
		});

		splitReceived.addActionListener(e -> splitReceived.setText(format(atLeastZero(kill.getLootSplitReceived()))));
		splitReceivedWrapper.add(splitReceivedLabel);
		splitReceivedWrapper.add(splitReceived);

		JPanel ffaWrapper = new JPanel(new GridLayout(0, 2));
		ffaWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JCheckBox ffa = new JCheckBox("FFA?");
		ffa.setBorder(new EmptyBorder(0, 15, 0, 0));
		ffa.setSelected(kill.isFreeForAll());
		ffa.addActionListener(e ->
		{
			kill.setFreeForAll(ffa.isSelected());
			locked = true;
			if (ffa.isSelected())
			{
				setFfa();
			}
			else
			{
				setSplit();
			}
			splitReceived.setText(format(atLeastZero(kill.getLootSplitReceived())));
			splitReceived.setToolTipText(NumberFormat.getInstance().format(atLeastZero(kill.getLootSplitReceived())));
			locked = false;
			panel.persistKill(kill);
		});

		JPanel receivedWrapper = new JPanel();
		receivedWrapper.setLayout(new BoxLayout(receivedWrapper, BoxLayout.Y_AXIS));
		receivedWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel receivedBy = panel.textPanel("received by: ");
		receivedBy.setForeground(ColorScheme.LIGHT_GRAY_COLOR.brighter());
		JLabel receiver = panel.textPanel(fixSpaces(kill.getSpecialLootReceiver()));
		receiver.setForeground(ColorScheme.LIGHT_GRAY_COLOR.brighter());
		receivedWrapper.add(receivedBy);
		receivedWrapper.add(receiver);

		ffaWrapper.add(receivedWrapper);
		ffaWrapper.add(ffa);

		JPanel teamSizeWrapper = new JPanel(new GridLayout(0, 2));
		teamSizeWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel teamSizeLabel = panel.textPanel("Team Size: ");
		teamSizeLabel.setHorizontalAlignment(SwingConstants.LEFT);

		SpinnerNumberModel model = new SpinnerNumberModel(Math.min(Math.max(1, kill.getTeamSize()), 100), 1, 100, 1);
		JSpinner teamSize = new JSpinner(model);
		teamSize.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		JFormattedTextField spinnerTextField = ((JSpinner.DefaultEditor) teamSize.getEditor()).getTextField();
		spinnerTextField.setColumns(2);
		teamSize.addChangeListener(e ->
		{
			locked = true;
			kill.setTeamSize(Math.min(Math.max(1, Integer.parseInt(teamSize.getValue().toString())), 100));
			setSplit();
			splitReceived.setText(format(atLeastZero(kill.getLootSplitReceived())));
			splitReceived.setToolTipText(NumberFormat.getInstance().format(atLeastZero(kill.getLootSplitReceived())));
			panel.setUpdateButton(true);
			locked = false;
		});

		teamSizeWrapper.add(teamSizeLabel);
		teamSizeWrapper.add(teamSize);

		varPanel.add(ffaWrapper);
		varPanel.add(Box.createRigidArea(new Dimension(0, 3)));
		varPanel.add(splitReceivedWrapper);
		varPanel.add(Box.createRigidArea(new Dimension(0, 3)));
		varPanel.add(teamSizeWrapper);

		return varPanel;
	}

	private JTextField getTextField()
	{
		JTextField textField = new JTextField();
		textField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		textField.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		return textField;
	}

	private int atLeastZero(int value)
	{
		return Math.max(value, 0);
	}

	private void setFfa()
	{
		if (kill.isSpecialLootInOwnName())
		{
			kill.setLootSplitReceived(kill.getSpecialLootValue());
			kill.setLootSplitPaid(-1);
		}
		else
		{
			kill.setLootSplitPaid(-1);
			kill.setLootSplitReceived(-1);
		}
	}

	private void setSplit()
	{
		int splitSize = kill.getSpecialLootValue() / Math.max(kill.getTeamSize(), 1);
		if (!kill.isFreeForAll())
		{
			if (kill.isSpecialLootInOwnName())
			{
				kill.setLootSplitPaid(splitSize);
			}
			else
			{
				kill.setLootSplitPaid(-1);
			}
			kill.setLootSplitReceived(splitSize);
		}
	}

	private BufferedImage resizeImage(BufferedImage before, double scale)
	{
		return panel.resizeImage(before, scale, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	}

	public static String getDateSection(NexLootTracker kill)
	{
		LocalDate date = Instant.ofEpochMilli(kill.getDate()).atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		LocalDate startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
		YearMonth currentMonth = YearMonth.from(today);
		YearMonth dropMonth = YearMonth.from(date);

		if (date.equals(today))
		{
			return "Today";
		}
		if (date.equals(yesterday))
		{
			return "Yesterday";
		}
		if (!date.isBefore(startOfThisWeek))
		{
			return "This Week";
		}
		if (!date.isBefore(startOfLastWeek))
		{
			return "Last Week";
		}
		if (dropMonth.equals(currentMonth))
		{
			return "This Month";
		}
		if (dropMonth.equals(currentMonth.minusMonths(1)))
		{
			return "Last Month";
		}
		return date.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
	}

	private LocalDate getKillDate()
	{
		return Instant.ofEpochMilli(kill.getDate()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private String getDateText()
	{
		LocalDate date = getKillDate();
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		LocalDate startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
		YearMonth currentMonth = YearMonth.from(today);
		YearMonth dropMonth = YearMonth.from(date);

		if (date.equals(today))
		{
			return "today";
		}
		if (date.equals(yesterday))
		{
			return "yesterday";
		}
		if (!date.isBefore(startOfThisWeek))
		{
			return "this week";
		}
		if (!date.isBefore(startOfLastWeek))
		{
			return "last week";
		}
		if (dropMonth.equals(currentMonth))
		{
			return "this month";
		}
		if (dropMonth.equals(currentMonth.minusMonths(1)))
		{
			return "last month";
		}
		return date.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
	}

	private String getAbsoluteDateText()
	{
		LocalDate date = getKillDate();
		int day = date.getDayOfMonth();
		String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		String dateText = day + getOrdinalSuffix(day) + " " + month + " " + date.getYear();
		if (kill.getCompletionCount() >= 0)
		{
			return dateText + " - " + kill.getCompletionCount() + " KC";
		}
		return dateText;
	}

	private static String getOrdinalSuffix(int day)
	{
		if (day >= 11 && day <= 13)
		{
			return "th";
		}
		switch (day % 10)
		{
			case 1:
				return "st";
			case 2:
				return "nd";
			case 3:
				return "rd";
			default:
				return "th";
		}
	}

	private static final NavigableMap<Long, String> SUFFIXES = new TreeMap<>();

	static
	{
		SUFFIXES.put(1_000L, "k");
		SUFFIXES.put(1_000_000L, "m");
		SUFFIXES.put(1_000_000_000L, "b");
	}

	public static String format(long value)
	{
		if (value == Long.MIN_VALUE)
		{
			return format(Long.MIN_VALUE + 1);
		}
		if (value < 0)
		{
			return "-" + format(-value);
		}
		if (value < 1000)
		{
			return Long.toString(value);
		}

		Map.Entry<Long, String> entry = SUFFIXES.floorEntry(value);
		long divideBy = entry.getKey();
		String suffix = entry.getValue();
		long truncated = value / (divideBy / 10);
		boolean hasDecimal = truncated < 1000;
		return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
	}

	public static int parse(String s)
	{
		if (s == null || s.isEmpty())
		{
			return -5;
		}

		char c = s.charAt(s.length() - 1);
		if (Character.isLetter(c))
		{
			int multiplier;
			if (c == 'k')
			{
				multiplier = 1000;
			}
			else if (c == 'm')
			{
				multiplier = 1_000_000;
			}
			else if (c == 'b')
			{
				multiplier = 1_000_000_000;
			}
			else
			{
				return -5;
			}

			String substr = s.substring(0, s.length() - 1);
			if (isNumeric(substr))
			{
				return (int) Math.round(Double.parseDouble(substr) * multiplier);
			}
		}
		else if (isNumeric(s))
		{
			return Integer.parseInt(s.replace(",", ""));
		}

		return -5;
	}

	private static boolean isNumeric(String str)
	{
		try
		{
			Double.parseDouble(str);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	private String fixSpaces(String text)
	{
		return text == null ? "" : text.replace('\u00A0', ' ');
	}

	interface SimpleDocumentListener extends DocumentListener
	{
		void changed(DocumentEvent e);

		@Override
		default void insertUpdate(DocumentEvent e)
		{
			changed(e);
		}

		@Override
		default void removeUpdate(DocumentEvent e)
		{
			changed(e);
		}

		@Override
		default void changedUpdate(DocumentEvent e)
		{
			changed(e);
		}
	}
}
