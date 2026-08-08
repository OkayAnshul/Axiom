# Play Store submission pack

Everything needed for the v1.0 listing, plus the answers to every declaration Play will ask for.
Copy the text blocks straight into Play Console.

---

## 0. Before anything else — check your account type

Personal developer accounts created after **13 November 2023** must run a closed test with
**12+ testers opted in for 14 continuous days** before production access unlocks. Organisation
accounts are exempt.

Check: **Play Console → Setup → (banner or "Production access" status)**.

- If the rule applies → start closed testing *today*; everything else can proceed in parallel.
  Realistic go-live: ~3 weeks.
- If it doesn't → internal testing → production in a few days.

---

## 1. Signing key

Run this yourself so you own the password. **Do not lose the file or the passwords** — without
Play App Signing enrolment, losing them means never updating this listing again.

```bash
cd /path/to/Axiom
keytool -genkeypair -v -keystore release.jks -alias axiom \
  -keyalg RSA -keysize 4096 -validity 10000
```

Then `cp keystore.properties.example keystore.properties` and fill in:

```properties
storeFile=release.jks
storePassword=<yours>
keyAlias=axiom
keyPassword=<yours>
```

Both `keystore.properties` and `*.jks` are already gitignored. **Enrol in Play App Signing** when
prompted during the first upload — it means Google holds the signing key and you can recover from
losing yours.

## 2. Build

```bash
./gradlew clean bundleRelease
# -> app/build/outputs/bundle/release/app-release.aab
```

Verified working: builds clean, R8 runs, `lintVitalRelease` passes, ~11 MB.

**Verify the release build before uploading.** R8 breakage is release-only and invisible to
`assembleDebug`:

```bash
# Install the AAB as a universal APK
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=/tmp/axiom.apks --mode=universal \
  --ks=release.jks --ks-key-alias=axiom
bundletool install-apks --apks=/tmp/axiom.apks
```

Then exercise the AI path end to end — **Settings → AI → test the key**, then send a real
companion message. That is where kotlinx-serialization and Ktor actually run.

---

## 3. Store listing

### App name (30 char max)
```
Axiom: Journal & Companion
```
*(25 characters)*

### Short description (80 char max)
```
A private journal you can talk to. It remembers, and it stays on your phone.
```
*(75 characters)*

### Full description (4000 char max)

```
Most journals fail the same way: a blank page is intimidating, and there's no reason to open it again tomorrow.

Axiom starts a conversation instead.

Tell it how your day went. When you're done, it writes the conversation up as a proper journal entry — in your words, not a summary — and files it for you. Over time it learns what matters to you, and it shows you exactly what it has learned.

TALK, DON'T COMPOSE
The home screen is a conversation, not an empty text field. Say what happened. Axiom turns it into something you can read back in a year.

IT REMEMBERS — AND SHOWS ITS WORKING
People, facts, goals and preferences are kept in a list you can read line by line, each one showing where it came from and how often it's come up. Edit any of it. Delete any of it. Nothing about you is hidden from you.

IT FOLLOWS UP. ONCE.
Mention an interview on Tuesday and it asks how it went on Wednesday. Then it lets the subject drop — because a companion that nags is one you stop talking to.

PATTERNS, NOT DIAGNOSES
Which weekday consistently runs hardest. Whether this week reads lighter than last. Which people your better days cluster around. All worked out on your device — and it stays quiet when there genuinely isn't enough to say yet.

WRITE HOWEVER YOU WANT
Markdown with a live preview and a focus mode. Voice notes with on-device speech recognition. Hindi and Hinglish are first-class, including in search. Guided prompts for days when nothing comes.

FIND ANYTHING
Full-text search across everything you've written, plus meaning-based search for the question people actually ask a journal: when did I feel like this before?

ALWAYS TO HAND
Home-screen widgets, a Quick Settings tile for the thought you'll otherwise lose, a month calendar, and an encrypted export of your whole journal that you control.

YOUR JOURNAL STAYS ON YOUR PHONE
No account. No sign-up. No upload. No analytics, no ads, no trackers of any kind. There is no server, so there is nowhere for your writing to go.

Journalling, mood tracking, patterns, search, streaks, widgets and backup all work with the network switched off entirely.

AI FEATURES ARE OPTIONAL, AND RUN ON YOUR KEY
If you want the companion to reply, you add your own API key for your own Groq or Google Gemini account. Only then — and only for something you asked for — does text leave your device, and it goes straight to that provider. Your key is encrypted with a key held in the Android Keystore.

Never add a key, and Axiom makes no network requests at all.

—

Axiom is not a medical device and is not a substitute for professional care. If you write something that plainly says you're in danger, it will show you Indian mental health helplines — detected entirely on your device, recorded nowhere, reported to no one.
```

### Category and tags
- **Category:** Health & Fitness *(or Lifestyle — Health & Fitness suits the mood/patterns angle)*
- **Tags:** journal, diary, mental health, mood tracker, privacy

### Contact details
- **Email:** anshulisokay@gmail.com
- **Website:** `https://okayanshul.github.io/axiom-site/`
- **Privacy policy:** `https://okayanshul.github.io/axiom-site/privacy.html` ← **must resolve
  publicly before submitting; Play validates it**

---

## 4. Graphic assets

| Asset | Requirement | File |
|---|---|---|
| App icon | 512×512, 32-bit PNG | `tools/store/play-icon-512.png` ✅ |
| Feature graphic | 1024×500, PNG/JPG | `tools/store/play-feature-1024x500.png` ✅ |
| Phone screenshots | 2–8, 16:9 or 9:16 | `screenshots/light/` — 1080×2400 ✅ |

**Recommended upload order** (the first two are what most people ever see):

1. `01-companion.png` — the conversation, referencing an earlier entry
2. `03c-patterns-charts.png` — streak, mood sparkline, emotions
3. `02-journal.png` — the timeline
4. `07-memory.png` — "What I remember", with provenance
5. `09-search.png` — search with highlighting
6. `04-settings-ai.png` — "Your journal stays on this device"
7. `10-calendar.png` — month view
8. `dark/01-companion.png` — dark mode

Tablet screenshots are optional; omitting them gets the listing flagged "not optimised for larger
screens", which is cosmetic. Regenerate any of these with `tools/screenshots.sh`.

---

## 5. Data safety form

Must match [PRIVACY.md](../PRIVACY.md) exactly. Answers:

**Does your app collect or share any of the required user data types?** → **Yes**
*(Because text is transmitted to a third-party AI provider when the user configures a key. If you
answer "No", Play may consider it a misrepresentation.)*

| Question | Answer |
|---|---|
| Is all data encrypted in transit? | **Yes** — HTTPS only |
| Do you provide a way to delete data? | **Yes** — in-app deletion, and uninstall removes everything |

**Data type: Personal info → Name** (optional display name)
- Collected: **No** — stored on device only, never transmitted to us
- Shared: **No**

**Data type: Personal info → Other (journal content / "Other user-generated content")**
- Collected: **No** *(we operate no server and receive nothing)*
- **Shared: Yes** — with a third-party AI provider **chosen by the user**, under **the user's own
  account**, only when they have configured an API key
- Purpose: **App functionality**
- Is sharing optional? → **Yes, users can choose whether this data is shared**

**Data type: Audio → Voice or sound recordings**
- Collected: **No**
- **Shared: Yes**, conditionally — only when on-device recognition is unavailable *and* the user
  has enabled the Whisper fallback *and* has a Groq key
- Purpose: **App functionality**
- Optional: **Yes**

**Not collected, not shared, explicitly:** location, contacts, financial info, health/fitness
records, photos, files, calendar, app activity/analytics, crash logs, advertising ID, device IDs.

> The framing that matters: Axiom is not a data controller. There is no server. "Shared" is
> checked because the user's text reaches Groq/Google under the user's own account — declaring it
> is correct and honest, and Play rejects apps that under-declare.

---

## 6. Content rating (IARC questionnaire)

Answer honestly. The ones that matter for this app:

| Question | Answer |
|---|---|
| Violence, sexual content, profanity, gambling, drugs | **No** to all |
| Does the app reference or deal with **suicide or self-harm**? | **Yes** |
| → If yes: does it provide crisis resources? | **Yes** — Tele-MANAS, AASRA, iCall, shown on-device |
| Does the app allow users to interact or share content? | **No** — no social features, no UGC sharing between users |
| Does the app share the user's location? | **No** |
| Does the app allow purchases? | **No** |

Expected rating: **Teen / 12+**, though you are declaring 18+ as target audience anyway.

> Declaring the self-harm reference is the right call. Google *requires* apps in this space to
> surface crisis resources — having `DistressSignal` + `CareCard` is a point in your favour, not a
> risk. Hiding it would be the risk.

## 7. Target audience and content

- **Target age group:** **18 and over** only. Simplest and most honest for an app inviting
  emotional disclosure; also avoids the Families policy programme entirely.
- **Appeals to children?** → **No**
- **Ads:** **No**
- **In-app purchases:** **No**

## 8. App access

**All functionality is available without special access.** No login, no account, no credentials
for reviewers. Add a note:

```
No account or login is required. All features are available immediately on install.

AI-powered replies require the user to supply their own Groq or Google Gemini API key
(Settings > AI). The app is fully functional without one — journalling, search, mood
tracking, patterns, widgets and backup all work offline with no key.
```

## 9. Other declarations

| Declaration | Answer |
|---|---|
| Government app | No |
| Financial features | None |
| Health apps declaration | Not a health app in Play's regulated sense — no medical device claims, no health records |
| News app | No |
| COVID-19 contact tracing | No |
| Data deletion request URL | Not required — no account exists. Point to in-app deletion. |

### Permissions
`RECORD_AUDIO`, `INTERNET`, `POST_NOTIFICATIONS`. **None require a sensitive-permission
declaration form.** Notably absent and worth knowing you don't have to justify: no
`SCHEDULE_EXACT_ALARM`, no `QUERY_ALL_PACKAGES`, no background location, no `MANAGE_EXTERNAL_STORAGE`.

---

## 10. Release checklist

- [ ] Account type checked (§0) — closed testing started if the 12-tester rule applies
- [ ] `release.jks` generated, backed up somewhere durable, `keystore.properties` filled
- [ ] Play App Signing enrolled at first upload
- [ ] `./gradlew clean test` green
- [ ] `./gradlew clean bundleRelease` succeeds
- [ ] Release build installed via bundletool and the **AI path exercised** (R8 verification)
- [ ] Backup/restore verified: save a key → `adb shell bmgr backupnow com.cosmiclaboratory.axiom`
      → uninstall → reinstall → restore → app opens and prompts to reconnect, no crash
- [ ] Site published, `privacy.html` resolving publicly
- [ ] Icon, feature graphic, 8 screenshots uploaded
- [ ] Listing copy pasted (§3)
- [ ] Data safety form completed (§5)
- [ ] Content rating questionnaire completed (§6)
- [ ] Target audience set to 18+ (§7)
- [ ] App access note added (§8)
- [ ] Internal testing track → install from Play → smoke test
- [ ] Closed testing (if required) → 12+ testers, 14 continuous days
- [ ] Production rollout
