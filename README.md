# Reality Unfolded — Spell-Language Proof of Concept

Forge 1.20.1 proof of concept for an order-sensitive magical programming language.
This build implements independent words and a generic resumable runtime. It does
not contain hardcoded spell classes.

## POC vocabulary

`BOLT`, `BREAK`, `IGNITE`, `IMPACT`, `EXPLOSION`, `AMPLIFY`, `SPLIT(n)`, `LINK`, `HOME`

## POC Grimoire

Obtain the temporary Grimoire from the Tools & Utilities creative tab or with:

```text
/give @s reality_unfolded:grimoire
```

- Right-click while holding it to open the eight-slot player spell library.
- Edit the name and ordered word source, then validate it through the server.
- Save, select an existing saved slot, or use `Save + Select` in one step.
- Cast the active slot from the interface, or Shift + right-click while holding
  the Grimoire after closing it.

Spell slots belong to persistent player data, not the physical ItemStack. The
client UI only proposes edits; the server validates and stores them before
returning an authoritative snapshot.

Acceptance-path example:

```text
Name:   Fuck That Guy
Source: BOLT -> HOME -> IMPACT -> IGNITE
```

`Validate`, `Save`, `Select Saved`, and `Cast Selected` are server-backed
actions. Failed validation leaves the current editor draft intact.

## Developer interface

Commands require permission level 2.

```text
/ru cast <ordered words>
/ru inspect <ordered words>
/ru links
/ru examples
```

Both spaces and arrows are accepted as separators:

```text
/ru cast BOLT -> HOME -> IMPACT -> IGNITE
/ru cast BOLT SPLIT(3) LINK IMPACT IGNITE
```

`AMPLIFY` is a postfix modifier. For example, `EXPLOSION AMPLIFY` doubles the
explosion's damage power while its effect radius remains fixed.

## Runtime architecture

- `SpellProgram` is a serializable directed node graph. The temporary command
  parser emits a linear path, but the representation and executor support
  multiple child edges.
- Each word is independently registered through `WordRegistry` and receives a
  generic `SpellExecutionContext`.
- `IMPACT` suspends the current branch and stores downstream node IDs on the
  manifestation. Collision resumes those nodes in an impact context.
- `SPLIT(n)` creates real server-side child manifestation entities. Each child
  independently resumes the downstream graph.
- `HOME` installs continuous server-side steering on the current manifestation.
- `LINK` registers manifestations in a server runtime relationship keyed by cast
  and graph node. `/ru links` exposes live relationship/member counts.
- Manifestations serialize their program, cast lineage, power, homing state, and
  impact continuations to NBT.

## POC test commands

```text
/ru cast BOLT
/ru cast BOLT IMPACT IGNITE
/ru cast BOLT IMPACT EXPLOSION
/ru cast BOLT IMPACT EXPLOSION AMPLIFY
/ru cast BREAK
/ru cast BOLT HOME IMPACT IGNITE
/ru cast BOLT SPLIT(3) IMPACT IGNITE
/ru cast BOLT SPLIT(3) LINK IMPACT IGNITE
```

The log prefix `[RU SPELL]` exposes cast IDs, executed nodes, context, child
creation, impacts, link lifecycle, validation failures, and termination.

## Scope boundary

No progression, Domains, Void Embalming, Soul Curses, mana system, final
Grimoire artwork, Workbench UI, reagents, targeting language, conditions,
functions, or final VFX are part of this proof of concept.
