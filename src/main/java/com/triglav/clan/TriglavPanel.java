package com.triglav.clan;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

	private Runnable onPair = () ->
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
		final JLabel hint = new JLabel(
			"<html><body style='width:150px'>Ali na strani /profil prilepi kodo, ki jo dobiš s klikom zgoraj.</body></html>");
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		hint.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(hint);

		add(content, BorderLayout.NORTH);
	}

	public void setOnPair(Runnable onPair)
	{
		this.onPair = onPair;
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
