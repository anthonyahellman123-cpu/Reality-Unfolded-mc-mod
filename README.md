# Reality Unfolded

A programmable Void-magic mod for Minecraft Forge 1.20.1.

> Reality is only absolute until you learn how to unfold it.

## Current foundation

- Persistent player mana stored server-side
- Natural regeneration in every dimension
- Regeneration multipliers: Overworld 1×, Nether 1.25×, End 2×
- Minimal synchronized mana HUD
- Developer commands for inspection and tuning
- GitHub Actions build producing a test JAR

## Developer commands

All commands require permission level 2. Omit `[player]` to target yourself.

```text
/realitymana get [player]
/realitymana set <amount> [player]
/realitymana add <amount> [player]
/realitymana drain <amount> [player]
/realitymana refill [player]
/realitymana capacity <amount> [player]
/realitymana regen <amount-per-second> [player]
```

## Design boundary

This first branch intentionally contains no spell runtime, attunement progression, constructs, Loom, or Domains. Mana must be stable and testable before higher systems depend on it.
