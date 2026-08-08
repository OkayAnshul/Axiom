# Privacy Policy — Axiom

**Last updated: 7 August 2026**

Axiom is a personal journalling app for Android, published by Anshul
(`com.cosmiclaboratory.axiom`). This policy describes exactly what the app does with your data.

The short version: **Axiom has no server.** There is no account to create, nothing is uploaded,
and nothing is collected about you. The only time any of your writing leaves your phone is when
you have supplied your own AI provider key and you have triggered a request that needs it.

---

## 1. What Axiom stores, and where

Everything Axiom keeps lives in a private database on your device, inside the app's own storage,
which other apps cannot read:

- Journal entries, titles, tags, timestamps and word counts
- Moods and named feelings, whether you chose them or the app inferred them
- Conversations with the companion
- The memory store — facts, people, goals, preferences and open loops
- Your display name and the companion's name, if you set them
- Your settings: theme, voice, check-in time, retention window, chosen AI provider

**We do not collect any of it.** There is no analytics SDK, no crash reporting service, no
advertising identifier, no tracking of any kind, and no third-party libraries that phone home.

## 2. When data leaves your device

AI features are **off until you turn them on**, and turning them on means pasting in an API key
for an account you already own at either Groq or Google (Gemini).

Once a key is present, and **only** for an action you take, Axiom sends the relevant text
directly from your phone to that provider over HTTPS. Nothing is routed through any server of
ours, because there isn't one. Requests are made for:

| Action | What is sent |
|---|---|
| Sending a message to the companion | Your message, recent conversation, the strongest memories, and a relevant excerpt of your journal |
| Turning a conversation into a journal entry | That conversation |
| Extracting memories | The conversation or entry being processed |
| Daily openers and check-in lines | A short summary of recent context |
| Voice transcription (fallback only — see §4) | The audio recording |
| Testing your key from Settings | A short fixed test string |

Your use of Groq or Gemini is governed by **your** agreement with that provider, under your own
account. Their handling of what you send is subject to their policies:

- Groq — <https://groq.com/privacy-policy/>
- Google Gemini API — <https://ai.google.dev/gemini-api/terms>

**If you never add a key, Axiom makes no network requests at all.** Journalling, search, the
calendar, mood tracking, patterns, streaks, widgets, backup and the crisis resources all work
entirely offline.

## 3. Your API keys

Keys are stored in `EncryptedSharedPreferences`, encrypted with a master key held in the Android
Keystore — on devices with secure hardware, that key never leaves the secure element. Keys are
deliberately excluded from Android's automatic cloud backup and from device-to-device transfer,
so they cannot be extracted from a backup. You can delete a key at any time in
**Settings → AI**, and it is removed immediately.

## 4. Microphone

Axiom requests `RECORD_AUDIO` so you can dictate entries instead of typing. It is used only
while you are actively recording a voice note, never in the background.

Transcription prefers Android's **on-device** `SpeechRecognizer`, which needs no network and no
key. Only if on-device recognition is unavailable for your language — and only if you have both
enabled the fallback and supplied a Groq key — is the recording sent to Groq's Whisper endpoint
for transcription. Recordings are not retained after transcription and are never uploaded
anywhere else.

## 5. Notifications

`POST_NOTIFICATIONS` is used for optional daily check-ins and companion messages. Notification
content is composed on your device. You can turn check-ins off in Settings, or deny the
permission entirely — the rest of the app is unaffected.

## 6. Backups

Axiom includes an encrypted whole-journal export you control: you choose when to create the file
and where it goes.

Android's *automatic* cloud backup is deliberately limited to your settings only. Your journal
database and your API keys are excluded from it — the database because a partially captured
database is worse than none, and the keys because they cannot be decrypted on a different
device. Use the in-app export to move your journal.

## 7. Crisis resources

If you write something that plainly states an intent to harm yourself, Axiom shows Indian mental
health helplines (Tele-MANAS, AASRA, iCall). This detection is plain text matching that runs
**entirely on your device** — no model, no network, and no key. Nothing about it is recorded,
transmitted, or reported to anyone.

Axiom is not a medical device and is not a substitute for professional care.

## 8. Children

Axiom is not directed at children and is intended for users aged 18 and over.

## 9. Your control

- **Delete an entry, a conversation or a memory** — removed from the database immediately.
- **Delete everything** — uninstalling the app removes the database with it.
- **Export everything** — Settings → Backup produces an encrypted file you own.
- **Turn AI off** — remove your key; the app keeps working without it.

## 10. Changes

Material changes to this policy will be reflected here, with an updated date at the top, before
they take effect in a released version.

## 11. Contact

Questions about this policy: **anshulisokay@gmail.com**
