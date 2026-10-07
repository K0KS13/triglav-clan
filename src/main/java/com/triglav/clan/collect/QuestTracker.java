package com.triglav.clan.collect;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/**
 * The quest completion scroll (interface 153) is the one reliable signal that a quest is done.
 * Its title wording varies ("You have completed The Corsair Curse!", "'One Small Favour'
 * completed!"), so the name is best effort; the counts the site actually uses for the quest cape
 * milestone come from varbits, which are exact.
 */
@Singleton
public class QuestTracker
{
	private static final Pattern COMPLETED_FIRST = Pattern.compile(".*?completed:? '?(?<quest>.+?)'?[!.]?$");
	private static final Pattern COMPLETED_LAST = Pattern.compile("'?(?<quest>.+?)'? (?:quest )?completed[!.]?$", Pattern.CASE_INSENSITIVE);

	public static final int WIDGET_GROUP = InterfaceID.QUESTSCROLL;

	/** Scroll wording that does not follow the usual pattern (same cases Dink special-cases). */
	private static final Map<String, String> RENAMED = Map.of(
		"Lumbridge Cook... again", "Another Cook's Quest",
		"Skrach 'Bone Crusher' Uglogwee", "Skrach Uglogwee");

	/** "You have kind of completed..." is a partial step, not a finished quest. */
	public boolean isPartial(String title)
	{
		return title != null && title.contains("kind of");
	}

	/** @return the quest scroll's title, or null when it isn't loaded */
	public String title(Client client)
	{
		final Widget title = client.getWidget(InterfaceID.Questscroll.QUEST_TITLE);
		return title == null ? null : Text.removeTags(title.getText()).trim();
	}

	/**
	 * @return the QUEST `extra` payload. Reads varbits, so it must run on the client thread one
	 * tick after the widget loads, by which time the client has applied the new counts.
	 */
	public JsonObject build(Client client, String title)
	{
		final JsonObject extra = new JsonObject();

		final String quest = questName(title);
		if (quest != null)
		{
			extra.addProperty("questName", quest);
		}

		final int completedQuests = client.getVarbitValue(VarbitID.QUESTS_COMPLETED_COUNT);
		final int totalQuests = client.getVarbitValue(VarbitID.QUESTS_TOTAL_COUNT);
		if (completedQuests > 0 && totalQuests > 0)
		{
			extra.addProperty("completedQuests", completedQuests);
			extra.addProperty("totalQuests", totalQuests);
		}

		final int questPoints = client.getVarpValue(VarPlayerID.QP);
		final int totalQuestPoints = client.getVarbitValue(VarbitID.QP_MAX);
		if (questPoints > 0 && totalQuestPoints > 0)
		{
			extra.addProperty("questPoints", questPoints);
			extra.addProperty("totalQuestPoints", totalQuestPoints);
		}

		return extra;
	}

	private static String questName(String title)
	{
		if (title == null || title.isEmpty())
		{
			return null;
		}

		final Matcher first = COMPLETED_FIRST.matcher(title);
		final Matcher last = COMPLETED_LAST.matcher(title);
		final String name = first.matches() ? first.group("quest").trim()
			: last.matches() ? last.group("quest").trim() : title;
		return normalise(name, title);
	}

	/** A partial completion ("...kind of...") is not a finished quest; a few titles read differently from the quest name. */
	static String normalise(String name, String scrollTitle)
	{
		return scrollTitle.contains("kind of") ? null : RENAMED.getOrDefault(name, name);
	}
}
