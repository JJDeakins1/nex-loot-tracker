package com.nexloottracker.ui;

import com.nexloottracker.NexLootTracker;
import com.nexloottracker.NexLootTrackerConfig;
import com.nexloottracker.NexLootTrackerItem;
import com.nexloottracker.NexUniques;
import com.nexloottracker.WorldUtils;
import com.nexloottracker.filereadwriter.FileReadWriter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class NexLootTrackerPanel extends PluginPanel
{
	private final ItemManager itemManager;
	private final FileReadWriter fileReadWriter;
	private final NexLootTrackerConfig config;
	private final Client client;

	@Setter
	private ArrayList<NexLootTracker> killList = new ArrayList<>();
	private final HashMap<String, NexLootTracker> uuidMap = new LinkedHashMap<>();

	@Setter
	private boolean loaded = false;

	private final JPanel contentPanel = new JPanel();
	private JButton updateButton;

	@Setter
	private String dateFilter = "All Time";
	@Setter
	private String mvpFilter = "Both";
	@Setter
	private String teamSizeFilter = "All Sizes";
	@Setter
	private String splitFilter = "Both";

	private static final EnumSet<NexUniques> NEX_UNIQUES = EnumSet.allOf(NexUniques.class);

	public NexLootTrackerPanel(
		final ItemManager itemManager,
		final FileReadWriter fileReadWriter,
		final NexLootTrackerConfig config,
		final Client client
	)
	{
		this.itemManager = itemManager;
		this.fileReadWriter = fileReadWriter;
		this.config = config;
		this.client = client;

		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		add(contentPanel, BorderLayout.NORTH);
		updateView();
	}

	public void loadKillList()
	{
		if (WorldUtils.playerOnBetaWorld(client))
		{
			showDisabledView();
			return;
		}

		if (!fileReadWriter.ensureProfile(client.getUsername(), client.getAccountHash()))
		{
			return;
		}

		killList = fileReadWriter.readFromFile();
		uuidMap.clear();
		for (NexLootTracker kill : killList)
		{
			uuidMap.put(kill.getUniqueID(), kill);
		}
		loaded = true;
		log.info("Nex Loot Tracker panel refreshed with {} kills", killList.size());
		updateView();
	}

	public void addKill(NexLootTracker kill, boolean updateList)
	{
		uuidMap.put(kill.getUniqueID(), kill);
		if (updateList)
		{
			killList = new ArrayList<>(uuidMap.values());
		}
		else
		{
			killList.add(kill);
		}
		updateView();
	}

	public void setUpdateButton(boolean enabled)
	{
		if (updateButton != null)
		{
			updateButton.setEnabled(enabled);
			updateButton.setToolTipText(enabled ? "Save split changes" : "Nothing to update");
		}
	}

	public void updateView()
	{
		contentPanel.removeAll();

		if (WorldUtils.playerOnBetaWorld(client))
		{
			showDisabledView();
			return;
		}

		if (config.showTitle())
		{
			contentPanel.add(buildTitlePanel());
			contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		if (config.showFilters())
		{
			contentPanel.add(buildFilterPanel());
			contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		if (config.showKillsLogged())
		{
			contentPanel.add(buildKillsLoggedPanel());
			contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		if (config.showUniquesTable())
		{
			contentPanel.add(buildUniquesPanel());
			contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		if (config.showSplitGPEarned())
		{
			contentPanel.add(buildSplitGpPanel());
			contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		if (config.showRegularDrops())
		{
			contentPanel.add(buildRegularDropsPanel());
			contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		if (config.showSplitChanger())
		{
			contentPanel.add(buildSplitChangerPanel());
		}

		contentPanel.revalidate();
		contentPanel.repaint();
	}

	private void showDisabledView()
	{
		contentPanel.removeAll();
		JPanel title = new JPanel();
		JLabel label = new JLabel("Tracker Disabled on Beta Worlds");
		label.setForeground(Color.WHITE);
		title.add(label);
		contentPanel.add(title);
		contentPanel.revalidate();
		contentPanel.repaint();
	}

	private JPanel buildTitlePanel()
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		wrapper.setBorder(new EmptyBorder(10, 8, 10, 8));

		JLabel title = new JLabel("Nex Loot Tracker", SwingConstants.CENTER);
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeFont());
		wrapper.add(title, BorderLayout.CENTER);
		return wrapper;
	}

	private JPanel buildKillsLoggedPanel()
	{
		JPanel wrapper = new JPanel(new GridLayout(0, 2));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		wrapper.setBorder(new EmptyBorder(5, 10, 5, 10));

		JLabel label = textPanel("Kills Logged:");
		label.setHorizontalAlignment(SwingConstants.LEFT);
		JLabel value = textPanel(String.valueOf(getDistinctKills(getFilteredKillList()).size()));
		value.setHorizontalAlignment(SwingConstants.RIGHT);
		value.setForeground(Color.LIGHT_GRAY);

		wrapper.add(label);
		wrapper.add(value);
		return wrapper;
	}

	private JPanel buildFilterPanel()
	{
		JPanel wrapper = new JPanel(new GridBagLayout());
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		wrapper.setBorder(new EmptyBorder(5, 5, 5, 5));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;

		JLabel filterLabel = textPanel("Filter kills logged");
		filterLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
		wrapper.add(filterLabel, c);

		c.gridy++;

		JComboBox<String> dateChoices = comboBox(new String[]{
			"All Time", "12 Hours", "Today", "3 Days", "Week", "Month", "3 Months", "Year", "X Kills"
		}, dateFilter, selected -> dateFilter = selected);

		JComboBox<String> mvpChoices = comboBox(new String[]{"Both", "My MVP", "Not My MVP"}, mvpFilter, selected -> mvpFilter = selected);

		JComboBox<String> teamChoices = comboBox(new String[]{
			"All Sizes", "Solo", "Duo", "Trio", "4-Man", "5-Man"
		}, teamSizeFilter, selected -> teamSizeFilter = selected);

		JComboBox<String> splitChoices = comboBox(new String[]{"Both", "Split Only", "FFA Only"}, splitFilter, selected -> splitFilter = selected);

		c.gridx = 0;
		wrapper.add(dateChoices, c);
		c.gridx = 1;
		wrapper.add(mvpChoices, c);
		c.gridy++;
		c.gridx = 0;
		wrapper.add(teamChoices, c);
		c.gridx = 1;
		wrapper.add(splitChoices, c);

		return wrapper;
	}

	private JComboBox<String> comboBox(String[] options, String selected, java.util.function.Consumer<String> onChange)
	{
		JComboBox<String> comboBox = new JComboBox<>(options);
		comboBox.setSelectedItem(selected);
		comboBox.setFocusable(false);
		comboBox.addActionListener(e ->
		{
			onChange.accept(comboBox.getSelectedItem().toString());
			if (loaded)
			{
				updateView();
			}
		});
		return comboBox;
	}

	private JPanel buildUniquesPanel()
	{
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel table = new JPanel(new GridLayout(0, 4));
		table.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		table.add(headerCell(""));
		table.add(headerCell("Own"));
		table.add(headerCell("Seen"));
		table.add(headerCell("Dry"));

		int totalOwn = 0;
		int totalSeen = 0;
		int dryStreak = getDryStreak();

		for (NexUniques unique : NEX_UNIQUES)
		{
			if (unique == NexUniques.NEXLING)
			{
				ArrayList<NexLootTracker> pets = filterPetReceivers();
				ArrayList<NexLootTracker> ownPets = filterOwnPets(pets);
				addUniqueRowCells(table, unique, ownPets.size(), pets.size(), -1);
				totalOwn += ownPets.size();
				totalSeen += pets.size();
				continue;
			}

			ArrayList<NexLootTracker> seen = filterByUniqueName(unique.getName());
			ArrayList<NexLootTracker> own = filterOwnDrops(seen);
			addUniqueRowCells(table, unique, own.size(), seen.size(), -1);
			totalOwn += own.size();
			totalSeen += seen.size();
		}

		table.add(totalCell("Total:", SwingConstants.LEFT));
		table.add(totalCell(String.valueOf(totalOwn), SwingConstants.CENTER));
		table.add(totalCell(String.valueOf(totalSeen), SwingConstants.CENTER));
		table.add(totalCell(String.valueOf(dryStreak), SwingConstants.CENTER));

		wrapper.add(table);
		return wrapper;
	}

	private JLabel headerCell(String text)
	{
		JLabel label = centeredLabel(text);
		label.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		label.setOpaque(true);
		label.setBorder(new EmptyBorder(3, 3, 3, 3));
		return label;
	}

	private JLabel totalCell(String text, int alignment)
	{
		JLabel label = textPanel(text);
		label.setHorizontalAlignment(alignment);
		label.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		label.setOpaque(true);
		label.setBorder(new EmptyBorder(3, 3, 3, 3));
		return label;
	}

	private void addUniqueRowCells(JPanel table, NexUniques unique, int own, int seen, int dry)
	{
		AsyncBufferedImage image = itemManager.getImage(unique.getItemId(), 1, false);
		JLabel icon = new JLabel();
		icon.setIcon(new ImageIcon(resizeImage(image, 0.7, AffineTransformOp.TYPE_BILINEAR)));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setToolTipText(getUniqueToolTip(unique, seen, own));
		image.onLoaded(() ->
		{
			icon.setIcon(new ImageIcon(resizeImage(image, 0.7, AffineTransformOp.TYPE_BILINEAR)));
			icon.revalidate();
			icon.repaint();
		});

		JLabel ownLabel = centeredLabel(String.valueOf(own));
		JLabel seenLabel = centeredLabel(String.valueOf(seen));
		JLabel dryLabel = centeredLabel(dry >= 0 ? String.valueOf(dry) : "-");

		MatteBorder border = new MatteBorder(0, 0, 1, 1, ColorScheme.LIGHT_GRAY_COLOR.darker());
		icon.setBorder(border);
		ownLabel.setBorder(border);
		seenLabel.setBorder(border);
		dryLabel.setBorder(new MatteBorder(0, 0, 1, 0, ColorScheme.LIGHT_GRAY_COLOR.darker()));

		table.add(icon);
		table.add(ownLabel);
		table.add(seenLabel);
		table.add(dryLabel);
	}

	private JPanel buildSplitGpPanel()
	{
		long splitGp = getFilteredKillList().stream()
			.mapToLong(kill -> Math.max(kill.getLootSplitReceived(), 0))
			.sum();

		JPanel wrapper = new JPanel(new GridLayout(0, 2));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		wrapper.setBorder(new EmptyBorder(5, 10, 5, 10));

		JLabel label = textPanel("Split GP Earned:");
		label.setHorizontalAlignment(SwingConstants.LEFT);
		JLabel value = textPanel(SplitChanger.format(splitGp) + " gp");
		value.setHorizontalAlignment(SwingConstants.RIGHT);
		value.setForeground(Color.LIGHT_GRAY);
		value.setToolTipText(NumberFormat.getInstance().format(splitGp));

		wrapper.add(label);
		wrapper.add(value);
		return wrapper;
	}

	private JPanel buildRegularDropsPanel()
	{
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		Map<Integer, NexLootTrackerItem> aggregated = new LinkedHashMap<>();

		for (NexLootTracker kill : getFilteredKillList())
		{
			for (NexLootTrackerItem item : kill.getLootList())
			{
				if (NexUniques.isUniqueItemId(item.getId()))
				{
					continue;
				}

				NexLootTrackerItem existing = aggregated.get(item.getId());
				if (existing == null)
				{
					NexLootTrackerItem copy = new NexLootTrackerItem();
					copy.setId(item.getId());
					copy.setName(item.getName());
					copy.setQuantity(item.getQuantity());
					copy.setPrice(item.getPrice());
					aggregated.put(item.getId(), copy);
				}
				else
				{
					existing.setQuantity(existing.getQuantity() + item.getQuantity());
					existing.setPrice(existing.getPrice() + item.getPrice());
				}
			}
		}

		if (aggregated.isEmpty())
		{
			return wrapper;
		}

		ArrayList<NexLootTrackerItem> regularDropsList = new ArrayList<>(aggregated.values());
		regularDropsList.sort((a, b) -> Integer.compare(b.getPrice(), a.getPrice()));
		int regularDropsSum = regularDropsList.stream().mapToInt(NexLootTrackerItem::getPrice).sum();

		JPanel title = new JPanel(new GridLayout(0, 2));
		title.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		title.setBorder(new EmptyBorder(3, 20, 3, 10));
		title.add(textPanel("Regular Drops"));
		JLabel valueLabel = textPanel(SplitChanger.format(regularDropsSum) + " gp");
		valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		valueLabel.setForeground(Color.LIGHT_GRAY.darker());
		valueLabel.setToolTipText(NumberFormat.getInstance().format(regularDropsSum));
		title.add(valueLabel);
		wrapper.add(title);

		JPanel drops = new JPanel(new GridLayout(0, 5));
		drops.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		for (NexLootTrackerItem drop : regularDropsList)
		{
			AsyncBufferedImage image = itemManager.getImage(drop.getId(), drop.getQuantity(), drop.getQuantity() > 1);

			JPanel iconWrapper = new JPanel(new BorderLayout());
			iconWrapper.setPreferredSize(new Dimension(40, 40));
			iconWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			iconWrapper.setBorder(new MatteBorder(1, 0, 0, 1, ColorScheme.DARK_GRAY_COLOR));
			iconWrapper.setToolTipText(getRegularToolTip(drop));

			JLabel icon = new JLabel();
			icon.setBorder(new EmptyBorder(0, 5, 0, 0));
			image.addTo(icon);
			image.onLoaded(() ->
			{
				image.addTo(icon);
				icon.revalidate();
				icon.repaint();
			});

			iconWrapper.add(icon, BorderLayout.CENTER);
			drops.add(iconWrapper);
		}

		wrapper.add(drops);
		return wrapper;
	}

	private JPanel buildSplitChangerPanel()
	{
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

		JPanel titleWrapper = new JPanel(new GridBagLayout());
		titleWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		titleWrapper.setBorder(new EmptyBorder(3, 3, 3, 3));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;

		JLabel title = textPanel("Change Purple Splits");
		title.setBorder(new EmptyBorder(0, 5, 0, 0));
		titleWrapper.add(title, c);

		updateButton = new JButton("Update");
		updateButton.setFont(FontManager.getRunescapeSmallFont());
		updateButton.setPreferredSize(new Dimension(60, 20));
		updateButton.setEnabled(false);
		updateButton.setFocusPainted(false);
		updateButton.setToolTipText("Nothing to update");
		updateButton.addActionListener(e ->
		{
			ArrayList<SplitChanger> changers = new ArrayList<>();
			for (java.awt.Component component : wrapper.getComponents())
			{
				if (component instanceof SplitChanger)
				{
					changers.add((SplitChanger) component);
				}
			}

			for (SplitChanger changer : changers)
			{
				uuidMap.put(changer.getKill().getUniqueID(), changer.getKill());
			}

			killList = new ArrayList<>(uuidMap.values());
			fileReadWriter.updateKillList(killList);
			setUpdateButton(false);
			updateView();
		});

		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		titleWrapper.add(updateButton, c);
		wrapper.add(titleWrapper);
		wrapper.add(Box.createRigidArea(new Dimension(0, 2)));

		if (loaded)
		{
			ArrayList<NexLootTracker> purples = filterPurples();
			purples.sort((a, b) -> Long.compare(b.getDate(), a.getDate()));

			for (int i = 0; i < Math.min(purples.size(), 10); i++)
			{
				wrapper.add(new SplitChanger(itemManager, purples.get(i), this));
				wrapper.add(Box.createRigidArea(new Dimension(0, 7)));
			}
		}

		return wrapper;
	}

	private ArrayList<NexLootTracker> getFilteredKillList()
	{
		if (!loaded)
		{
			return new ArrayList<>();
		}

		ArrayList<NexLootTracker> filtered = new ArrayList<>(killList);

		switch (mvpFilter)
		{
			case "My MVP":
				filtered = filtered.stream().filter(NexLootTracker::isMvpInOwnName).collect(Collectors.toCollection(ArrayList::new));
				break;
			case "Not My MVP":
				filtered = filtered.stream().filter(kill -> !kill.isMvpInOwnName()).collect(Collectors.toCollection(ArrayList::new));
				break;
		}

		switch (teamSizeFilter)
		{
			case "Solo":
				filtered = filtered.stream().filter(kill -> kill.getTeamSize() == 1).collect(Collectors.toCollection(ArrayList::new));
				break;
			case "Duo":
				filtered = filtered.stream().filter(kill -> kill.getTeamSize() == 2).collect(Collectors.toCollection(ArrayList::new));
				break;
			case "Trio":
				filtered = filtered.stream().filter(kill -> kill.getTeamSize() == 3).collect(Collectors.toCollection(ArrayList::new));
				break;
			case "4-Man":
				filtered = filtered.stream().filter(kill -> kill.getTeamSize() == 4).collect(Collectors.toCollection(ArrayList::new));
				break;
			case "5-Man":
				filtered = filtered.stream().filter(kill -> kill.getTeamSize() == 5).collect(Collectors.toCollection(ArrayList::new));
				break;
		}

		switch (splitFilter)
		{
			case "Split Only":
				filtered = filtered.stream().filter(kill -> !kill.isFreeForAll()).collect(Collectors.toCollection(ArrayList::new));
				break;
			case "FFA Only":
				filtered = filtered.stream().filter(NexLootTracker::isFreeForAll).collect(Collectors.toCollection(ArrayList::new));
				break;
		}

		long now = System.currentTimeMillis();
		switch (dateFilter)
		{
			case "12 Hours":
				return filterByDate(filtered, now - 43_200_000L);
			case "Today":
				return filterByDate(filtered, now - 86_400_000L);
			case "3 Days":
				return filterByDate(filtered, now - 259_200_000L);
			case "Week":
				return filterByDate(filtered, now - 604_800_000L);
			case "Month":
				return filterByDate(filtered, now - 2_629_746_000L);
			case "3 Months":
				return filterByDate(filtered, now - 7_889_400_000L);
			case "Year":
				return filterByDate(filtered, now - 31_536_000_000L);
			case "X Kills":
				ArrayList<NexLootTracker> distinct = getDistinctKills(filtered);
				ArrayList<NexLootTracker> lastKills = new ArrayList<>(distinct.subList(
					Math.max(distinct.size() - config.lastXKills(), 0), distinct.size()
				));
				return filtered.stream()
					.filter(kill -> lastKills.stream().anyMatch(last -> last.getKillCountID().equals(kill.getKillCountID())))
					.collect(Collectors.toCollection(ArrayList::new));
			default:
				return filtered;
		}
	}

	private ArrayList<NexLootTracker> filterByDate(ArrayList<NexLootTracker> list, long since)
	{
		return list.stream().filter(kill -> kill.getDate() > since).collect(Collectors.toCollection(ArrayList::new));
	}

	private ArrayList<NexLootTracker> getDistinctKills(ArrayList<NexLootTracker> list)
	{
		LinkedHashMap<String, NexLootTracker> distinct = new LinkedHashMap<>();
		for (NexLootTracker kill : list)
		{
			distinct.put(kill.getKillCountID(), kill);
		}
		return new ArrayList<>(distinct.values());
	}

	private ArrayList<NexLootTracker> filterByUniqueName(String name)
	{
		return getFilteredKillList().stream()
			.filter(kill -> name.equalsIgnoreCase(kill.getSpecialLoot()))
			.collect(Collectors.toCollection(ArrayList::new));
	}

	private ArrayList<NexLootTracker> filterOwnDrops(ArrayList<NexLootTracker> list)
	{
		return list.stream().filter(NexLootTracker::isSpecialLootInOwnName).collect(Collectors.toCollection(ArrayList::new));
	}

	private ArrayList<NexLootTracker> filterPetReceivers()
	{
		return getFilteredKillList().stream()
			.filter(kill -> !kill.getPetReceiver().isEmpty())
			.collect(Collectors.toCollection(ArrayList::new));
	}

	private ArrayList<NexLootTracker> filterOwnPets(ArrayList<NexLootTracker> list)
	{
		return list.stream().filter(NexLootTracker::isPetInMyName).collect(Collectors.toCollection(ArrayList::new));
	}

	private ArrayList<NexLootTracker> filterPurples()
	{
		return getFilteredKillList().stream()
			.filter(kill ->
			{
				for (NexUniques unique : NEX_UNIQUES)
				{
					if (unique == NexUniques.NEXLING)
					{
						continue;
					}
					if (unique.getName().equalsIgnoreCase(kill.getSpecialLoot()))
					{
						return true;
					}
				}
				return false;
			})
			.collect(Collectors.toCollection(ArrayList::new));
	}

	private int getDryStreak()
	{
		ArrayList<NexLootTracker> distinct = getDistinctKills(new ArrayList<>(killList));
		distinct.sort((a, b) -> Long.compare(b.getDate(), a.getDate()));

		int streak = 0;
		for (NexLootTracker kill : distinct)
		{
			boolean hasUnique = !kill.getSpecialLoot().isEmpty() || !kill.getPetReceiver().isEmpty();
			if (hasUnique)
			{
				break;
			}
			streak++;
		}
		return streak;
	}

	public JLabel textPanel(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		label.setFont(FontManager.getRunescapeSmallFont());
		return label;
	}

	private JLabel centeredLabel(String text)
	{
		JLabel label = textPanel(text);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		return label;
	}

	public BufferedImage resizeImage(BufferedImage image, double scale, int transformType)
	{
		if (image == null)
		{
			return null;
		}

		int width = (int) (image.getWidth() * scale);
		int height = (int) (image.getHeight() * scale);
		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
		AffineTransformOp op = new AffineTransformOp(transform, transformType);
		op.filter(image, resized);
		return resized;
	}

	private String getUniqueToolTip(NexUniques unique, int seen, int received)
	{
		return "<html>" + unique.getName() + "<br>Received: " + received + "x<br>Seen: " + seen + "x";
	}

	private String getRegularToolTip(NexLootTrackerItem drop)
	{
		return "<html>" + drop.getName() + " x " + drop.getQuantity() + "<br>Price: " + SplitChanger.format(drop.getPrice()) + " gp";
	}

	public void clearData()
	{
		int delete = JOptionPane.showConfirmDialog(
			getRootPane(),
			"<html>Are you sure you want to clear all Nex loot data?<br/>There is no way to undo this action.</html>",
			"Warning",
			JOptionPane.YES_NO_OPTION
		);

		if (delete == JOptionPane.YES_OPTION)
		{
			if (!fileReadWriter.delete())
			{
				JOptionPane.showMessageDialog(getRootPane(), "Unable to clear stored data, please try again.");
				return;
			}
			loadKillList();
		}
	}
}
