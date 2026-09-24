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
