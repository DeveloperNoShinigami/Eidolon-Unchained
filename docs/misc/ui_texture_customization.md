# UI Texture Customization

Eidolon Unchained's user interface textures live under `assets/eidolon/textures/gui/`. Any file in that directory can be replaced by a resource pack by providing a texture with the same path and name.

## Default texture locations

Some common UI textures include:

- `assets/eidolon/textures/gui/mana_bar.png` – mana bar shown on the HUD.
- `assets/eidolon/textures/gui/codex_bg.png` – background for the Codex interface.

## Overriding textures in a resource pack

Create a resource pack with the same folder structure. Place your replacement PNGs in the `assets/eidolon/textures/gui/` directory of the pack.

```text
MyPack/
  pack.mcmeta
  assets/
    eidolon/
      textures/
        gui/
          mana_bar.png       # replaces the default mana bar
          codex_bg.png       # replaces the Codex background
```

## Before/after examples

| Default file path                                  | Override inside your pack                                           |
|----------------------------------------------------|---------------------------------------------------------------------|
| `assets/eidolon/textures/gui/mana_bar.png`         | `MyPack/assets/eidolon/textures/gui/mana_bar.png`                   |
| `assets/eidolon/textures/gui/codex_bg.png`         | `MyPack/assets/eidolon/textures/gui/codex_bg.png`                   |

Reload resource packs in game (`F3+T`) to see changes. Keep the same image dimensions as the originals to avoid stretching or misalignment.
