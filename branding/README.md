# Kumo · 雲

Kumo (くも / 雲) means cloud, and the identity is that kanji itself: 雲 in white over the existing blue #235BB5. There is no cloud pictogram and no Latin lettering in the launcher icon.

- kumo-icon.svg: scalable rounded-square brand asset.
- kumo-kanji.svg: the white mark on a transparent canvas, matching the Android adaptive foreground.
- Android: vector fallback for API 24–25, adaptive foreground/background for API 26+, monochrome themed icon for API 33+, and white notification silhouette.
- The kanji stays inside the central adaptive safe area, so launcher masks never clip a stroke. The square asset uses a slightly larger mark because it is never masked. Masks and parallax are system-controlled. SVG is the design source; Android uses matching vector XML, not an SVG decoder.
- Outlines were converted from Noto Sans JP Bold (SIL Open Font License 1.1) to path data. No font file is bundled for the icon and none is needed at runtime.
- AppIcons.kt contains the compact original interface glyph set, including the light, dark and system appearance icons. Material 3 components remain; the large material-icons-extended dependency is removed.
