# Git Commit Message Templates

## Standard Format
```
[TYPE]: Brief description (50 chars max)

Detailed explanation of changes:
- What was changed
- Why it was changed
- Impact of the change

Closes #issue-number (if applicable)
```

## Commit Types
- **feat:** New feature implementation
- **fix:** Bug fix
- **refactor:** Code refactoring without functionality change
- **style:** Code style/formatting changes
- **docs:** Documentation updates
- **test:** Adding or updating tests
- **chore:** Build process, dependency updates, etc.
- **ui:** User interface changes
- **perf:** Performance improvements

## Examples

### Feature Addition
```
feat: Add voice-to-text note creation

- Integrated Android SpeechRecognizer
- Added microphone permission handling
- Created VoiceInputViewModel for state management
- Implemented continuous listening mode

Closes #15
```

### Bug Fix
```
fix: Resolve note sync conflict resolution

- Fixed merge conflict detection algorithm
- Updated UI to show conflict resolution options
- Added proper error handling for network failures

Closes #23
```

### Architecture Change
```
refactor: Migrate to simplified MVVM structure

- Removed complex multi-module setup
- Consolidated into single app module
- Updated package structure for better maintainability
- Simplified dependency injection setup
```

## Pre-commit Checklist
- [ ] Code follows project style guidelines
- [ ] Tests pass (unit and UI)
- [ ] Documentation updated if needed
- [ ] Feature progress tracker updated
- [ ] No debugging code or console logs left
- [ ] Proper error handling implemented