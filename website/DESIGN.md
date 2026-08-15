---
name: Inkwell
description: A calm, editorial brand system for the AT Protocol publishing ecosystem — typography-first, cool-toned, with a single app-matched accent and restrained, native-feeling motion.
colors:
  ink:
    value: "oklch(0.30 0.06 260)"
    dark: "oklch(0.68 0.05 255)"
    role: primary-text
  paper:
    value: "oklch(0.98 0.01 240)"
    dark: "oklch(0.14 0.04 255)"
    role: primary-bg
  surface:
    value: "oklch(0.96 0.02 245)"
    dark: "oklch(0.20 0.05 258)"
    role: surface-bg
  muted:
    value: "oklch(0.40 0.055 258)"
    dark: "oklch(0.62 0.06 258)"
    role: muted-text
  border:
    value: "oklch(0.90 0.03 248)"
    dark: "oklch(0.28 0.06 260)"
    role: border
  accent:
    value: "oklch(0.48 0.28 142)"
    dark: "oklch(0.60 0.22 146)"
    canonical: "#139500"
    role: primary-accent
  accent-light:
    value: "oklch(0.52 0.26 142)"
    dark: "oklch(0.58 0.16 144)"
    role: primary-accent-hover
typography:
  display:
    fontFamily: "Inter Variable, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 800
    fontSize: "clamp(2rem, 4vw + 1rem, 3.052rem); clamp(2.25rem, 5vw + 1rem, 3.815rem) at sm+"
    lineHeight: 1.15
    letterSpacing: "-0.025em"
  headline:
    fontFamily: "Inter Variable, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 700
    fontSize: "clamp(1.75rem, 3vw + 1rem, 2.441rem)"
    lineHeight: 1.2
  title:
    fontFamily: "Inter Variable, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 700
    fontSize: "1.953rem"
    lineHeight: 1.25
  body:
    fontFamily: "Inter Variable, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 400
    fontSize: "1rem"
    lineHeight: 1.6
  label:
    fontFamily: "Inter Variable, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 600
    fontSize: "0.875rem"
    lineHeight: 1.4
  mono:
    fontFamily: "JetBrains Mono Variable, ui-monospace, monospace"
    fontWeight: 400
    fontSize: "0.875rem"
    lineHeight: 1.5
rounded:
  xs: "0.25rem"
  sm: "0.375rem"
  md: "0.5rem"
  lg: "0.75rem"
  xl: "1rem"
  full: "9999px"
spacing:
  "4xs": "0.125rem"
  "3xs": "0.25rem"
  "2xs": "0.5rem"
  xs: "0.75rem"
  sm: "1rem"
  md: "1.5rem"
  lg: "2rem"
  xl: "3rem"
  "2xl": "4rem"
  "3xl": "6rem"
motion:
  duration:
    instant: "100ms"
    fast: "150ms"
    normal: "250ms"
    slow: "400ms"
  easing:
    out-quart: "cubic-bezier(0.25, 1, 0.5, 1)"
    out-expo: "cubic-bezier(0.16, 1, 0.3, 1)"
    spring: "cubic-bezier(0.34, 1.56, 0.64, 1)"
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "#ffffff"
    rounded: "{rounded.full}"
    padding: "0.5rem 1.5rem"
    typography: "{typography.label}"
  button-primary-hover:
    backgroundColor: "accent-300"
  button-primary-active:
    backgroundColor: "accent-700"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.accent}"
    border: "1px solid {colors.accent}"
    rounded: "{rounded.full}"
    padding: "0.5rem 1.5rem"
    typography: "{typography.label}"
  button-outline-hover:
    backgroundColor: "accent-50"
  feature-card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.lg}"
    padding: "2rem"
    boxShadow: "0 4px 8px -2px oklch(0 0 0 / 0.06)"
    boxShadowHover: "0 6px 16px -4px oklch(0 0 0 / 0.1)"
  feature-icon:
    backgroundColor: "accent-50"
    textColor: "{colors.accent}"
    rounded: "{rounded.md}"
    size: "2.5rem"
---

# Design System: Inkwell

## 1. Overview

**Creative North Star: "The Writer's Desk"**

A clean, well-lit workspace. Tools within reach, nothing on the surface that isn't used. The design recedes so the words can speak — typography carries the page, color is deployed with restraint, and every element earns its place.

Inkwell's visual system is built for a marketing landing page that sells a native iOS reader/writer for the Standard.site publishing ecosystem on AT Protocol. It sits alongside sibling projects — Standard.site (the protocol layer: technical manifesto, code-as-documentation, "One schema. Every platform."), Leaflet (the publishing platform: "Delightful publishing," approachable sophistication, creator-first), and Pckt (the indie blog layer: utilitarian minimalism, action-oriented). Inkwell is the native iOS client that bridges these worlds — it should feel like it belongs to this ecosystem while carving its own space.

The personality is calm, trustworthy, and editorial. The system draws from iA Writer's minimalism, Linear's blog clarity, and The Browser Company's purpose-first marketing — but also from the AT Protocol ecosystem's indie-web confidence: technical without being cold, warm without being beige, polished without being corporate. The web client also borrows a second reference point: the native iOS app itself. Where the web and app share a vocabulary (button shape, shadow depth, spring timing), the web app-matches rather than invents its own — it should feel like the same product wearing a browser, not a separate marketing skin.

This system explicitly rejects: SaaS startup templates with gradient heroes and animated metric counters, over-designed agency portfolios with decorative glassmorphism (the one exception is the sticky header's translucent scrim — used functionally to keep navigation legible over scrolling content, not as a decorative frosted-glass card), and AI-generation clichés (warm-tinted cream body backgrounds masquerading as "editorial warmth," tiny tracked eyebrow labels above every section, numbered section markers, gradient text, side-stripe borders on cards).

**Key Characteristics:**
- Typography-first hierarchy: weight, size, and spacing do the work; color is secondary
- Cool-toned base: the palette leans into a crisp, cool off-white — warmth comes from typography, accent placement, and content voice, not from tinted backgrounds
- App-matched elevation: surfaces are differentiated by tonal value first, with a soft shadow mirrored from the iOS app's own elevation treatment — subtle at rest, slightly stronger on hover, never decorative
- Single-accent discipline: one accent (a vivid green, matched to the iOS app icon's ink-drop mark) used sparingly; its rarity is the point
- Light/dark parity: every color is a `light-dark()` pair, defined once per token rather than re-derived per component
- Tactile but calm interaction: hover/press states combine a tonal shift with a small lift or press-scale, timed to approximate the app's native spring feel — never a bounce or pulse, and never on content entrance, which stays a single calm fade
- Ecosystem-native: the design feels at home next to Standard.site, Leaflet, and Pckt — indie polish, not corporate sheen

## 2. Colors

A cool-toned ink/paper palette with one accent, each defined as a short ramp (50 / 100 / 200 / 300 / 500 / 700 / 900) rather than a single flat value, so one token family can serve text, borders, and surfaces without introducing new hues. Every step is a `light-dark(light, dark)` pair in OKLCH — light/dark parity lives in the token, not in per-component overrides.

### Primary — Accent (green)
The single brand accent (hue ≈142, a vivid green) — chosen to match the ink-drop mark in the iOS app icon (canonical `#139500`, the wordmark SVG's fallback value). `accent-500` carries primary actions, links, and feature icons; `accent-50` / `accent-100` are tint backgrounds (feature-icon fill, outline-button hover fill); `accent-300` / `accent-700` are hover/active states; `accent-900` is reserved for high-contrast accent text if ever needed. Used with the same restraint as before — one focal accent per section. The dark-mode `accent-500` lightness (0.60) is tuned to clear 4.5:1 against `surface-color`/`paper-200` — the original 0.56 measured 4.41:1, just under AA for normal text.

### Neutral — Ink (text/foreground) & Paper (background/surface)
Both ramps sit in the same cool hue family (≈240–262) so neutrals and surfaces read as one consistent world rather than competing temperatures. `ink-700` is default body text; `ink-900` is reserved for emphasis (`<strong>`, the nav brand mark, active nav state); `ink-600` is the muted/secondary-text alias — its dark-mode lightness (0.62) is tuned to clear 4.5:1 against `surface-color`/`paper-200`, since the original 0.58 measured 4.24:1. `paper-100` is the page background; `paper-200` (aliased `--surface-color`) lifts cards and callouts; `paper-50` (aliased `--surface-raised`) is the hover-lifted card surface; `paper-300` (aliased `--color-border`) is the hairline border used for every divider and card edge.

### Semantic aliases
Day-to-day component CSS reaches for these flat aliases rather than the raw ramp steps:

| Alias | Resolves to |
|---|---|
| `--color-ink` | `ink-700` |
| `--color-paper` | `paper-100` |
| `--color-surface` | `paper-200` |
| `--color-muted` | `ink-600` |
| `--color-border` | `paper-300` |
| `--color-accent` | `accent-500` |
| `--color-accent-light` | `accent-300` |

### Named Rules
**The One Accent Rule.** Accent green is used on ≤10% of any given screen. A primary button, a link, a feature icon — pick one focal point per section. The ramp gives it hover/active/tint depth without ever introducing a second hue.

**The Cool Canvas Rule.** The Paper ramp stays cool (hue ≈240–262), not warm. Warmth in this brand is carried by typographic authority and accent placement, not by beige backgrounds. A warm off-white body bg is the single most common AI-generation tell; this rule exists to prevent it.

**The Tonal + Border + Shadow Rule** (formerly the Tonal Surface Rule). Cards and containers step up from the background through tonal value (Surface vs Paper) and a 1px border first; a soft, app-matched shadow (see §4) adds the final bit of lift. If a card can't be told apart from the page with the shadow turned off, the tonal/border step is too small — the shadow is a finishing touch, not the mechanism.

## 3. Typography

**Display & Body Font:** Inter Variable (weights 100–900, with `ui-sans-serif, system-ui, sans-serif` fallback)
**Mono Font:** JetBrains Mono Variable (weights 100–800, with `ui-monospace, monospace` fallback)

**Character:** Inter is a pragmatic workhorse — highly readable at body sizes, authoritative at display weights. Its closed apertures and tall x-height give it a modern, no-nonsense feel that matches the brand's editorial confidence. JetBrains Mono is used only for code samples (privacy/terms `<code>`); it's present but intentionally invisible. The pairing is single-family with a mono utility — no serif/sans tension to manage. The type scale follows a 1.25 major-third ratio.

### Hierarchy
- **Display** (800 weight, fluid `clamp(2rem, 4vw + 1rem, 3.052rem)` → `clamp(2.25rem, 5vw + 1rem, 3.815rem)` at `sm:`, 1.15 line-height, -0.025em tracking): Hero headline only. One per page.
- **Headline** (700 weight, fluid `clamp(1.75rem, 3vw + 1rem, 2.441rem)`, 1.2 line-height): Section titles (h2). Now fluid rather than fixed, so it scales down gracefully on narrow viewports instead of clamping line-wraps.
- **Title** (700 weight, 1.953rem, 1.25 line-height): Card headings, callout titles (e.g. the "Secure by design" callout heading uses this, not Headline).
- **Body** (400 weight, 1rem, 1.6 line-height): Default prose. Line length capped at 65–75ch via container width. `text-wrap: pretty` reduces orphans (now applied via `.prose` directly, not per-instance).
- **Label** (600 weight, 0.875rem, 1.4 line-height): Button text, navigation links, footer. Semibold for presence at small sizes.
- **Mono** (400 weight, 0.875rem, 1.5 line-height): Code snippets, technical labels. Present but not prominent.

### Named Rules
**The Typography-First Rule.** Visual hierarchy is established through weight, size, and spacing — never through color splashes. If a heading needs a color accent to feel important, the weight or size is wrong.

**The One Display Rule.** The hero `h1` is the only element at Display weight (800) on any page. Section headings step down to Headline (700).

## 4. Elevation

This system uses **app-matched elevation**, not flat-by-default. Surfaces still differentiate primarily through tonal value (the Surface token sits a step above Paper in OKLCH lightness) and a 1px border — but feature cards now also carry a soft shadow, deliberately matched to the iOS app's own SwiftUI shadow modifier on its cover images (`.shadow(color: .black.opacity(0.08), radius: 8, y: 4)`), so the web reader and the native reader feel like one product.

The shadow is restrained on purpose: `0 4px 8px -2px oklch(0 0 0 / 0.06)` at rest, stepping to `0 6px 16px -4px oklch(0 0 0 / 0.1)` on hover, paired with a 2px lift (`transform: translateY(-2px)`), a lightened background (`paper-50`), and a darkened border (`paper-300`). The combination reads as "this card just responded to you," not "this card has fake depth."

### Named Rules
**The App-Matched Elevation Rule.** Shadows appear only where they mirror an equivalent native-app treatment, and only intensify in response to direct interaction (hover/press) — never as ambient decoration on static elements. If a future shadow doesn't map to something the app already does, default to tonal value + border instead.

**The Functional-Glass Rule.** Translucency + blur (`backdrop-filter`) is reserved for the sticky header's scrim, where it serves a real purpose (keeping nav legible as content scrolls underneath). It does not appear on cards, callouts, or other static surfaces — that would be decorative glassmorphism, which this system still rejects.

## 5. Motion

Motion is deliberately split into two registers, each with its own easing, so entrance and interaction never compete for the same visual language.

**Entrance** uses `--ease-out-expo` (`cubic-bezier(0.16, 1, 0.3, 1)`) at `--duration-slow` (400ms): a calm fade with a small upward drift, never a bounce. `.animate-in` (fade + 16px slide) staggers across the feature grid via `.stagger-1`…`.stagger-5` (50ms/120ms/200ms/280ms/360ms delays); `.hero-reveal` (fade + scale 0.96→1) plays once on the hero block. Both use `animation-fill-mode: backwards` rather than `both` — the pre-animation state holds during the stagger delay, but the animation doesn't permanently lock the element's `transform` afterward, so later hover/press effects on the same element still work.

**Interaction feedback** uses `--ease-spring` (`cubic-bezier(0.34, 1.56, 0.64, 1)`, approximating a SwiftUI `.spring(response: 0.3, dampingFraction: 0.7)`) at `--duration-fast` (150ms) or `--duration-normal` (250ms): a slight, intentional overshoot on direct manipulation only — button press (`scale(0.97)`), feature-card hover lift (`translateY(-2px)`), `.hover-lift` / `.active-press` utilities. This is the one place the system allows something bounce-adjacent, because it's mimicking the app's own native button feedback, not decorating page content.

`prefers-reduced-motion: reduce` collapses all animation and transition durations to ~0 globally (`tokens.css`), so every effect above degrades to an instant state change.

### Named Rules
**The Feedback-Not-Decoration Rule.** `--ease-spring` is reserved for things the user directly touched (press, hover) — never for content entrance, exit, or anything that plays without input. Entrance always uses `--ease-out-expo` or `--ease-out-quart`.

## 6. Components

### Buttons
Buttons are capsule pills (`border-radius: full`), matching the app's `ReaderActionPill` component — a deliberate cross-platform echo, not a generic rounded-rectangle default.

- **Shape:** Full capsule radius, `0.5rem` vertical × `1.5rem` horizontal padding, `text-sm` Label weight (600).
- **Primary (`.btn-primary`):** Accent background, white text (ink-50 in dark mode). Hover → `accent-300`. Active → `accent-700` + `scale(0.97)`. Not currently used on the live page (no primary CTA is wired up — see Hero CTA below).
- **Outline (`.btn-outline`):** Transparent background, accent text and 1px accent border. Hover → `accent-50` fill. Active → `accent-100` fill + `scale(0.97)`. Currently the only button on the page (hero "View on GitHub").
- **Disabled:** `opacity: 0.4`, `cursor: not-allowed`, `pointer-events: none`.
- **Transition:** background-color at `--duration-normal` with `--ease-spring`; transform at `--duration-fast` with `--ease-spring`.
- **Focus-visible:** A global 2px accent outline with 2px offset (`tokens.css`), not a per-component ring.

### Cards / Containers
- **Corner Style:** Gently curved (0.75rem / `--radius-lg`).
- **Background:** `--surface-color` (paper-200) at rest, `--surface-raised` (paper-50) on hover.
- **Border:** 1px `--color-border` (paper-300) at rest, darkens to `paper-300`'s dark-mode-aware step on hover.
- **Shadow:** See §4 — present at rest, strengthens on hover, paired with a 2px lift.
- **Internal Padding:** 2rem (32px) all around.
- **Content:** Icon (2.5rem accent-tinted square, 0.5rem radius) → Heading (Title weight) → Body text. Equal-height cards in a responsive grid (`auto-fit, minmax(300px, 1fr)`, `--spacing-lg` gap).

### Callouts
A narrower, centered variant of the card surface (`max-width: 42rem`, same background/border as feature cards, no shadow): heading at Title weight/size, body text, and an inline accent link with an arrow icon. Used for the single "Secure by design" callout below the feature grid.

### Navigation
- **Header:** Sticky (`position: sticky; top: 0`), translucent blurred background (8px `backdrop-filter`) so it stays legible as content scrolls beneath it. 1px bottom border.
- **Brand mark:** 32×32px favicon + "Inkwell" at `text-xl`, weight 800 (Extrabold, not Title weight — the brand mark is treated as its own small wordmark, distinct from body headings).
- **Desktop:** Horizontal row of text links (`≥640px`), 0.875rem Semibold, muted color, 1.5rem gap. Hover → ink-900. Active route (`aria-current="page"`) → accent.
- **Mobile toggle:** 44×44px tap target, Menu/X icon, `aria-expanded` + `aria-controls` wired to the drawer.
- **Mobile drawer:** Renders in normal flow directly under the header (not an overlay), full-width, paper-100 background, 1px bottom border. Each link gets its own padding (12px/16px) so the tap target clears 44px independent of the tight 4px gap between items. Hover → tinted background + ink-900. Active route → accent. Closes on link click, backdrop click, or <kbd>Escape</kbd>.
- **Mobile backdrop:** Fixed, full-viewport, 20%-opacity black scrim behind the drawer; fades in/out (150ms) rather than sliding, since it has no meaningful "height" to animate.

### Footer
- 1px top border, `--spacing-lg` padding top and bottom.
- Stacks centered on mobile; becomes a row (copyright left, links right) at `≥640px`.
- Copyright line: muted text with two inline links (AT Protocol, Standard.site) that keep an explicit underline, since they're embedded in flowing prose rather than presented as a list — consistent with how `.prose a` is treated elsewhere.
- Footer links (Privacy / Terms / GitHub): a separate labeled `<nav>`, color + hover-to-accent only, no underline — they're already visually set apart as a list, so color is a sufficient affordance.

### Hero CTA
Two buttons: `.btn.btn-primary` ("Get Inkwell", anchors to `#download`) paired with `.btn.btn-outline` ("View source", GitHub), both with `.hover-lift` and `.active-press`. There is still no App Store or Play Store badge — the primary button scrolls to the Download section rather than linking a store listing that doesn't exist.

### Download section
A second feature-grid pair, directly below the hero, presenting the two real install paths: an AltStore source (iOS) and a self-hosted F-Droid repo (Android, labelled experimental). Each card follows the standard `.feature-card` shape (icon → heading → body) plus a `.btn-primary` install action and a `text-sm text-muted` follow-up line for prerequisites/alternate links. This is the only place `.btn-primary` appears outside the hero — kept to one per card so the accent-button budget stays deliberate.

### Feature Icons
2.5rem (40px) square container, 0.5rem radius, `accent-50` background, accent fill on the Lucide icon. Icon size: 1.25rem (20px).

## 7. Do's and Don'ts

### Do:
- **Do** establish hierarchy through typography (weight, size, spacing) before reaching for color
- **Do** use the single accent (green, matched to the app icon) on ≤10% of any given screen — a button, a link, or an icon, not all three
- **Do** differentiate cards from the background through tonal value and a 1px border first; reserve the app-matched shadow (§4) as the finishing touch, not the primary mechanism
- **Do** keep the body background cool-toned (hue ≈240–262) — the AT Protocol ecosystem is muted and technical, not warm and beige
- **Do** use `text-wrap: balance` on h1–h3 and `text-wrap: pretty` on body prose
- **Do** cap body text lines at 65–75ch
- **Do** test every color against its background for ≥4.5:1 contrast (body) and ≥3:1 (large text ≥18px or bold ≥14px)
- **Do** provide a `prefers-reduced-motion` fallback for every animation (handled globally in `tokens.css`)
- **Do** support both light and dark color schemes via `light-dark()` on every token
- **Do** reserve `--ease-spring` for direct-manipulation feedback (press, hover) — never for content entrance/exit, which stays on `--ease-out-expo` / `--ease-out-quart`
- **Do** match shadow and motion values to the native iOS app's own SwiftUI modifiers where one exists (the feature-card shadow, the spring timing) — the web should feel like the same product, not a separate marketing skin
- **Do** trust the reader — the AT Protocol audience is technically literate; state the value clearly without over-explaining

### Don't:
- **Don't** use gradient text or side-stripe borders (`border-left`/`border-right` > 1px as a colored accent) — these are prohibited
- **Don't** introduce decorative glassmorphism (frosted cards, glossy panels). The sticky header's functional scrim is the one sanctioned exception
- **Don't** use the SaaS startup template kit: no gradient hero backgrounds, no animated metric counters, no "Pricing" tables, no generic tech-company sheen
- **Don't** add tiny uppercase tracked eyebrow labels ("ABOUT" / "FEATURES" / "PRICING") above section headings — if a section needs a label, rewrite the heading
- **Don't** use numbered section markers (01 / 02 / 03) unless the sections form a genuine ordered sequence where the number carries information
- **Don't** lean into warm-tinted cream/beige as the body background — the Paper ramp is cool-tinted, and warmth comes from typography, accent placement, and content voice
- **Don't** nest cards inside cards — if a container needs a sub-container, reconsider the layout
- **Don't** use `position: absolute` inside `overflow: hidden`/`overflow: auto` containers for dropdowns — use `<dialog>`, popover API, or `position: fixed` to escape the stacking context
- **Don't** animate CSS layout properties (width, height, top, left) — use `transform` and `opacity` for compositor-only animations
- **Don't** add a shadow anywhere that doesn't mirror something the native app already does — see the App-Matched Elevation Rule
- **Don't** over-explain the AT Protocol to the audience — link to atproto.com for background; the site sells the app, not the protocol
