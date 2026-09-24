package com.triglav.clan;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/** Side panel: pairing status and the "Poveži račun" button from docs/plugin-brief.md §3. */
@Singleton
public class TriglavPanel extends PluginPanel
{
	private final JLabel statusValue = value();
	private final JLabel codeValue = value();
	private final JButton pairButton = new JButton("Poveži račun");
	private final JTextField gearTitle = new JTextField();

	private Runnable onPair = () ->
	{
	};
	private Consumer<String> onSendGear = title ->
	{
	};

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
		content.add(row("Stanje", statusValue));
		content.add(row("Koda", codeValue));

		content.add(spacer());
		pairButton.setFont(FontManager.getRunescapeSmallFont());
		pairButton.setFocusPainted(false);
		pairButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		pairButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		pairButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		pairButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		pairButton.addActionListener(e -> onPair.run());
		content.add(pairButton);

		content.add(spacer());
		content.add(note("Klikni zgoraj, nato kodo vpiši na clan.kokalj.dev/profil in potrdi."));

		content.add(spacer());
		content.add(header("Gear setup"));
		gearTitle.setFont(FontManager.getRunescapeSmallFont());
		gearTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		gearTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		gearTitle.setToolTipText("Ime setupa na strani (neobvezno)");
		content.add(gearTitle);
		content.add(Box.createVerticalStrut(4));
		final JButton gearButton = button("Pošlji trenutni setup na stran");
		gearButton.addActionListener(e -> onSendGear.accept(gearTitle.getText().trim()));
		content.add(gearButton);
		content.add(Box.createVerticalStrut(4));
		content.add(note("Pošlje opremo in inventar, ki ju imaš zdaj, v gear builder na strani."));

		add(content, BorderLayout.NORTH);
	}

	public void setOnPair(Runnable onPair)
	{
		this.onPair = onPair;
	}

	public void setOnSendGear(Consumer<String> onSendGear)
	{
		this.onSendGear = onSendGear;
	}

	private static JLabel note(String text)
	{
		final JLabel label = new JLabel("<html><body style='width:150px'>" + text + "</body></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static JButton button(String text)
	{
		final JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setFocusPainted(false);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		return button;
	}

	public void update(boolean paired)
	{
		SwingUtilities.invokeLater(() ->
		{
			statusValue.setText(paired ? "povezano" : "ni povezano");
			pairButton.setEnabled(!paired);
		});
	}

	public void showPairingCode(String code)
	{
		SwingUtilities.invokeLater(() -> codeValue.setText(code));
	}

	private static JLabel header(String text)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	private static JLabel value()
	{
		final JLabel label = new JLabel("-");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private JPanel row(String key, JLabel value)
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		final JLabel keyLabel = new JLabel(key);
		keyLabel.setFont(FontManager.getRunescapeSmallFont());
		keyLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);

		panel.add(keyLabel, BorderLayout.WEST);
		panel.add(value, BorderLayout.EAST);
		return panel;
	}

	private static Component spacer()
	{
		final JPanel panel = new JPanel();
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}
}
