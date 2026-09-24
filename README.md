# TRIGLAV Clan

RuneLite plugin for the TRIGLAV OSRS clan ([clan.kokalj.dev](https://clan.kokalj.dev)). One short code links your
account to the clan site — no webhook URLs to copy, unlike Dink.

## What it does today

- **Linking** — every member has a permanent code `TRG-XXXX` on their profile at clan.kokalj.dev. Type it into the
  TRIGLAV side panel and click *Poveži*. The same code works on every computer you play on.
- **LOGIN/LOGOUT** — sends your current skill levels and XP on login, so your site profile stays fresh.
- **LOOT** — reports NPC drops (item, quantity, GE price, kill count parsed from the kill-count chat message),
  with a screenshot once the drop clears the site's configured threshold.
- **Clan rank sync** — reads the in-game clan tab and reports members' ranks to the site, so leadership stops
  retyping them by hand.
- **In-game feed** — short site notifications (upcoming events, confirmed bingo tiles, new LFG posts, approved
  shop orders) printed to the game chat.
- **Slayer** — task streak and slayer points on every login.
- **Achievement diary** — how many diary tiers are complete, on every login.
- **Collection log** — a `COLLECTION` event the moment a new item unlocks.
- **Pets** — a `PET` event when a pet is unlocked (name read off the NPC that spawns next to you).
- **Deaths** — an approximate GP value lost, from the equipment+inventory value just before and just after death.
- **Bingo board** — during an active clan bingo, your team's board as an in-game overlay (green = approved,
  yellow = waiting for staff). It refreshes shortly after a drop that matches an open tile. Move it with Alt+drag.
- **Gear setup** — *Pošlji trenutni setup na stran* in the panel sends your worn equipment, inventory and
  spellbook to the site's gear builder and prints the link in chat.

## Not implemented yet

Collection log **totals** (`completed`/`total`), combat achievement points, and quest count/points are not sent —
the site can display them (same JSON shape as [Dink](https://github.com/pajlads/DinkPlugin)), but RuneLite has no
reliable API for those without the same widget-reading and per-quest/per-task lookup tables Dink maintains, and a
wrong number here would silently skew the site's automatic rank recommendations. See `DECISIONS.md` for the
detail on each one — every choice below was checked against the actual `runelite-api` jar, not memory.

## Configuration

The clan code is entered only in the side panel. The plugin settings show just four switches — send screenshots, show in-game notifications, send
deaths, show the bingo board. Everything else (screenshot threshold and so on) comes from the site: the plugin
re-reads it on start, right after linking and every hour, so a change on the site needs no plugin update.

## Privacy

- The **clan code** works like a password, not a shareable ID — whoever has it can send fake events to the
  site under your name. Never share it or show it on stream; if it leaks, replace it on `/profil` (the old one
  stops working instantly). The site rate-limits wrong codes and alerts staff when someone is guessing.
- Only what's listed under "What it does today" is sent, to `clan.kokalj.dev` only — no private messages, no
  bank contents beyond what a loot/death event needs, no IP addresses.
- **Screenshots**, when enabled, show your username, chat and whatever's in your inventory/equipment at that
  moment.

## For clan members

See the "Poveži TRIGLAV plugin" section on `/profil` on the site for setup instructions once the plugin is
available in the Plugin Hub.

## License

BSD 2-Clause, see `LICENSE`.
