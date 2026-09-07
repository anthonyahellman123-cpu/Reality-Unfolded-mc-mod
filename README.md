# Reality Unfolded — Stage 2 Smart Spell Foundation

Forge 1.20.1 proof of concept for an order-sensitive magical programming language.
This build implements independent words and a generic resumable runtime. It does
not contain hardcoded spell classes.

## Player-facing vocabulary

- Manifestations: `BOLT`, `ORB`
- Context: `SELF`, `ENTITY`, `PLAYER`, `NEAREST`, `HOSTILE`, `TOUCH`
- Logic: `SENSE`, `IF`, `NOT`
- Flow: `IMPACT`, `SPLIT`, `LINK`, `DELAY`, `RELEASE`
- Motion: `HOME`, `ACCELERATE`, `GRAVITY`, `ANTI_GRAVITY`
- Effects/world: `BREAK`, `IGNITE`, `EXPLOSION`, `AMPLIFY`

All 24 words are registered through the same runtime and presentation registry.
The glyph browser pages through that registry, so none of these words require raw
source typing. `DELAY` exposes a bounded tick parameter in the current visual
editor.

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

## Smart-spell runtime

- `SpellProgram` is a serializable directed node graph. `IF` now compiles a
  genuine true edge and false terminal edge. The current visual projection marks
  this as `T↓ / F END`; an explicit ELSE editor is intentionally deferred.
- Each word is independently registered through `WordRegistry` and receives a
  branch-local `SpellExecutionContext`. It carries the caster, target/query,
  condition, current manifestation, continuation boundary, and cast lineage;
  active state is never stored in global mutable "current spell" fields.
- `ENTITY` and `PLAYER` establish candidate queries. `HOSTILE` refines the query
  before `NEAREST` resolves it. `PLAYER` excludes the caster; `SELF` names the
  caster explicitly.
- `SENSE` asks whether the following context query is currently true. `NOT`
  negates that result and `IF` selects a graph edge server-side.
- `TOUCH` uses the player's Forge block/entity reach attributes. It cannot reuse
  the cast service's longer debug aim trace, so unreachable targets resolve false.
- `IMPACT` suspends the current branch and stores downstream node IDs on the
  manifestation. Collision resumes those nodes in an impact context.
- `DELAY(n)` stores a bounded server-owned continuation and resumes it once after
  `n` ticks. `RELEASE` currently consumes that resumed delay boundary; it does not
  imply the future CHARGE system.
- `SPLIT` replaces every compatible manifestation with two server-side child
  manifestations. Repeating the glyph doubles the set: 1→2→4. Legacy
  developer input `SPLIT(n)` remains accepted, and validation caps a program at
  an estimated 64 manifestations.
- `HOME` consumes an explicitly resolved entity target, or resolves the nearest
  candidate from an existing filtered query, and installs continuous server-side
  steering. Only truly bare legacy `HOME` retains the Stage-2 nearest-living-target
  fallback for compatibility; guidance never guarantees impact.
- `ACCELERATE` composes multiplicatively up to a runtime speed cap. `GRAVITY` and
  `ANTI_GRAVITY` apply continuous downward/upward influence.
- `ORB` is a distinct persistent, non-impacting manifestation entity. It supports
  generic `SPLIT`, `LINK`, motion, and gravity operations without conversion to a
  bolt.
- `LINK` registers manifestations in a server runtime relationship keyed by cast
  and graph node. `/ru links` exposes live relationship/member counts.
- Manifestations serialize their program, cast lineage, power, homing state, and
  impact continuations to NBT.

## Smart-spell examples

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
/ru cast ENTITY HOSTILE NEAREST BOLT HOME IMPACT IGNITE
/ru cast SENSE ENTITY HOSTILE IF BOLT HOME IMPACT IGNITE
/ru cast BOLT DELAY(20) RELEASE ACCELERATE IMPACT IGNITE
/ru cast BOLT ACCELERATE GRAVITY IMPACT IGNITE
/ru cast ORB SPLIT LINK
/ru cast ORB ANTI_GRAVITY LINK
```

The log prefix `[RU SPELL]` exposes cast IDs, executed nodes, context, child
creation, impacts, link lifecycle, validation failures, and termination.

## POC runtime guardrails

- Query radius: 32 blocks; at most 64 candidate entities are inspected.
- Estimated manifestations: 64 per validated program; legacy `SPLIT(n)` accepts
  2–16 children while the glyph remains parameterless ×2.
- Delay: 1–200 ticks, at most 32 queued continuations per cast and 256 per level.
- Executor: 256 nodes per resume; visual draft: 32 glyphs.
- Bolt lifetime: 120 ticks; persistent ORB lifetime: 600 ticks; manifestation
  speed cap: 4 blocks/tick.

These are multiplayer POC engineering limits, not lore or permanent balance.

## Scope boundary

No progression, Domains, Void Embalming, Soul Curses, mana/AP system, SCAN,
CHARGE, final Spellcraft redesign, functions/macros, or final VFX are part of
this proof of concept.
