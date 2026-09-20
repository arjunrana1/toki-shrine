# Time Shrine icon pack

Selected mark: torii gate combined with an hourglass.

## Files

- `master/time-shrine-icon-source-1254.png` — untouched generated source.
- `master/time-shrine-icon-1024.png` — working master for design files.
- `play-store/time-shrine-play-icon-512.png` — Google Play listing asset.
- `android/mipmap-*/ic_launcher.png` — Android legacy launcher sizes.
- `android/mipmap-*/ic_launcher_round.png` — matching round-icon resources.

## Android integration

The five legacy and round `mipmap-*dpi` raster sets are integrated in
`app/src/main/res/` and match this pack. The manifest keeps the
`@mipmap/ic_launcher` and `@mipmap/ic_launcher_round` references.

Android 8+ uses the adaptive-icon XML files in `mipmap-anydpi-v26/`. Those
resources now point to a raster foreground derived from the selected artwork
over the brand background, so the chosen mark is also the visible modern
launcher icon.

For production, redraw the selected mark as separate vector foreground and
background layers. This generated artwork includes soft shading and a composed
rounded-square background, so it cannot provide a clean Android monochrome or
themed icon without a vector redraw.

## Brand colors

- Background: `#161826`
- Primary blurple: `#9184D9`
- Pale highlight: `#D2CEFD`
