# Product

## Register

brand

## Users

AT Protocol natives, writers and bloggers who care about data ownership, and iOS early adopters discovering decentralized tools. They arrive from app store links, GitHub, AT Protocol directories, or word of mouth — often on mobile, mid-scroll, deciding in seconds whether to download. The primary job: understand what Inkwell does, trust that it's legitimate and private, and install it.

## Product Purpose

Inkwell is a native reader and writer for the Standard.site publishing ecosystem on AT Protocol — primarily iOS (SwiftUI), with an experimental Android port (Jetpack Compose). It exists because decentralized publishing deserves a first-class native client — not a web view, not a Bluesky tab. The website's job is to convert curiosity into downloads by showing what the app does, how it respects user data, and why AT Protocol matters. Success is a visitor who reads the hero, scans the features, and adds the AltStore source or F-Droid repo (or, for the technically curious, clones the GitHub source).

## Brand Personality

Calm, trustworthy, editorial — the confidence of restraint. Like iA Writer's minimalism, Linear's blog clarity, and The Browser Company's purpose-first marketing. The site should feel like a quiet reading room: everything present has a reason, nothing is decorative. Typography carries the page. The warmth is in the idea (your writing, your control), not in beige backgrounds.

## Anti-references

- **SaaS startup template**: no gradient-heavy hero sections, no animated metric counters, no "Pricing" tables, no generic tech-company sheen
- **Over-designed portfolio/agency aesthetic**: no trendy glassmorphism, no excessive animation, no design flexing that overshadows the product
- **AI slop defaults**: no warm-tinted cream/paper body backgrounds carrying "warmth," no tiny tracked eyebrow labels above every section, no numbered section markers, no gradient text, no side-stripe borders on cards

## Design Principles

1. **Restraint is confidence.** Fewer elements, each chosen deliberately. If something can be removed without losing meaning, remove it.
2. **Typography carries the page.** Strong hierarchy through weight, size, and spacing — not through color splashes or heavy imagery. The words do the work.
3. **Trust through transparency.** The product is decentralized, private, and open-source. The site should feel honest, open, and never manipulative. No dark patterns, no urgency tricks.
4. **Singular focus.** Each page does one thing well. The landing page sells the app — nothing else. No feature-checklist sprawl, no link farms.
5. **Editorial calm.** The feeling of a quiet reading room, not a sales floor. Space to breathe. Content over chrome.

## Accessibility & Inclusion

- Target: WCAG 2.1 AA
- All interactive elements keyboard-accessible
- Respect `prefers-reduced-motion` — all animations must have a reduced-motion alternative
- Support light and dark color schemes (already wired via `light-dark()`)
- Semantic HTML with appropriate heading levels and landmark regions
- Body text contrast ≥ 4.5:1 against background; large text ≥ 3:1
