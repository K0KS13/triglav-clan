# TRIGLAV Clan

RuneLite plugin for the TRIGLAV OSRS clan ([clan.kokalj.dev](https://clan.kokalj.dev)). One short code links your
account to the clan site — no webhook URLs to copy, unlike Dink.

## What it does today

- **Linking** — every member has a permanent code `TRG-XXXX` on their profile at clan.kokalj.dev. Type it into the
  TRIGLAV side panel and click *Poveži*. The same code works on every computer you play on.
- **LOGIN/LOGOUT** — sends your current skill levels and XP on login, so your site profile stays fresh.
- **LOOT** — reports drops (item, quantity, GE price, kill count parsed from the kill-count chat message),
  with a screenshot once the drop clears the site's configured threshold. NPC kills come straight from RuneLite;
  raid chests, clue caskets, implings and pickpocketing come through the Loot Tracker plugin's `LootReceived`
  event, so Loot Tracker has to stay enabled for those (it is on by default).
- **Levels** — a `LEVEL` event the moment a skill goes up, carrying every skill's level and your combat level.
- **Kill counts** — `KILL_COUNT` with the boss, the count, the fight duration and your personal best, read from
  the game messages; the site keeps them as boss records.
- **Combat achievements** — the completed task and tier, with the point total and current reward tier read from
  varbits (the site uses the tier for automatic rank recommendations).
- **Clues** — how many Treasure Trails of each tier you have finished. The casket's contents arrive separately as
  normal loot, so they show up in the drop feed without being counted twice.
- **Quests** — quest name, quests completed and quest points, read off the completion scroll.
- **PK** — in the wilderness and on PvP worlds, a `PLAYER_KILL` with the victim's name, combat level and the GE
  value of the gear they were wearing. Safe minigames (Castle Wars, Soul Wars, LMS, PvP Arena) are never reported.
- **Clan rank sync** — reads the in-game clan tab and reports members' ranks to the site, so leadership stops
  retyping them by hand.
- **In-game feed** — short site notifications (upcoming events, confirmed bingo tiles, new LFG posts, approved
  shop orders) printed to the game chat.
- **Slayer** — task streak and slayer points on every login, plus a `SLAYER` event with the finished task and
  your running task total when a slayer master hands out points.
- **Achievement diary** — how many diary tiers are complete, on every login.
- **Collection log** — a `COLLECTION` event the moment a new item unlocks, with your log progress (`n / total`) read
  from the same counters the log's header uses; the login snapshot carries it too, once the client knows it.
- **XP milestones** — an `XP_MILESTONE` each time a skill passes another 50M XP.
- **Pets** — a `PET` event when a pet is unlocked (name read off the NPC that spawns next to you).
- **Deaths** — an approximate GP value lost, from the equipment+inventory value just before and just after death.
- **Bingo board** — during an active clan bingo, your team's board as an in-game overlay (green = approved,
  yellow = waiting for staff). It refreshes shortly after a drop that matches an open tile. Move it with Alt+drag.
- **Today in the panel** — events and open LFG posts for the next 24 hours, the clan's shared goals (e.g. "10,000
  Vorkath KC together") as progress bars, your deaths this month with the value lost, your points, and a few
  shop rewards you can buy with one click (titles and name colours stay on the site, they need a choice).
- **Which boss, and where** — the countdown box next to the minimap shows the boss's own picture when an LFG or event
  names a boss (Zulrah, Vorkath, ToA …), the skill icon for a skill competition and an event-type icon otherwise. The
  site resizes and caches the images, so the plugin only talks to the clan site. If the post has a meeting spot, it is marked on
  the world map (hover for the name, click to jump there), as a gold pin on the minimap while it is in view, and
  named in the chat reminder. The spot is picked on a map on the website, or *zbor je tu, kjer stojim* in the
  *Nov LFG* form uses where you stand.
- **Clan events on the minimap** — a countdown next to the minimap for clan events you signed up for (reminder in
  chat ten minutes before), and while any clan event is running (a BOTW or SOTW) a box with the time left.
- **Join an LFG from the game** — the open LFG posts listed under *Danes* in the panel have a *Pridruži se* /
  *Odjavi se* button, with the same rules as the website and Discord buttons (full posts and your own are not joinable).
- **Post an LFG from the game** — *Nov LFG* in the panel: what, when (in 5 minutes to 2 hours), how many spots, and
  optionally your current setup as the recommended gear. It goes to the clan's Discord with the join button, the same as a
  post made on the website. At most three an hour.
- **LFG reminders** — once you have joined an LFG, a chat reminder ten minutes before it starts, a check of what you
  carry against the setup the post recommends ("missing: Abyssal whip, Shark x2"), and a countdown next to the
  minimap from half an hour out. *Check gear for LFG* in the panel runs the same check on demand.
- **Tell the clan** — a panel button that posts a screenshot and a short line to the clan's Discord. At most four
  posts an hour.
- **Bingo confirmations** — a chat line the moment one of your team's tiles is confirmed.
- **Auto gear (off by default)** — *Samodejno pošlji setup*: half a minute after you change equipment, the setup is
  uploaded to one record on the site (it overwrites itself, it never piles up).
- **Gear setup** — *Pošlji trenutni setup na stran* in the panel sends your worn equipment, inventory and
  spellbook to the site's gear builder and prints the link in chat.

## Not implemented

- **Drop rarity** (`1/N` next to a drop): needs the drop-rate tables Dink ships and maintains.
- **Pet name from the game**: there is no reliable source in the API, so it is still read off the NPC that spawns next to you.
- **Region names** on deaths: `regionId` is now sent with every event, but the API has no ID → name table.
- **Group storage contents**: nothing on the site reads it.

## Configuration

The clan code is entered only in the side panel. The plugin settings show just four switches — send screenshots, show in-game notifications, send
deaths, show the bingo board. Everything else (screenshot threshold and so on) comes from the site: the plugin
re-reads it on start, right after linking and every hour, so a change on the site needs no plugin update.

## Privacy

- The **clan code** works like a password, not a shareable ID — whoever has it can send fake events to the
  site under your name. Never share it or show it on stream; if it leaks, replace it on `/profil` (the old one
  stops working instantly). The site rate-limits wrong codes and alerts staff when someone is guessing.
- Only what's listed under "What it does today" is sent, and only to `clan.kokalj.dev` — no private messages and
  no bank contents beyond what a loot/death event needs. As with any HTTP request, that server also sees the IP
  address you connect from.
- **Screenshots**, when enabled, show your username, chat and whatever's in your inventory/equipment at that
  moment.

## For clan members

See the "Poveži TRIGLAV plugin" section on `/profil` on the site for setup instructions once the plugin is
available in the Plugin Hub.

## License

BSD 2-Clause, see `LICENSE`.
