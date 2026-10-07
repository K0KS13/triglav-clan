package com.triglav.clan;

import com.triglav.clan.overview.Overview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * Side panel: link status, the clan-code field with short instructions and a direct link to the
 * member's profile (where the code is), the gear-setup button and a link to the site.
 */
@Singleton
public class TriglavPanel extends PluginPanel
{
	static final String SITE_URL = "https://clan.kokalj.dev";
	static final String PROFILE_URL = SITE_URL + "/profil";

	private final JLabel statusValue = new JLabel("ni povezano");
	private final JTextField codeField = new JTextField();
	private final JTextField gearTitle = new JTextField();

	private Consumer<String> onLink = code ->
	{
	};
	private Consumer<String> onSendGear = title ->
	{
	};
	private Consumer<String> onShare = text ->
	{
	};
	private Consumer<String> onBuy = itemId ->
	{
	};
	private Runnable onCheckGear = () ->
	{
	};

	/** Rebuilt on every overview refresh: today, goals, deaths, points and shop. */
	private final JPanel live = new JPanel();
	private final JTextField shareText = new JTextField();

	@Inject
	private TriglavPanel()
	{
		super(false);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		content.add(header("TRIGLAV"));
		statusValue.setFont(FontManager.getDefaultFont());
		statusValue.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		statusValue.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(statusValue);

		live.setLayout(new BoxLayout(live, BoxLayout.Y_AXIS));
		live.setBackground(ColorScheme.DARK_GRAY_COLOR);
		live.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(live);

		content.add(spacer());
		content.add(header("Poveži račun"));
		content.add(note("1. Na strani odpri svoj profil in klikni <b>Pokaži mojo kodo</b>."));
		content.add(Box.createVerticalStrut(4));
		final JButton profileButton = button("Odpri moj profil na strani");
		profileButton.addActionListener(e -> LinkBrowser.browse(PROFILE_URL));
		content.add(profileButton);
		content.add(Box.createVerticalStrut(6));
		content.add(note("2. Kodo (TRG-XXXX) vpiši sem in klikni <b>Poveži</b>. Ista koda velja na vseh tvojih računalnikih."));
		content.add(Box.createVerticalStrut(4));
		codeField.setFont(FontManager.getDefaultFont());
		codeField.setAlignmentX(Component.LEFT_ALIGNMENT);
		codeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		codeField.setToolTipText("TRG-XXXX");
		codeField.addActionListener(e -> onLink.accept(codeField.getText().trim()));
		content.add(codeField);
		content.add(Box.createVerticalStrut(4));
		final JButton linkButton = button("Poveži");
		linkButton.addActionListener(e -> onLink.accept(codeField.getText().trim()));
		content.add(linkButton);

		content.add(spacer());
		content.add(header("Povej klanu"));
		shareText.setFont(FontManager.getDefaultFont());
		shareText.setAlignmentX(Component.LEFT_ALIGNMENT);
		shareText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		shareText.setToolTipText("Kratko sporocilo (neobvezno)");
		content.add(shareText);
		content.add(Box.createVerticalStrut(4));
		final JButton shareButton = button("Pošlji screenshot klanu");
		shareButton.addActionListener(e ->
		{
			onShare.accept(shareText.getText().trim());
			shareText.setText("");
		});
		content.add(shareButton);
		content.add(Box.createVerticalStrut(4));
		content.add(note("Posnetek zaslona in tvoje besedilo gresta v Discord klana. Največ 4 objave na uro."));

		content.add(spacer());
		content.add(header("Gear setup"));
		gearTitle.setFont(FontManager.getDefaultFont());
		gearTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		gearTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		gearTitle.setToolTipText("Ime setupa na strani (neobvezno)");
		content.add(gearTitle);
		content.add(Box.createVerticalStrut(4));
		final JButton gearButton = button("Pošlji setup na stran");
		gearButton.addActionListener(e -> onSendGear.accept(gearTitle.getText().trim()));
		content.add(gearButton);
		content.add(Box.createVerticalStrut(4));
		content.add(note("Pošlje opremo in inventar, ki ju imaš zdaj, v gear builder na strani."));
		content.add(Box.createVerticalStrut(6));
		final JButton checkButton = button("Preveri opremo za LFG");
		checkButton.addActionListener(e -> onCheckGear.run());
		content.add(checkButton);
		content.add(Box.createVerticalStrut(4));
		content.add(note("Primerja, kar nosiš, s setupom LFG-ja, v katerega si prijavljen."));

		content.add(spacer());
		final JButton siteButton = button("Odpri clan.kokalj.dev");
		siteButton.addActionListener(e -> LinkBrowser.browse(SITE_URL));
		content.add(siteButton);

		add(content, BorderLayout.NORTH);
	}

	public void setOnLink(Consumer<String> onLink)
	{
		this.onLink = onLink;
	}

	public void setOnSendGear(Consumer<String> onSendGear)
	{
		this.onSendGear = onSendGear;
	}

	public void setOnShare(Consumer<String> onShare)
	{
		this.onShare = onShare;
	}

	public void setOnBuy(Consumer<String> onBuy)
	{
		this.onBuy = onBuy;
	}

	public void setOnCheckGear(Runnable onCheckGear)
	{
		this.onCheckGear = onCheckGear;
	}

	/** Redraws the live sections from the latest overview; safe to call from any thread. */
	public void showOverview(Overview overview, boolean linked)
	{
		SwingUtilities.invokeLater(() ->
		{
			live.removeAll();
			if (linked)
			{
				addLive(overview);
			}
			live.revalidate();
			live.repaint();
		});
	}

	private void addLive(Overview o)
	{
		live.add(spacer());
		live.add(header("Točke: " + o.points));

		if (!o.events.isEmpty() || !o.lfg.isEmpty())
		{
			live.add(spacer());
			live.add(header("Danes"));
			for (Overview.Event e : o.events)
			{
				live.add(line(when(e.startsAt) + "  " + e.title + (e.mine ? "  (prijavljen)" : "") + "  · " + e.going + " gre"));
			}
			for (Overview.Lfg l : o.lfg)
			{
				live.add(line(when(l.startsAt) + "  LFG " + l.title + "  · " + l.taken + "/" + l.capacity + (l.mine ? "  (si noter)" : "")));
			}
		}

		if (!o.goals.isEmpty())
		{
			live.add(spacer());
			live.add(header("Cilji klana"));
			for (Overview.Goal g : o.goals)
			{
				live.add(line(g.title));
				final JProgressBar bar = new JProgressBar(0, Math.max(1, g.target));
				bar.setValue(Math.min(g.current, g.target));
				bar.setStringPainted(true);
				bar.setString(g.current + " / " + g.target);
				bar.setAlignmentX(Component.LEFT_ALIGNMENT);
				bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
				live.add(bar);
				live.add(Box.createVerticalStrut(4));
			}
		}

		live.add(spacer());
		live.add(header("Smrti ta mesec"));
		live.add(line(o.deathsThisMonth == 0 ? "ni jih" : o.deathsThisMonth + "× · izgubljeno " + gp(o.valueLostThisMonth)));

		if (!o.shop.isEmpty())
		{
			live.add(spacer());
			live.add(header("Trgovina"));
			for (Overview.ShopItem item : o.shop)
			{
				final JButton buy = button(item.name + " · " + item.cost);
				buy.setEnabled(item.affordable);
				buy.setToolTipText(item.affordable ? "Kupi (vodstvo potrdi)" : "Premalo točk");
				buy.addActionListener(e -> onBuy.accept(item.id));
				live.add(buy);
				live.add(Box.createVerticalStrut(3));
			}
			live.add(note("Nazive in barve izbereš na strani."));
		}
	}

	private static String when(Instant at)
	{
		final long minutes = Duration.between(Instant.now(), at).toMinutes();
		return minutes <= 0 ? "zdaj" : minutes < 60 ? "čez " + minutes + " min" : DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(at);
	}

	private static String gp(long value)
	{
		return value >= 1_000_000 ? String.format("%.1fM", value / 1_000_000.0) : value >= 1_000 ? (value / 1_000) + "k" : String.valueOf(value);
	}

	private static JLabel line(String text)
	{
		final JLabel label = new JLabel("<html><body style='width:150px'>" + text.replace("<", "&lt;") + "</body></html>");
		label.setFont(FontManager.getDefaultFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	/** @param name Discord name the code belongs to, or null if unknown this session */
	public void showLinked(boolean linked, String name)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (!linked)
			{
				statusValue.setText("ni povezano");
				statusValue.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
				return;
			}
			statusValue.setText(name == null ? "povezano" : "povezano: " + name);
			statusValue.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
			codeField.setText("");
		});
	}

	private static JLabel header(String text)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getDefaultBoldFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	private static JLabel note(String html)
	{
		final JLabel label = new JLabel("<html><body style='width:150px'>" + html + "</body></html>");
		label.setFont(FontManager.getDefaultFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static JButton button(String text)
	{
		final JButton button = new JButton(text);
		button.setFont(FontManager.getDefaultFont());
		button.setFocusPainted(false);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		return button;
	}

	private static Component spacer()
	{
		final JPanel panel = new JPanel();
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}
}
