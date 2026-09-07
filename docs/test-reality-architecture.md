# Test Reality architecture raid — Forge 1.20.1

Status: implementation gate for the first Test Reality POC.

The governing rule is **save the instructions for reality; do not save reality**. A player's persistent RU data is authoritative. Any chunks written by Minecraft for the Test Reality are disposable cache and must be deterministically rebuilt before an allocation is reused.

## Sources inspected

- Mojang/vanilla 1.20.1 dimension JSON, `ServerLevel`, chunk ticket, teleport, and saved-player-data behavior through the Forge 47.3.0 mapped environment.
- [Compact Machines](https://github.com/compactmods/compactmachines), tag `v6.0.3`, commit `7e90b64f0a8ae87a933f459b15c1608632a8e6a2`.
- [Dimensional Doors](https://github.com/DimensionalDevelopment/DimDoors), Forge-compatible 1.20.1 source line, inspected commit `871026b8a0b50c2d7295e80276277777e6ddf421`.

Neither mod becomes an RU dependency. Their code was studied for boundaries and failure modes, not copied wholesale.

## What survives the raid

| Problem | Useful precedent | RU decision |
|---|---|---|
| Isolated allocations | Compact Machines places owned rooms at separated chunk coordinates in one registered dimension. DimDoors maps pocket IDs to separated grid cells. | Use one statically registered `reality_unfolded:test_reality` dimension and allocate widely separated, chunk-aligned cells. Do not dynamically mutate the live server's dimension registry. |
| Multiple owners | Both projects use UUID-to-room/pocket identity. | Runtime instance identity is `(session UUID, owner UUID, cell)`. Three players can occupy independent cells without creating three `ServerLevel` objects. |
| Load only what is used | DimDoors `LazyGenerationPocket` generates a pocket chunk only when served and marks it generated. | Generate/reset an active allocation one chunk at a time. Entry prepares only the arrival chunk; player chunk tickets drive the rest. Never preload the theoretical bounds. |
| Return safety | Compact Machines stores precise entry dimension, position, and rotation, then falls back to respawn/overworld if history is invalid. DimDoors adds a post-teleport chunk ticket. | Persist one crash-safe return ticket on the player before transfer. On normal exit consume it. On login in an orphaned Test Reality, eject through the ticket or fall back to the overworld spawn. |
| Bounds | DimDoors pockets have explicit boxes and resolve position to pocket ID. | Every active allocation has chunk and block bounds. Generation, edit operations, cleanup, and commands must resolve through those bounds. |
| Reproducibility | Vanilla generation is deterministic from seed and parameters; DimDoors separates a generator definition from lazy chunk realization. | Store a seed plus versioned generation parameters. Rebuild each chunk from that definition, then replay deliberate edit operations in order. |
| Cleanup | Both projects track room/pocket ownership and teleport through server APIs; temporary chunk tickets are preferable to permanent forced chunks. | Evacuate players, discard non-player entities inside the allocation, cancel RU continuations for its owner/level, release runtime references, then make the cell reusable. Do not use persistent forced chunks. |

## Deliberately rejected inheritance

- **Dynamic `ServerLevel` creation/removal.** Forge 1.20.1 does not provide a small, safe public lifecycle API for arbitrary live dimension objects. Mutating the server level map risks dangling references in players, chunks, entities, tickets, and saved data.
- **Permanent-room authority.** Compact Machines and DimDoors preserve rooms/pockets because persistence is their feature. RU must treat serialized chunks as replaceable cache.
- **Live region-file deletion.** Deleting dimension files while a server owns chunk/storage handles is unsafe. The POC performs logical destruction by deterministic reset and runtime cleanup.
- **Permanent force-loading.** It survives restarts and turns a temporary experiment into server obligation. Normal player tickets and the short post-teleport load are enough.
- **Whole-instance snapshots.** Normal mining, explosions, builds, mobs, and drops are deliberately disposable. A million-block build must not become a million-entry player NBT list.
- **A version-abstraction framework.** This is intentionally Forge 1.20.1 architecture.

## POC lifecycle

1. `enter`
   - reject a second active allocation for the same owner;
   - persist the exact return ticket first;
   - allocate a free separated cell and create an ephemeral session record;
   - deterministically reset only the arrival chunk;
   - replay saved definition operations intersecting that chunk;
   - teleport with the server API.
2. `play`
   - when another chunk in the active bounds loads for the first time this session, reset it from seed and replay saved operations;
   - normal gameplay changes are not recorded;
   - keep a bounded set of touched/generated chunk keys per session.
3. `edit`
   - explicit owner-only edit mode enables a small palette;
   - edits create bounded procedural operations such as `FILL min max block` in a staged list;
   - `save` atomically appends the staged operations to the persistent definition;
   - unsaved operations vanish with the instance.
4. `exit` / destruction
   - move every player out safely (owner ticket where present, overworld fallback otherwise);
   - discard non-player entities in bounds;
   - cancel owner Test Reality continuations and let manifestation removal clean link membership;
   - remove session maps and all strong references to the level/chunks/entities;
   - clear the consumed return ticket.
5. next `enter`
   - allocate a cell and rebuild chunks before exposure. Old on-disk state is overwritten and never treated as authority.

## Persistent versus ephemeral state

| Player-persistent definition | Active server runtime only |
|---|---|
| schema/generator version | session UUID and allocated cell |
| seed | instance bounds |
| provisional generation parameters | chunks rebuilt this session |
| committed procedural edit operations | staged edit operations |
| scanned-subject descriptors (future-facing empty collection in POC) | spawned mobs and dropped items |
| diagnostic settings | players currently inside and return transfer activity |
| crash-safe return ticket | chunk/level/entity references |

The scanned-subject store contains identifiers/configuration needed to create new subjects. It never serializes a live entity as the canonical specimen.

## Safety and provisional limits

- Fixed hard bounds exist for encoded operations, operation count, edit volume, active instances, and per-session touched chunks. These are POC engineering guardrails, not tier lore.
- Instance spacing is much larger than the initial playable bounds so adjacent chunk render/simulation distances cannot overlap.
- The static dimension type supplies an engine-level vertical envelope; the generated terrain occupies a relatively shallow part of it. Tier-specific horizontal authorship remains data, not a new dimension type.
- Cell allocation and active maps are server-thread-only. No static mutable "current instance" or "current owner" exists.
- Login, logout, death/dimension anomalies, and server stopping all have explicit cleanup/ejection paths.
- A production follow-up should profile reset cost, chunks loaded per player, entity counts, server tick time, and serialized definition size before changing limits.

## Integration seam

The POC lives behind `/ru reality ...` developer commands. It does not add progression, a Void Bench, SCAN, final UI, or permanent acquisition rules. The command layer calls a Test Reality service; it does not own allocation/generation logic. That keeps a later player-facing entrance free to call the same service.
