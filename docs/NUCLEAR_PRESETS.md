# Nuclear Mode — named presets

Lets you save several Nuclear configurations (app list + duration) under a name, and re-apply them
quickly.

## Why

Before: a single implicit "preset" was stored (`lastNuclearApps`, `lastNuclearDurationMin`),
overwritten on every new activation. You lost your config as soon as you tried a variant.

After: N named presets, plus the "Configure new…" slot that starts from scratch.

## Components

- **`NuclearPresetsManager.kt`** — JSON persistence in `filesDir/nuclear_presets.json`. API:
  - `list()` / `get(name)` / `save(name, apps, durationMin)` / `delete(name)` / `rename(old, new)`
  - A preset = `{ name: String, apps: List<String>, durationMinutes: Int }`
- **`MainActivity.kt`**:
  - 💾 *Save as preset* button in the Nuclear confirmation dialog → name prompt → save, the dialog
    stays open
  - Picker when tapping "Activate Nuclear Mode": list of presets + "Configure new…" at the top
  - *Manage Presets* button under the main button → rename / delete

## Persisted state

- `nuclear_presets.json` in `filesDir` (survives `installDebug` updates, lost only on clear-data or
  uninstall)
- The old `lastNuclearApps` / `lastNuclearDurationMin` are still there to pre-fill the
  "Configure new…" mode — not migrated into a default preset (intentional, to avoid a stray
  "Unnamed" preset).
