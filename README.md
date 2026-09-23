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

## Not implemented yet

Collection log, combat achievements, achievement diary, quest count, slayer streak, pets and death tracking are
not sent yet — the site can display them (same JSON shape as [Dink](https://github.com/pajlads/DinkPlugin)), the
plugin just doesn't collect them yet. See `DECISIONS.md` for why (mostly: no reliable RuneLite API for those
without the same widget/chat scraping Dink does, and getting the numbers wrong would corrupt the site's stats).

## Configuration

Members only ever see: the clan code (filled in automatically by pairing), and three switches — send
screenshots, show in-game notifications, send deaths.

## For clan members

See the "Poveži TRIGLAV plugin" section on `/profil` on the site for setup instructions once the plugin is
available in the Plugin Hub.

## License

BSD 2-Clause, see `LICENSE`.
