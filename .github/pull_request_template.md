## What does this PR do?

<!-- Summarize the change and the motivation. Link the issue it closes (e.g. "Closes #7"). -->

## Platform(s)

<!-- Check all that apply: -->
- [ ] iOS
- [ ] Android
- [ ] Website
- [ ] All

## Checklist

- [ ] I have read the platform-specific `AGENTS.md` for any touched code
- [ ] I have run the relevant build / lint / tests for the platform(s) changed
  - iOS: `xcodebuild ... build test`
  - Android: `./gradlew clean assembleDebug lint test`
  - Website: `pnpm install --frozen-lockfile && pnpm check && pnpm build`
- [ ] I have not committed secrets, keys, or credentials
- [ ] I have updated any affected documentation or metadata (e.g. `source.json`, F-Droid YAML, `AGENTS.md`)
- [ ] My commits are atomic and scoped (one logical change per commit)

## Notes for reviewers

<!-- Anything specific to look out for, edge cases, or follow-up work. -->
