package com.triglav.clan.collect;

import com.google.gson.JsonObject;
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
		if (first.matches())
		{
			return first.group("quest").trim();
		}

		final Matcher last = COMPLETED_LAST.matcher(title);
		return last.matches() ? last.group("quest").trim() : title;
	}
}
