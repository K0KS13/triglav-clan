# TRIGLAV Clan

RuneLite plugin for the TRIGLAV OSRS clan ([clan.kokalj.dev](https://clan.kokalj.dev)). One pairing code links your
account to the clan site — no webhook URLs to copy, unlike Dink.

## What it does today

- **Pairing** — click *Poveži račun* in the panel, enter the 6-character code on `/profil` on the site, done.
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

## Not implemented yet

Collection log **totals** (`completed`/`total`), combat achievement points, and quest count/points are not sent —
the site can display them (same JSON shape as [Dink](https://github.com/pajlads/DinkPlugin)), but RuneLite has no
reliable API for those without the same widget-reading and per-quest/per-task lookup tables Dink maintains, and a
wrong number here would silently skew the site's automatic rank recommendations. See `DECISIONS.md` for the
detail on each one — every choice below was checked against the actual `runelite-api` jar, not memory.

## Configuration

Members only ever see: the clan code (filled in automatically by pairing), and three switches — send
screenshots, show in-game notifications, send deaths.

## For clan members

See the "Poveži TRIGLAV plugin" section on `/profil` on the site for setup instructions once the plugin is
available in the Plugin Hub.

## License

BSD 2-Clause, see `LICENSE`.
