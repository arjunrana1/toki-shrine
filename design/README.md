# Time Shrine design

Every design source for Time Shrine lives in this folder. The [PRD](../PRD.md) (including its §17 addenda) and the [theme/UI contract](../docs/components/theme-ui.md) override any mock value, copy or layout here.

| Path | What's there |
|---|---|
| [`mockups/`](mockups/README.md) | The Claude Design export (formerly `toki-shrine-ui-mockups/`, moved here on 23 October 2026): the **Nocturne design system** (`mockups/project/_ds/nocturne-*/styles.css` holds the canonical tokens; `readme.md` beside it holds the rules), the source mock markup `mockups/project/TimeShrine Mocks.dc.html`, and the Stats hi-fi handoff in `mockups/stats-screen/`. |
| `screens/` | Numbered screen renders (01–25) plus owner-supplied references. |
| [`screens/stats-redesign/`](screens/stats-redesign/README.md) | Stats redesign references S1–S8. |
| [`screens/unlock-minutes/`](screens/unlock-minutes/README.md) | Gate 14 v2 and step 3 / 09 v3 handoff. |
| [`icon-final/`](icon-final/README.md) | App icon: master, Play Store 512 px and Android launcher sizes. |
| `humor-assets/` | Block-screen images 1–11 and the Anton display font. |
| `regenerate-screens.py` | Rebuilds renders from the mock markup into `mockups/project/_render/` (ignored by git). |

Shipped images are in the app itself: `app/src/main/res/drawable-nodpi/` holds the block-screen backgrounds 1–15 (12–15 were added on 9 October and exist only there) and the celebration images. The gate and celebration copy lives in `app/src/main/java/com/arjunrana/timeshrine/config/AppConfig.kt`.

Nocturne is dark theme only.
