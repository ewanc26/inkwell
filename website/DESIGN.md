---
name: Inkwell
description: A high-contrast editorial brand system shared with ewancroft.uk — green-tinted near-black on near-white, Variable Inter, mono labels, tonal surfaces, and zero decorative decoration.
colors:
  accent:
    value: "rgb(100 187 68)"
    dark: "rgb(100 187 68)"
    canonical: "#64BB44"
    role: primary-accent
  text-900:
    value: "rgb(22 34 17)"
    dark: "rgb(226 238 221)"
    role: primary-text
  text-950:
    value: "rgb(11 17 9)"
    dark: "rgb(241 246 238)"
    role: heading-text
  background-50:
    value: "rgb(239 249 236)"
    dark: "rgb(10 19 6)"
    role: primary-bg
  surface:
    value: "rgb(193 230 179)"
    dark: "rgb(39 77 25)"
    role: surface-bg
  muted:
    value: "rgb(89 136 68)"
    dark: "rgb(140 187 119)"
    role: muted-text
typography:
  display:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 800
    fontSize: "clamp(2.5rem, 6vw, 4.5rem)"
    lineHeight: 0.95
    letterSpacing: "-0.05em"
  headline:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 800
    fontSize: "clamp(1.25rem, 1.5vw + 1rem, 2rem)"
    lineHeight: 1.1
  body:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 400
    fontSize: "clamp(1rem, 1vw + 0.8rem, 1.25rem)"
    lineHeight: 1.6
  label:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 600
    fontSize: "clamp(0.8rem, 0.75vw + 0.6rem, 1rem)"
    lineHeight: 1.4
  mono:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontWeight: 400
    fontSize: "clamp(0.64rem, 0.6vw + 0.5rem, 0.8rem)"
    lineHeight: 1.5
rounded:
  xs: "4px"
  sm: "6px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  full: "9999px"
spacing:
  "2xs": "2px"
  xs: "4px"
  sm: "8px"
  "3": "12px"
  md: "16px"
  "6": "24px"
  lg: "32px"
  "12": "48px"
  xl: "64px"
  "2xl": "96px"
motion:
  duration:
    instant: "100ms"
    fast: "200ms"
    normal: "300ms"
    slow: "500ms"
  easing:
    out-quart: "cubic-bezier(0.25, 1, 0.5, 1)"
    out-expo: "cubic-bezier(0.16, 1, 0.3, 1)"
components:
  button-primary:
    backgroundColor: "{colors.text-950}"
    textColor: "{colors.background-50}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"
    typography: "{typography.label}"
  button-primary-hover:
    backgroundColor: "primary-800"
  button-outline:
    backgroundColor: "surface-raised"
    textColor: "{colors.text-950}"
    border: "1px solid {colors.surface}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"
    typography: "{typography.label}"
  feature-card:
    backgroundColor: "surface-raised"
    rounded: "{rounded.lg}"
    border: "1px solid {colors.surface}"
    padding: "2rem"
    boxShadow: "none"
  feature-icon:
    backgroundColor: "primary-50"
    textColor: "primary-600"
    rounded: "{rounded.sm}"
    size: "2.5rem"
---

# Design System: Inkwell

## 1. Overview

**Creative North Star: "The Traditional Meets Technical"**

Inkwell inherits its visual system from its sibling, [ewancroft.uk](https://ewancroft.uk). The register is the same high-contrast editorial language: green-tinted near-black text on a near-white background, Variable Inter set with tight display tracking, JetBrains Mono for labels, and surfaces distinguished by tonal value and a hairline border rather than by decoration.

This is a deliberate departure from the previous cool-navy/glassmorphism system. Where the old system reached for translucent headers and app-matched drop shadows, this one trusts contrast: the page is nearly white, the type is nearly black, and the one accent — a vivid green matching the app icon's ink-drop mark — is used sparingly for focus, hover, links, and the wordmark's dot.

**Key Characteristics:**

- High contrast first: body text `text-900` on `background-50`, headings `text-950`, every step checked against 4.5:1 (3:1 for large text)
- Typography-first hierarchy: weight, size, and tight tracking do the work; color is secondary
- Mono as a technical register: navigation, footer, page metadata, captions, and section labels speak in JetBrains Mono
- Tonal + border surfaces: cards and lists step up from the page with a raised surface and a 1px border — no drop shadows anywhere
- Single-accent discipline: one green (`accent-500`, canonical `#64BB44`) for links, focus rings, markers, and the ink-drop mark
- Light/dark parity: every color is a `light-dark()` pair defined once per token
- Calm motion: exponential-eased entrances, decisive press feedback, everything collapsing under reduced motion
- Ecosystem-native: the site sits alongside the personal site and the AT Protocol ecosystem's indie-web confidence — technical without being cold

## 2. Colors

Five full ramps (50–950), each a `light-dark(light, dark)` pair in plain RGB, shared verbatim with ewancroft.uk. The palette is green-tinted throughout: the "neutral" text ramp and the "white" background ramp both carry a faint green cast, so foreground, background, and accent read as one world.

| Ramp           | Light (default)                                               | Dark              | Role                                                   |
| -------------- | ------------------------------------------------------------- | ----------------- | ------------------------------------------------------ |
| `text-*`       | dark greens → near-black (`#162211` at 900, `#0B1109` at 950) | near-white greens | Text hierarchy. `text-900` body, `text-950` headings   |
| `background-*` | near-white greens (`#EFF9EC` at 50)                           | near-black greens | Surfaces. `background-50` page, `200` hairline borders |
| `primary-*`    | green (`#68B34D` at 500)                                      | same              | Brand green: links, hover, focus, active nav           |
| `secondary-*`  | green                                                         | same              | Reserved secondary ramp                                |
| `accent-*`     | green (`#64BB44` at 500)                                      | same              | Accent ramp: wordmark dot, selection, markers          |

### Semantic aliases

Day-to-day component CSS reaches for these flat aliases rather than the raw ramp steps:

| Alias                  | Resolves to              |
| ---------------------- | ------------------------ |
| `--color-accent`       | `accent-500` (`#64BB44`) |
| `--color-accent-light` | `accent-400`             |
| `--color-muted`        | `text-600`               |
| `--color-border`       | `background-200`         |
| `--color-surface`      | `background-200`         |
| `--surface-color`      | `background-200`         |
| `--surface-raised`     | `background-100`         |
| `--surface-sunken`     | `background-50`          |

### Named Rules

**The High-Contrast Rule.** Body text is `text-900` on `background-50`; headings are `text-950`. Muted text is `text-600` — still green-tinted, but checked to clear 4.5:1 against `background-50`. Never place body text on `text-700` or lighter.

**The One Accent Rule.** Accent green appears on links, focus rings, list markers, the active nav underline, and the wordmark dot — as the interactive signal, not as a decorative surface. A primary button is dark (`text-950`), not green.

**The Tonal + Border Rule.** Cards, lists, and callouts differentiate from the page via `surface-raised` background and a 1px `surface-color` border. There are no drop shadows, no gradients, no glass.

## 3. Typography

**Display & Body:** Inter (self-hosted variable woff2, weights 400–800) with `ui-sans-serif, system-ui, sans-serif` fallback.
**Mono:** JetBrains Mono (self-hosted) with `ui-monospace, monospace` fallback. The mono register is visible by design: nav links, footer, page metadata, platform labels, and captions.

### Hierarchy

- **Display** (800, fluid `clamp(2.5rem, 6vw, 4.5rem)`, line-height 0.95, -0.05em): hero `h1` only
- **Headline** (800, fluid `clamp(1.25rem, 1.5vw + 1rem, 2rem)`, 1.1): card/callout headings
- **Section title** (700, fluid `clamp(1.6rem, 3.5vw, 2.4rem)`, -0.035em): section `h2` on the landing page
- **Section label** (700, `text-sm`, uppercase, +0.05em, mono not required): mono-uppercase platform labels (iOS / Android)
- **Body** (400, `clamp(1rem, 1vw + 0.8rem, 1.25rem)`, 1.6): prose, capped at 60–70ch
- **Label** (600, `text-sm`): buttons
- **Mono** (400, `text-xs`): nav, footer, metadata, captions, code

### Named Rules

**The Typography-First Rule.** Hierarchy is established through weight, size, and tracking — never through color splashes. If a heading needs an accent color to feel important, the weight or size is wrong.

**The One Display Rule.** The hero `h1` is the only element at Display weight on any page.

## 4. Elevation

There is no elevation. The page is flat and high-contrast: surfaces are distinguished by `surface-raised` background plus a 1px `surface-color` border, and interaction states shift tonal value (hover → `surface-sunken`) and border color (→ `background-300`) rather than lifting or casting shadows.

### Named Rules

**The No-Glass, No-Shadow Rule.** No `backdrop-filter`, no `box-shadow`, no gradients. The sticky nav is a solid `background-50` bar with a bottom border — legibility comes from contrast, not blur.

## 5. Motion

Motion mirrors ewancroft.uk: entrance uses `--ease-out-expo` at `--duration-slow` (500ms) as a calm fade with a 12px upward drift; `.stagger-1`…`.stagger-5` delay in 100ms steps. Direct manipulation (button press, nav link underline) uses `--ease-out-quart` at `--duration-fast` (200ms). `prefers-reduced-motion: reduce` collapses everything to near-instant globally.

### Named Rules

**The Feedback-Not-Decoration Rule.** Animation plays on content entrance and on direct manipulation only — never as ambient looping decoration.

## 6. Components

### Navigation

Solid sticky bar (`background-50`, 1px `surface-color` bottom border), brand mark (inline capsule SVG + wordmark) left, mono links right. Links are `text-600`, hover and active route → `primary-700` with a 2px `primary-500` inset underline. At `≤800px` the toggle (44×44) reveals an in-flow dropdown panel (`surface-raised`, rounded, bordered) with full-width links. Escape and route changes close it; focus returns to the toggle.

### Buttons

Pills with `--radius-md`. Primary = `text-950` background, `background-50` text (hover → `primary-800`); this is the high-contrast editorial choice — the darkest surface on the page carries the single most important action. Outline = `surface-raised` background, `surface-color` border, `text-950` text (hover → `primary-700` + `primary-500` border). Both press at `scale(0.98)`. Primary CTAs: hero "Get Inkwell" → `#download`; "Add AltStore source" / "Add F-Droid repo" on the download cards.

### Feature cards & callouts

`surface-raised` on a 1px `surface-color` border, `--radius-lg`, 2rem padding, no shadow. Icon (2.5rem `primary-50` tile, `primary-600` glyph) → heading → body. Hover darkens the surface and border. Callouts are a narrower (42rem), centered variant with an inline accent link.

### Editorial rows

Raised list surfaces (`surface-raised` + border, `--radius-md`) containing baseline-aligned rows: title left, mono `row-meta` right. Hover → `surface-sunken`. Used for any "list of items" content.

### Screenshot frames

1px `surface-color` border, `--radius-md`, on `surface-raised`; mono caption beneath. No shadow — the mockups read as flat illustrations.

### Prose (privacy / terms)

Max-width 70ch, `text-900`, line-height 1.75. Links are `primary-600` underlined in a 35% primary tint; headings `text-950`. Code is mono on a `surface-raised` chip. List markers are `primary-500`.

## 7. Do's and Don'ts

### Do:

- **Do** keep body text `text-900` on `background-50` and headings `text-950` — high contrast is the system's signature
- **Do** use the mono register (JetBrains Mono) for nav, footer, metadata, and captions
- **Do** differentiate surfaces with tonal value + a 1px border, never a shadow
- **Do** use `text-wrap: balance` on headings and `text-wrap: pretty` on prose
- **Do** cap body text at 60–70ch
- **Do** test every color against its background for ≥4.5:1 (body) and ≥3:1 (large text ≥18px or bold ≥14px)
- **Do** provide a `prefers-reduced-motion` fallback for every animation (handled globally in `system.css`)
- **Do** support both light and dark color schemes via `light-dark()` on every token
- **Do** reserve the accent green for interactive signals (links, focus, markers, the wordmark dot)

### Don't:

- **Don't** use drop shadows, gradients, or `backdrop-filter` anywhere — the sticky nav is a solid bar
- **Don't** make a primary button accent-green — primary buttons are `text-950`/`background-50` inverse
- **Don't** use the SaaS startup template kit: no gradient heroes, no animated counters, no glassmorphism
- **Don't** set body text lighter than `text-700` against the page background
- **Don't** nest cards inside cards — if a container needs a sub-container, reconsider the layout
- **Don't** animate CSS layout properties — use `transform` and `opacity` only
- **Don't** over-explain the AT Protocol to the audience — link to atproto.com for background; the site sells the app, not the protocol
