# 🚀 Axiom - Pre-Production Audit & Readiness Checklist

**Date:** October 2025
**Version:** 1.0 (Pre-Release)
**Audit Scope:** Production readiness, security, stability, Play Store compliance

---

## 📊 Executive Summary

**Total Issues Identified:** 20
**Critical Blockers:** 5
**High Priority:** 5
**Medium Priority:** 5
**Technical Debt:** 5

**Estimated Time to Production Ready:** 3-4 weeks
**Minimum Viable Release:** 1-2 weeks (Critical + High priority only)

---

## ❌ CRITICAL BLOCKERS (Must Fix Before ANY Release)

### 🔴 1. ProGuard/R8 Configuration - SECURITY & SIZE ISSUE

**File:** `app/build.gradle.kts:28`

**Current State:**
```kotlin
release {
    isMinifyEnabled = false  // ❌ DANGEROUS!
    proguardFiles(...)
}
```

**Problem:**
- APK size ~10-15MB larger than necessary
- All code readable via reverse engineering (security risk)
- Dead code not removed
- No code obfuscation

**Fix:**
```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

**ProGuard Rules Needed:**
```proguard
# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt/Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep data classes
-keep class com.cosmiclaboratory.axiom.domain.model.** { *; }
-keep class com.cosmiclaboratory.axiom.data.database.entity.** { *; }
```

**Priority:** 🔴 CRITICAL
**Effort:** 2-3 hours
**Risk if skipped:** App reverse-engineered, poor performance

---

### 🔴 2. Error Handling - SILENT FAILURES

**Affected Files:**
- `NoteDetailScreen.kt:512` - "TODO: Show snackbar"
- `NotesListScreen.kt:131` - "TODO: Show snackbar or error dialog"
- `SearchScreen.kt:89` - "TODO: Show snackbar"

**Problem:**
- Errors happen silently
- Users don't know when save fails, delete fails, or search errors occur
- Poor UX, confusion

**Fix Required:**
```kotlin
// Add SnackbarHost to all screens
val snackbarHostState = remember { SnackbarHostState() }

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
) { ... }

// Show errors:
LaunchedEffect(errorMessage) {
    errorMessage?.let {
        snackbarHostState.showSnackbar(
            message = it,
            duration = SnackbarDuration.Short
        )
    }
}
```

**Priority:** 🔴 CRITICAL
**Effort:** 1-2 hours
**Risk if skipped:** Users lose data without knowing, 1-star reviews

---

### 🔴 3. Privacy Policy & Play Store Requirements

**Status:** ❌ MISSING

**Required:**
1. Privacy Policy URL (for RECORD_AUDIO permission)
2. Terms of Service
3. Data collection disclosure
4. GDPR compliance statement

**Why Critical:**
- Play Store **WILL REJECT** without privacy policy
- RECORD_AUDIO is sensitive permission
- Legal liability

**Action Items:**
1. Create `privacy_policy.html` hosted on GitHub Pages or website
2. Update `AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="@string/privacy_policy_url"/>
```
3. Add privacy link in Developer Screen
4. Add to Play Store listing

**Priority:** 🔴 CRITICAL
**Effort:** 3-4 hours
**Risk if skipped:** Play Store rejection, legal issues

---

### 🔴 4. App Signing Configuration

**File:** `app/build.gradle.kts`

**Current State:** ❌ No signing config

**Problem:**
- Can't publish to Play Store
- Each build has different signature
- Can't update app later

**Fix:**
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/axiom-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            ...
        }
    }
}
```

**Steps:**
1. Generate release keystore: `keytool -genkey -v -keystore axiom-release-key.jks ...`
2. Store keystore securely (NEVER commit to Git)
3. Document passwords in secure location
4. Add keystore to `.gitignore`

**Priority:** 🔴 CRITICAL
**Effort:** 1 hour
**Risk if skipped:** Cannot publish app

---

### 🔴 5. Crash Reporting - ZERO VISIBILITY

**Current State:** No crash tracking

**Problem:**
- Production crashes invisible
- Can't diagnose user issues
- Can't prioritize bug fixes

**Solutions:**

**Option A: Firebase Crashlytics (Recommended)**
```kotlin
// Add to build.gradle.kts
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}

plugins {
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}
```

**Option B: Sentry (Open Source)**
```kotlin
dependencies {
    implementation("io.sentry:sentry-android:7.0.0")
}

// In Application class:
Sentry.init { options ->
    options.dsn = "YOUR_DSN"
    options.tracesSampleRate = 1.0
}
```

**Priority:** 🔴 CRITICAL
**Effort:** 2-3 hours
**Risk if skipped:** Blind to production crashes, can't fix bugs

---

## ⚠️ HIGH PRIORITY (Required for Quality Release)

### 🟠 6. Settings Screen Implementation

**Status:** Route exists (`AxiomScreen.Settings`) but screen NOT implemented

**Required Features:**
- ✅ Theme selection (Light/Dark/System)
- ✅ Data management (Export notes, Clear cache, Delete all)
- ✅ About section (Version, Developer, Licenses)
- ✅ Privacy Policy link
- ✅ Backup & Restore
- ✅ Voice input settings (if keeping RECORD_AUDIO)

**Why High Priority:**
- Users expect settings in every app
- No way to export/backup data currently
- Theme toggle needed for accessibility

**Effort:** 4-6 hours
**File to create:** `SettingsScreen.kt`

---

### 🟠 7. Data Export/Backup Integration

**Status:** `ExportManager.kt` exists but NOT connected to UI

**Problem:**
- Users can't export their notes
- No backup before uninstall
- Data loss risk

**Required:**
- Export to JSON
- Export to Markdown files
- Export to PDF (optional)
- Import from backup

**Integration Points:**
- Settings screen "Export All Notes" button
- Note detail screen "Share as..." menu

**Effort:** 3-4 hours

---

### 🟠 8. Database Migration Strategy

**File:** `AxiomDatabase.kt:45`

**Current State:**
```kotlin
.fallbackToDestructiveMigration()  // ❌ DATA LOSS!
```

**Problem:**
- Any schema change = ALL DATA DELETED
- Users will lose notes on updates
- 1-star reviews guaranteed

**Fix:**
```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3)

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column
        database.execSQL("ALTER TABLE notes ADD COLUMN new_field TEXT")
    }
}
```

**Priority:** 🟠 HIGH
**Effort:** 2-3 hours per migration
**Risk if skipped:** User data loss on updates

---

### 🟠 9. Accessibility Compliance

**Issues Found:**
- Missing `contentDescription` on IconButtons
- No TalkBack testing
- Color contrast issues (some text on background)
- No haptic feedback on important actions

**Required Fixes:**
```kotlin
// Add descriptions:
Icon(
    imageVector = Icons.Default.Search,
    contentDescription = "Search notes"  // ✅
)

// Test with TalkBack enabled
// Ensure 4.5:1 contrast ratio minimum
```

**Effort:** 3-4 hours
**Impact:** ~15% of users need accessibility features

---

### 🟠 10. Play Store Assets Creation

**Missing:**
- App screenshots (phone & tablet)
- Feature graphic (1024x500)
- Promotional video (optional but recommended)
- Short description (80 chars)
- Full description (4000 chars)
- What's New / Changelog

**Required Screenshots:**
1. Splash screen with Axiom.md animation
2. Notes list with sample notes
3. Note editor with markdown toolbar
4. Markdown preview
5. Developer screen
6. Search functionality

**Effort:** 4-6 hours

---

## 🟡 MEDIUM PRIORITY (Polish & UX)

### 11. Onboarding Flow
- First-run tutorial
- Sample note with markdown examples
- Feature discovery

### 12. Enhanced Search
- Recent searches
- Search filters (date, tags)
- Search suggestions

### 13. Improved Markdown Rendering
- Syntax highlighting for code blocks
- Image rendering support
- Custom styles for headers

### 14. Widget Testing & Polish
- Test QuickNoteWidget
- Test NotesListWidget
- Add widget screenshots

### 15. Input Validation
- Max note size (prevent database bloat)
- Title length limits
- Content sanitization

---

## 🔧 TECHNICAL DEBT

### 16. Testing Coverage
**Current:** 0 tests written despite test dependencies

**Needed:**
- Unit tests for ViewModels
- Repository tests
- UI tests for critical flows

### 17. Localization
**Current:** English only

**Target:** Spanish, French, German, Hindi

### 18. Remove Unused Permission
**Issue:** INTERNET permission declared but unused

**Fix:** Remove from AndroidManifest or justify usage

### 19. Version Management
**Current:** v1.0, no changelog

**Needed:**
- Create CHANGELOG.md
- Semantic versioning strategy
- Release notes process

### 20. Code Documentation
**Current:** Minimal KDoc comments

**Needed:**
- Public API documentation
- Complex algorithm explanations
- Architecture decision records (ADRs)

---

## 📋 IMPLEMENTATION ROADMAP

### Phase 1: Critical Blockers (Week 1)
**Goal:** App can be safely released

- [ ] Day 1-2: Enable ProGuard + configure rules
- [ ] Day 2-3: Implement error handling (Snackbars)
- [ ] Day 3-4: Create Settings screen
- [ ] Day 4: Add crash reporting (Firebase/Sentry)
- [ ] Day 5: Privacy policy + app signing setup

**Deliverable:** Minimally viable production build

---

### Phase 2: High Priority (Week 2)
**Goal:** Quality user experience

- [ ] Day 6-7: Data export/import UI
- [ ] Day 8: Database migrations
- [ ] Day 9: Accessibility fixes
- [ ] Day 10: Play Store assets

**Deliverable:** Play Store ready app

---

### Phase 3: Polish (Week 3)
**Goal:** Competitive feature set

- [ ] Day 11-12: Onboarding flow
- [ ] Day 13: Enhanced search
- [ ] Day 14: Markdown improvements
- [ ] Day 15: Widget testing

**Deliverable:** High-quality release

---

### Phase 4: Testing & Launch (Week 4)
**Goal:** Go live

- [ ] Day 16-17: Internal testing
- [ ] Day 18: Beta release (closed track)
- [ ] Day 19: Bug fixes
- [ ] Day 20: Production release

**Deliverable:** Live on Play Store! 🎉

---

## 🎯 MINIMUM VIABLE RELEASE CHECKLIST

**Can release with ONLY these fixed:**

1. ✅ ProGuard enabled
2. ✅ Error handling (Snackbars)
3. ✅ Privacy policy created
4. ✅ App signing configured
5. ✅ Crash reporting enabled
6. ✅ Settings screen (basic)
7. ✅ Export functionality
8. ✅ Play Store assets

**Time Required:** 1-2 weeks focused work

---

## 🚨 BEFORE LEAVING PROJECT CHECKLIST

**If you must leave project for extended time, MINIMUM:**

1. **Enable ProGuard** - Security critical
2. **Fix error handling** - User experience critical
3. **Create Settings screen with Export** - Data safety
4. **Document build process** - Future you will thank you
5. **Commit & push all changes** - Don't lose work
6. **Tag current state** - `git tag v1.0-pre-production`
7. **Update this document** - Track progress

---

## 📞 SUPPORT & RESOURCES

**Build Issues:**
- Check `./gradlew assembleRelease --stacktrace`
- Review ProGuard logs: `app/build/outputs/mapping/release/`

**Database Migrations:**
- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)

**Play Store:**
- [Launch Checklist](https://developer.android.com/distribute/best-practices/launch/launch-checklist)
- [Privacy Policy Generator](https://www.privacypolicies.com/blog/privacy-policy-template/)

**Testing:**
- Firebase Test Lab for device testing
- Android Studio Profiler for performance

---

## ✅ PROGRESS TRACKER

**Last Updated:** [DATE]

### Completed ✅
- [x] Splash screen
- [x] Animated title
- [x] Developer screen
- [x] Custom app icon

### In Progress 🔄
- [ ] Error handling
- [ ] Settings screen

### Blocked ⛔
- [ ] Play Store submission (needs privacy policy)

### Deferred 📅
- [ ] Cloud sync
- [ ] Collaboration features

---

**Next Review Date:** [SET DATE]
**Owner:** Anshul (@OkayAnshul)
**Status:** Pre-Production
