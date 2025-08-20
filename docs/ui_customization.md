# UI Texture Customization

Eidolon Unchained's GUI artwork can be replaced through a resource pack. Any PNG in
`assets/eidolon/textures/gui/` can be overridden by providing a file with the same
name and path in your pack.

## Example resource pack structure

```
MyPack/
  pack.mcmeta
  assets/
    eidolon/
      textures/
        gui/
          codex_bg.png          # Codex background (512x512)
          codex_title_page.png  # Title page overlay (256x256)
```

## Resolution and format

* Textures must be standard PNG files.
* Keep the same pixel dimensions as the originals (e.g. `codex_bg.png` is
  512×512, `codex_title_page.png` is 256×256). Larger or differently proportioned
  images may stretch or be cropped.
* Using higher‑resolution multiples (e.g. 1024×1024 for the background) is
  supported, but preserve the aspect ratio.

## Limitations and testing

Layout sizes for codex pages and other widgets are hard‑coded, so texture
changes cannot move UI elements or resize slots. Always test custom graphics in
game—reload resource packs (`F3+T`) and verify that alignment looks correct at
your target resolution.
