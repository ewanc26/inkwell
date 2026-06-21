---
name: Inkwell
description: A calm, editorial brand system for the AT Protocol publishing ecosystem — typography-first, cool-toned, deliberately restrained.
colors:
  ink:
    value: "#1a1a2e"
    role: primary-text
  paper:
    value: "#f7f6fc"
    role: primary-bg
    canonical: oklch(0.97 0.005 260)
  accent-indigo:
    value: "#4f46e5"
    role: primary-accent
  accent-indigo-light:
    value: "#818cf8"
    role: primary-accent-hover
  surface:
    value: "#f8f8fe"
    role: surface-bg
    canonical: oklch(0.98 0.01 260)
  muted:
    value: "#7d7f8c"
    role: muted-text
    canonical: oklch(0.55 0.02 260)
typography:
  display:
    fontFamily: "Inter Variable, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 800
    fontSize: "3.052rem"
    lineHeight: 1.15
    letterSpacing: "-0.025em"
  headline:
    fontFamily: "Inter Variable, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 700
    fontSize: "2.441rem"
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
components:
  button-primary:
    backgroundColor: "{colors.accent-indigo}"
    textColor: "#ffffff"
    rounded: "{rounded.lg}"
    padding: "0.75rem 2rem"
    typography: "{typography.label}"
  button-primary-hover:
    backgroundColor: "{colors.accent-indigo-light}"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.accent-indigo}"
    rounded: "{rounded.lg}"
    padding: "0.75rem 2rem"
    typography: "{typography.label}"
  button-outline-hover:
    backgroundColor: "oklch(from {colors.accent-indigo} l c h / 0.1)"
  feature-card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.lg}"
    padding: "2rem"
  feature-icon:
    backgroundColor: "oklch(from {colors.accent-indigo} l c h / 0.1)"
    textColor: "{colors.accent-indigo}"
    rounded: "{rounded.md}"
    size: "2.5rem"
---

# Design System: Inkwell

## 1. Overview

**Creative North Star: "The Writer's Desk"**

A clean, well-lit workspace. Tools within reach, nothing on the surface that isn't used. The design recedes so the words can speak — typography carries the page, color is deployed with restraint, and every element earns its place.

Inkwell's visual system is built for a marketing landing page that sells a native iOS reader/writer for the Standard.site publishing ecosystem on AT Protocol. It sits alongside sibling projects — Standard.site (the protocol layer: technical manifesto, code-as-documentation, "One schema. Every platform."), Leaflet (the publishing platform: "Delightful publishing," approachable sophistication, creator-first), and Pckt (the indie blog layer: utilitarian minimalism, action-oriented). Inkwell is the native iOS client that bridges these worlds — it should feel like it belongs to this ecosystem while carving its own space.

The personality is calm, trustworthy, and editorial. The system draws from iA Writer's minimalism, Linear's blog clarity, and The Browser Company's purpose-first marketing — but also from the AT Protocol ecosystem's indie-web confidence: technical without being cold, warm without being beige, polished without being corporate. The ecosystem has personality (Leaflet's "secret third thing," Standard.site's manifesto energy, Pckt's directness); Inkwell's site should match that confidence.

This system explicitly rejects: SaaS startup templates with gradient heroes and animated metric counters, over-designed agency portfolios with glassmorphism, and AI-generation clichés (warm-tinted cream body backgrounds masquerading as "editorial warmth," tiny tracked eyebrow labels above every section, numbered section markers, gradient text, side-stripe borders on cards).

**Key Characteristics:**
- Typography-first hierarchy: weight, size, and spacing do the work; color is secondary
- Cool-toned base: the palette leans into a crisp, cool off-white — warmth comes from typography, accent placement, and content voice, not from tinted backgrounds
- Flat elevation: surfaces are differentiated by tonal value alone — no box-shadows
- Single-accent discipline: one indigo accent used sparingly; its rarity is the point
- Light/dark parity: every color decision works in both schemes via `light-dark()`
- Restrained interaction: hover states are subtle tonal shifts, not bounces or pulses
- Ecosystem-native: the design feels at home next to Standard.site, Leaflet, and Pckt — indie polish, not corporate sheen

## 2. Colors

A restrained ink-and-paper palette with one accent. The system uses six tokens total — no secondary or tertiary colors, no extended ramp. The accent's power comes from its scarcity. The palette is deliberately cool-toned (hue 260 across all neutrals) to sit naturally alongside the AT Protocol ecosystem's muted, technical aesthetic and to avoid the warm-cream AI default.

### Primary
- **Accent Indigo** (#4f46e5): The single brand accent. Used on primary buttons, links, feature icons, and the hero wordmark highlight. Appears on ≤10% of any given screen. Its lighter variant **Accent Indigo Light** (#818cf8) is used for hover states and selection highlights in dark mode.

### Neutral
- **Ink** (#1a1a2e): Primary text color in light mode, background in dark mode. A deep navy-black — not pure black, carrying a hint of blue that softens the contrast without sacrificing readability.
- **Paper** (#f7f6fc, canonical `oklch(0.97 0.005 260)`): Primary background in light mode, text color in dark mode. A cool off-white with a barely-perceptible blue tint. The cool cast is deliberate: it rejects the warm-cream AI default, aligns with the AT Protocol ecosystem's muted-technical aesthetic, and creates a crisp canvas where the indigo accent reads as purposeful rather than decorative.
- **Surface** (#f8f8fe, canonical `oklch(0.98 0.01 260)`): Elevated surface background — used on feature cards and bordered callout sections. Slightly lighter and more chromatic than Paper, creating tonal distinction without shadows. Same hue family (260) so the step from Paper to Surface reads as a lift, not a color change.
- **Muted** (#7d7f8c, canonical `oklch(0.55 0.02 260)`): Secondary text — body copy on the landing page, footer text, navigation links. A slate with enough weight to clear 4.5:1 contrast against both light and dark backgrounds.

### Named Rules
**The One Accent Rule.** Accent Indigo is used on ≤10% of any given screen. A primary button, a link, a feature icon — pick one focal point per section. If everything is accented, nothing is.

**The Cool Canvas Rule.** The Paper background is cool-tinted (hue 260), not warm. Warmth in this brand is carried by typographic authority and accent placement, not by beige backgrounds. A warm off-white body bg is the single most common AI-generation tell; this rule exists to prevent it.

**The Tonal Surface Rule.** Cards and containers are distinguished from the background by tonal value alone (a 0.01 L difference in OKLCH plus a chroma step), never by borders heavier than 1px or by box-shadows. If you can't tell a card from the page without a shadow, the tonal step is too small.

## 3. Typography

**Display & Body Font:** Inter Variable (weights 100–900, with `ui-sans-serif, system-ui, sans-serif` fallback)
**Mono Font:** JetBrains Mono Variable (weights 100–800, with `ui-monospace, monospace` fallback)

**Character:** Inter is a pragmatic workhorse — highly readable at body sizes, authoritative at display weights. Its closed apertures and tall x-height give it a modern, no-nonsense feel that matches the brand's editorial confidence. JetBrains Mono is used only for code samples (if ever shown); it's present but intentionally invisible. The pairing is single-family with a mono utility — no serif/sans tension to manage. The type scale follows a 1.25 major-third ratio.

### Hierarchy
- **Display** (800 weight, 3.052rem / clamp for hero, 1.15 line-height, -0.025em tracking): Hero headlines only. One per page. The weight contrast (Extrabold against regular body) creates hierarchy without color or size gymnastics.
- **Headline** (700 weight, 2.441rem, 1.2 line-height): Section titles (h2). Bold enough to anchor a section without competing with the hero.
- **Title** (700 weight, 1.953rem, 1.25 line-height): Card headings, callout titles. Pairs with body text inside bounded containers.
- **Body** (400 weight, 1rem, 1.6 line-height): Default prose. Line length capped at 65–75ch via container width. `text-wrap: pretty` to reduce orphans.
- **Label** (600 weight, 0.875rem, 1.4 line-height): Button text, navigation links, footer. Semibold for presence at small sizes.
- **Mono** (400 weight, 0.875rem, 1.5 line-height): Code snippets, technical labels. Present but not prominent.

### Named Rules
**The Typography-First Rule.** Visual hierarchy is established through weight, size, and spacing — never through color splashes. If a heading needs a color accent to feel important, the weight or size is wrong.

**The One Display Rule.** The hero `h1` is the only element at Display weight (800) on any page. Section headings step down to Headline (700). Two display-weight elements on the same page compete; one wins.

## 4. Elevation

This system is **flat by default**. Surfaces are differentiated through tonal value alone — the Surface token sits 0.01 L above Paper in OKLCH with a subtle chroma increase, creating a barely-perceptible lift that's enough to distinguish a card or callout from the page background. No box-shadows are used anywhere in the current system.

Depth is conveyed through content density and spacing, not simulated z-height. This is a deliberate choice: shadows imply UI layering (overlays, modals, drawers), and a marketing landing page doesn't need that vocabulary. The flat approach also keeps the system honest — nothing feels "clickable" through artificial affordance; buttons earn their prominence through color and placement.

### Named Rules
**The Flat-at-Rest Rule.** Surfaces are flat at rest. If shadows are ever introduced (for a modal, dropdown, or tooltip in a future app shell), they appear only as a response to functional need, never as decoration.

**The No-Shadow Rule.** Box-shadow is prohibited on cards, containers, and static elements. Tonal differentiation is the only mechanism for distinguishing surfaces from the background.

## 5. Components

### Buttons
Buttons feel considered, not urgent. Hover states are subtle tonal shifts — no scale transforms, no bounces, no pulses. Confidence through calm.

- **Shape:** Gently rounded (0.75rem / `--radius-lg`) with no border on primary, 1.5px accent border on outline variant.
- **Primary (`.btn-primary`):** Accent Indigo background, white text, 0.75rem vertical × 2rem horizontal padding, 600 weight. Hover: lightens to Accent Indigo Light. Transition: 150ms ease on background-color only.
- **Outline (`.btn-outline`):** Transparent background, Accent Indigo text and 1.5px border. Hover: fills with 10% accent opacity. Same padding and transition as primary.
- **Focus-visible:** Both variants use the default browser focus ring (no custom ring to maintain — the accent color on the outline variant provides sufficient focus distinction).

### Cards / Containers
- **Corner Style:** Gently curved (0.75rem / `--radius-lg`).
- **Background:** Surface (#f8f8fe) in light mode, a darker tinted neutral (`oklch(0.2 0.01 260)`) in dark mode.
- **Shadow Strategy:** None. Cards are distinguished from the background by tonal value and a 1px subtle border (`oklch(0.9 0.01 260)` light / `oklch(0.25 0.01 260)` dark).
- **Internal Padding:** 2rem (32px) all around.
- **Content:** Icon (2.5rem accent-tinted square, 0.5rem radius) → Heading (Title weight) → Body text (Muted color). Equal-height cards in a responsive grid.

### Navigation
- **Desktop:** Horizontal row of text links, 0.875rem Semibold, Muted color. Hover shifts to Ink (light mode) or Paper (dark mode). 1.5rem gap between items. No underlines, no background pills.
- **Mobile:** Vertical list in a slide-down drawer, revealed by a Menu/X icon toggle. Full-width, border-bottom separator. Same typography as desktop, larger touch target.
- **Brand mark:** Favicon (32×32px ink bottle icon) + "Inkwell" in Title weight (1.25rem, 700) — no tagline.

### App Store Badge
- Fixed height (3rem), auto width. No hover effect — it's an image, not a styled button. Sits alongside the GitHub button in the hero.

### Feature Icons
- 2.5rem (40px) square container, 0.5rem radius, 10% accent opacity background, Accent Indigo fill on the Lucide icon. Icon size: 1.25rem (20px). Purpose: visual anchors that make the feature grid scannable without competing with headings.

## 6. Do's and Don'ts

### Do:
- **Do** establish hierarchy through typography (weight, size, spacing) before reaching for color
- **Do** use the single accent (Accent Indigo, #4f46e5) on ≤10% of any given screen — a button, a link, or an icon, not all three
- **Do** differentiate cards from the background through tonal value (Surface vs Paper), never through box-shadows
- **Do** keep the body background cool-toned (hue 260) — the AT Protocol ecosystem is muted and technical, not warm and beige
- **Do** use `text-wrap: balance` on h1–h3 for even line lengths
- **Do** cap body text lines at 65–75ch
- **Do** test every color against its background for ≥4.5:1 contrast (body) and ≥3:1 (large text ≥18px or bold ≥14px)
- **Do** provide a `prefers-reduced-motion` fallback (instant or crossfade) for every animation
- **Do** support both light and dark color schemes — every color assignment must use `light-dark()` or have a dark-mode counterpart
- **Do** trust the reader — the AT Protocol audience is technically literate; the site should state the value clearly without over-explaining

### Don't:
- **Don't** use gradient text, side-stripe borders (`border-left`/`border-right` > 1px as a colored accent), or glassmorphism — these are prohibited
- **Don't** use the SaaS startup template kit: no gradient hero backgrounds, no animated metric counters, no "Pricing" tables, no generic tech-company sheen
- **Don't** add tiny uppercase tracked eyebrow labels ("ABOUT" / "FEATURES" / "PRICING") above section headings — if a section needs a label, rewrite the heading
- **Don't** use numbered section markers (01 / 02 / 03) unless the sections form a genuine ordered sequence where the number carries information. Standard.site uses numbered principles (01–04) because they're a genuine ordered specification; don't copy the surface tic without the structural reason.
- **Don't** lean into warm-tinted cream/beige as the body background — the Paper token is cool-tinted (hue 260), and warmth comes from typography, accent placement, and content voice
- **Don't** nest cards inside cards — if a container needs a sub-container, reconsider the layout
- **Don't** use `position: absolute` inside `overflow: hidden`/`overflow: auto` containers for dropdowns — use `<dialog>`, popover API, or `position: fixed` to escape the stacking context
- **Don't** animate CSS layout properties (width, height, top, left) — use `transform` and `opacity` for compositor-only animations
- **Don't** over-explain the AT Protocol to the audience — link to atproto.com for background; the site sells the app, not the protocol
