# Kumo · 雲

Kumo (くも / 雲) means cloud. The identity uses a soft, asymmetric white cloud over the existing blue #235BB5. No letters are packed into the launcher icon.

- kumo-icon.svg: scalable rounded-square brand asset.
- kumo-cloud.svg: white mark on a transparent canvas.
- Android: vector fallback for API 24–25, adaptive foreground/background for API 26+, monochrome themed icon for API 33+, and white notification silhouette.
- Cloud artwork stays inside the central adaptive safe area. Launcher masks and parallax are system-controlled. SVG is the design source; Android uses matching vector XML, not an SVG decoder.
- AppIcons.kt contains 19 compact original interface glyphs. Material 3 components remain; the large material-icons-extended dependency is removed.
