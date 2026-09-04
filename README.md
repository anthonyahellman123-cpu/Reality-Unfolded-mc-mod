# Reality Unfolded — Stage 2 Spellcraft + Void Caster

Forge 1.20.1 proof of concept for an order-sensitive magical programming language.
This build implements independent words and a generic resumable runtime. It does
not contain hardcoded spell classes.

## POC vocabulary

`BOLT`, `BREAK`, `IGNITE`, `IMPACT`, `EXPLOSION`, `AMPLIFY`, `SPLIT`, `LINK`, `HOME`

## Player workflow

Obtain the Grimoire programmer and Void Caster Gauntlet from the Tools & Utilities
creative tab or with:

```text
/give @s reality_unfolded:grimoire
/give @s reality_unfolded:void_caster_gauntlet
```

- Right-click the Grimoire to open the eight-slot visual Spellcraft programmer.
- Click registered glyphs to append them to the current spell. Select a placed
  glyph to move or remove it; no raw spell source is required.
- Name the spell, validate it, and use `Save + Select` to make it active.
- Close Spellcraft, hold the Void Caster Gauntlet, and right-click to cast the
  active spell through normal gameplay.

Spell slots belong to persistent player data, not the physical ItemStack. The
client UI only proposes edits; the server validates and stores them before
returning an authoritative snapshot.

Acceptance-path visual sequence:

```text
Name: Fuck That Guy
BOLT → HOME → IMPACT → IGNITE
```

Validation, saving, selection, and casting are server-authoritative. There is no
GUI cast button: the gauntlet delegates the selected player-owned source to the
same `SpellCastService` used by `/ru cast`.

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
/ru cast BOLT SPLIT SPLIT LINK IMPACT IGNITE
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
- `SPLIT` replaces every compatible manifestation with two server-side child
  manifestations. Repeating the glyph doubles the set: 1→2→4. Legacy
  developer input `SPLIT(n)` remains accepted, and validation caps a program at
  an estimated 64 manifestations.
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
/ru cast BOLT SPLIT IMPACT IGNITE
/ru cast BOLT SPLIT SPLIT HOME IMPACT IGNITE
/ru cast BOLT SPLIT LINK IMPACT IGNITE
```

The log prefix `[RU SPELL]` exposes cast IDs, executed nodes, context, child
creation, impacts, link lifecycle, validation failures, and termination.

## Scope boundary

No progression, Domains, Void Embalming, Soul Curses, mana system, final
Grimoire artwork, Workbench UI, reagents, targeting language, conditions,
functions, or final VFX are part of this proof of concept.
