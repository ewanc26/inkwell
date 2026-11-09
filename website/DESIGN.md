---
name: Inkwell
description: A dark-only teal editorial system aligned with Fathom — near-black teal surfaces, a mint accent, large type, pill actions, rounded gradient-bordered cards, and a soft radial glow.
colors:
  accent:
    value: "rgb(45 212 191)"
    canonical: "#2DD4BF"
    role: primary-accent
  text-900:
    value: "rgb(207 226 222)"
    role: primary-text
  text-950:
    value: "rgb(231 244 241)"
    role: heading-text
  background-50:
    value: "rgb(11 20 22)"
    role: primary-bg
  surface:
    value: "rgb(18 31 36)"
    role: surface-bg
  muted:
    value: "rgb(140 166 163)"
    role: muted-text
typography:
  display:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 700
    fontSize: "clamp(2.8rem, 7.5vw, 5.2rem)"
    lineHeight: 1.04
    letterSpacing: "-0.045em"
  headline:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 700
    fontSize: "1.12rem"
    lineHeight: 1.3
  section:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 700
    fontSize: "clamp(1.7rem, 3.5vw, 2.7rem)"
    lineHeight: 1.12
    letterSpacing: "-0.035em"
  body:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 400
    fontSize: "clamp(1.05rem, 2vw, 1.3rem)"
    lineHeight: 1.65
  label:
    fontFamily: "InterVariable, Inter, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 700
    fontSize: "0.9rem"
    lineHeight: 1
  mono:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontWeight: 400
    fontSize: "clamp(0.7rem, 0.6vw + 0.58rem, 0.82rem)"
    lineHeight: 1.5
rounded:
  xs: "4px"
  sm: "8px"
  md: "14px"
  lg: "20px"
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
    backgroundColor: "primary-500"
    textColor: "#042F2E"
    rounded: "{rounded.full}"
    padding: "0.7rem 1.15rem"
    typography: "{typography.label}"
  button-primary-hover:
    backgroundColor: "primary-700"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.text-950}"
    border: "1px solid {colors.surface}"
    rounded: "{rounded.full}"
    padding: "0.7rem 1.15rem"
    typography: "{typography.label}"
  feature-card:
    backgroundColor: "linear-gradient(180deg, surface-raised, background-50)"
    rounded: "{rounded.lg}"
    border: "1px solid {colors.surface}"
    padding: "1.5rem"
    boxShadow: "none"
  feature-icon:
    backgroundColor: "12% primary-500 tint"
    textColor: "primary-500"
    rounded: "10px"
    size: "2.5rem"
---

# Design System: Inkwell

## 1. Overview

**Creative North Star: "The Traditional Meets Technical"**

Inkwell now shares Fathom's visual register: a dark, teal-tinted page, large editorial type, generous rounded surfaces and one bright mint accent. The palette is deliberately narrow — near-black teal for the page, a slightly raised teal for cards and a soft radial glow behind the top of the page. The effect is quiet and bookish rather than glossy: ink on a dark desk, with a single mint mark.

This is a deliberate departure from the previous green-on-near-white system. There is no light mode and no `light-dark()` parity: the site is dark-only, and `color-scheme: dark` is declared in the document and in the tokens. The old green accent, mono navigation and flat white cards are gone.

**Key Characteristics:**

- Dark-only: `background-50` (`#0B1416`) page, with a soft mint radial glow at the top and no light scheme
- High contrast: body `text-900`, headings `text-950`, muted copy `text-700`; every combination checked against its surface
- Large, tight editorial type: a `5.2rem` maximum hero, `2.7rem` section headings and short, balanced measures
- One mint accent: `primary-500` / `accent-500` (`#2DD4BF`) for links, focus rings, eyebrow labels, active nav marks, the wordmark dot and primary buttons
- Tonal depth: 20px cards use a subtle vertical gradient and a hairline border; there are no drop shadows and no glass
- Pill actions and a 1080px (`67.5rem`) shell, matching Fathom's proportions
- Calm motion: exponential-eased entrances, decisive press feedback, everything collapsing under reduced motion
- Ecosystem-native: technical enough for the AT Protocol audience without turning the marketing site into a protocol explainer

## 2. Colors

Four ramps are used directly: `text-*` (mint-tinted light text), `background-*` (deep teal surfaces and borders), `primary-*` (mint/teal actions) and `accent-*` (the wordmark dot and selection). `secondary-*` is a cooler teal reserved for future use. All values are plain hex tokens; there is no light/dark pair.

| Ramp           | Key steps                                                              | Role                                                       |
| -------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------- |
| `text-*`       | `#78928F` at 600, `#8CA6A3` at 700, `#CFE2DE` at 900, `#E7F4F1` at 950 | Text hierarchy. `text-900` body, `text-950` headings       |
| `background-*` | `#0B1416` at 50, `#121F24` at 100, `#24383E` at 200, `#1A2B31` at 300  | Page, raised surfaces, hairlines and raised hover surfaces |
| `primary-*`    | `#2DD4BF` at 500, `#99F6E4` at 700, `#0F766E` at 300                   | Links, focus, active nav, eyebrow labels, primary buttons  |
| `accent-*`     | `#2DD4BF` at 500                                                       | Wordmark dot, selection and the accent mark                |

### Semantic aliases

Day-to-day component CSS reaches for these flat aliases rather than the raw ramp steps:

| Alias              | Resolves to                         |
| ------------------ | ----------------------------------- |
| `--color-accent`   | `accent-500` (`#2DD4BF`)            |
| `--color-muted`    | `text-700`                          |
| `--color-border`   | `background-200`                    |
| `--color-surface`  | `background-100`                    |
| `--surface-color`  | `background-200` (the hairline)     |
| `--surface-raised` | `background-100` (cards, nav panel) |
| `--surface-sunken` | `background-50` (the page itself)   |

### Named Rules

**The Dark-Only Rule.** The site has one colour scheme. Do not add a `prefers-color-scheme` branch, a theme toggle or a `light-dark()` token. A light version would be a separate visual system, not a variant of this one.

**The High-Contrast Rule.** Body text is `text-900` on `background-50`; headings are `text-950`. Muted copy is `text-700` and must clear 4.5:1 against the surface it sits on. Never use `text-500` or darker for body copy.

**The One Accent Rule.** Mint is the interactive signal: links, focus rings, list markers, the active nav underline, eyebrow labels, the wordmark dot and primary buttons. It is not a decorative wash. A primary button may be mint-filled, but its label is the dark teal `#042F2E`, never white.

**The Tonal + Border Rule.** Cards, lists and callouts step away from the page through a subtle gradient plus a 1px `surface-color` border. There are no drop shadows, no glass and no decorative blur. The gradient is a surface treatment, not a highlight.

## 3. Typography

**Display & Body:** Inter (self-hosted variable woff2, weights 400–800) with `ui-sans-serif, system-ui, sans-serif` fallback.
**Mono:** JetBrains Mono (self-hosted) with `ui-monospace, monospace` fallback. Mono is now reserved for code, compact row metadata, comparison headers and technical fragments — not navigation, the footer or page eyebrows.

### Hierarchy

- **Display** (700, fluid `clamp(2.8rem, 7.5vw, 5.2rem)`, line-height 1.04, -0.045em): hero `h1` only
- **Section title** (700, fluid `clamp(1.7rem, 3.5vw, 2.7rem)`, 1.12, -0.035em): landing-page section `h2`s and callout headings
- **Card heading** (700, `1.12rem`, 1.3): feature-card `h3`s
- **Section label** (700, `0.78rem`, uppercase, +0.12em, sans): platform labels and eyebrow-style section markers
- **Body** (400, `clamp(1.05rem, 2vw, 1.3rem)` for leads; default `--text-md`, 1.65): prose, capped at 42rem for copy and 70ch for legal prose
- **Label** (700, `0.9rem`): buttons
- **Mono** (400, `--text-xs`): code, `row-meta`, comparison column labels, raw URLs

### Named Rules

**The Typography-First Rule.** Hierarchy is established through weight, size and tracking — not through colour splashes. If a heading needs a bright accent to feel important, the weight or size is wrong.

**The One Display Rule.** The hero `h1` is the only element at Display size on any page.

## 4. Elevation

There is no shadow elevation. The page is flat and tonal: cards use a restrained vertical gradient from `surface-raised` to `background-50`, a 1px `surface-color` border and a 20px radius. Interaction states lift by 2px and shift the border toward mint; they never gain a drop shadow. The sticky nav is a solid `background-50` bar with a bottom hairline.

### Named Rules

**The No-Glass, No-Shadow Rule.** No `box-shadow`, no `backdrop-filter`, no glassmorphism. The only gradient is the surface treatment on cards/callouts and the radial page glow.

## 5. Motion

Motion mirrors the shared site language: entrance uses `--ease-out-expo` at `--duration-slow` (500ms) as a calm fade with a 12px upward drift; `.stagger-1`…`.stagger-5` delay in 100ms steps. Direct manipulation (button press, nav underline) uses `--ease-out-quart` at `--duration-fast` (200ms). `prefers-reduced-motion: reduce` collapses everything to near-instant globally.

### Named Rules

**The Feedback-Not-Decoration Rule.** Animation plays on content entrance and on direct manipulation only — never as ambient looping decoration.

## 6. Components

### Navigation

Solid sticky bar (`background-50`, 1px `surface-color` bottom border), brand mark (inline capsule SVG + wordmark) left, sans links right. Links are `text-700`, hover/active → `text-950` with a 2px `primary-500` inset underline. At `≤800px` the toggle (44×44) reveals an in-flow dropdown panel (`surface-raised`, 20px radius, bordered) with full-width links. Escape and route changes close it; focus returns to the toggle.

### Buttons

Pills with `--radius-full`, a 2.9rem minimum height and `0.7rem 1.15rem` padding. Primary = `primary-500` fill, `#042F2E` label, hover → `primary-700`. Outline = transparent background, `surface-color` border, `text-950` label, hover → 10% mint tint + `primary-500` border. Both press at `scale(0.98)`. Primary CTAs: hero "Get Inkwell" → `#download`; "Add AltStore source" / "Add F-Droid repo" on the download cards.

### Feature cards & callouts

20px radius, 1.5rem padding, 1px `surface-color` border and a `linear-gradient(180deg, surface-raised, background-50)` surface. Hover → lighter gradient, mint-tinted border and a 2px lift. Icon (2.5rem tile with a 12% mint tint, mint glyph) → heading → body. Callouts are a narrower (42rem) left-aligned variant with an inline mint link.

### Editorial rows

Raised list surfaces (`surface-raised` + border, 14px radius) containing baseline-aligned rows: title left, mono `row-meta` right. Hover → `background-300`. Used for any "list of items" content.

### Screenshot frames

1px `surface-color` border, 16px radius, on `surface-raised`; sans caption beneath. No shadow — the mockups read as flat illustrations.

### Prose (privacy / terms)

Max-width 70ch, `text-900`, line-height 1.75. Links are `primary-500` underlined in a 35% mint tint; headings `text-950`. Code is mono on a `surface-raised` chip. List markers are `primary-500`.

## 7. Do's and Don'ts

### Do:

- **Do** keep body text `text-900` on `background-50` and headings `text-950` — contrast is the system's signature
- **Do** treat the site as dark-only; update tokens rather than adding a light variant
- **Do** use mint (`primary-500`) for interactive signals and primary actions
- **Do** differentiate surfaces with a subtle gradient + 1px border, never a shadow
- **Do** use `text-wrap: balance` on headings and `text-wrap: pretty` on prose
- **Do** cap body text at 60–70ch for legal prose and 42rem for marketing leads
- **Do** test every colour against its background for ≥4.5:1 (body) and ≥3:1 (large text ≥18px or bold ≥14px)
- **Do** provide a `prefers-reduced-motion` fallback for every animation (handled globally in `system.css`)
- **Do** use sans for nav, footer and eyebrows; reserve JetBrains Mono for code and compact metadata

### Don't:

- **Don't** add a light theme, `prefers-color-scheme` branch or theme toggle
- **Don't** use drop shadows, `backdrop-filter` or glassmorphism
- **Don't** make a primary button an inverse white/dark pair — primary actions are mint with dark teal labels
- **Don't** use the SaaS startup template kit: no gradient hero images, no animated counters, no glassmorphism
- **Don't** set body copy lighter than `text-700` against the page background
- **Don't** nest cards inside cards — if a container needs a sub-container, reconsider the layout
- **Don't** animate CSS layout properties — use `transform` and `opacity` only
- **Don't** over-explain the AT Protocol to the audience — link to atproto.com for background; the site sells the app, not the protocol
