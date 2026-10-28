# திருக்குறள் முற்றோதல் — Recitation of Entire Tirukkural (v2.0.0)

This is a native Android rewrite of the Play Store app `com.cict.recitation_of_entire_Tirukkural`. The earlier version was built with App Inventor. This version is built with Kotlin, Jetpack Compose and Media3, and targets **API level 37 (Android 17)**. The minimum is API 24 (Android 7.0).

## What's new for listeners

- **Modern player**: full-screen "Now Playing" view with the Thiruvalluvar artwork. The artwork pulses gently while a recitation plays, and the background glows in the colour of the current பால்.
- **Waveform seek bar**: tap or drag to seek. There are also ±10 s skip buttons, previous/next, shuffle, and repeat (all or one).
- **Continuous recitation (முற்றோதல்)**: all 133 அதிகாரங்கள் play back to back in one queue.
- **Background playback**: control playback from the notification, lock screen, Bluetooth or a headset. Playback pauses automatically when headphones are unplugged.
- **Resume**: the app remembers the last chapter and position, along with speed, repeat and shuffle.
- **Playback speed**: 0.5× to 2×.
- **Sleep timer**: 15 to 90 minutes, or "at the end of this chapter".
- **Offline listening**: chapters you've heard are cached. You can also download single chapters, or all 133 (about 536 MB) from the ⋮ menu, and remove them again.
- **Search**: find a chapter by Tamil name, transliteration, chapter number or **kural number** (for example `391` finds கல்வி). You can also filter by அறத்துப்பால் / பொருட்பால் / காமத்துப்பால்.
- **Display**: light and dark themes and an edge-to-edge layout. On tablets and other wide screens (840 dp or more), the chapter list and the player sit side by side.
- **Home-screen shortcut kept**: the old launcher entry (`…Screen1`) is preserved, so existing shortcuts keep working after the update.

Audio is streamed from the Internet Archive item <https://archive.org/details/thiru-kkural>, with one MP3 per அதிகாரம்.

## Build

Requirements: JDK 17+ (JDK 23 is used here) and the Android SDK with platform 37 (`sdk.dir` in `local.properties`).

```powershell
.\build-release.ps1
```

The script writes these files to `release\`:

- `Thirukkural-Mutrodhal-v2.0.0-100.aab`: upload this to Google Play.
- `Thirukkural-Mutrodhal-v2.0.0-100.apk`: use this for direct install and testing.

## Signing: read before uploading to Play

Google Play only accepts an update if the bundle is signed with the app's **upload key**. For this app that is the original App Inventor key:

- File: `keystore/appinventor-upload.keystore` (alias `androidkey`)
- SHA1: `77:24:EC:29:61:FC:FF:AD:54:17:CA:C7:C7:6C:F5:65:50:F9:C5:D2`

`keystore.properties` points the release build at this key, so `.\build-release.ps1` produces files Play accepts. **Back up the `keystore` folder and `keystore.properties` somewhere private.** Without this key you can't publish further updates.

`keystore/cict-upload.jks` (SHA1 `3A:B5:00:79:…`) is a spare key and is not used. Switch to it only if you ever request an **upload key reset** in Play Console (**Test and release → App integrity → App signing**) using `keystore/upload_certificate.pem`. Its settings are kept, commented out, in `keystore.properties`.

## Before you publish

- **versionCode** is `100` in `app/build.gradle.kts`. It must be higher than the version code currently on Play, which you can check under **App bundle explorer**. If the live one is already 100 or more, raise this number and rebuild.
- **Foreground service declaration**: the app uses `FOREGROUND_SERVICE_MEDIA_PLAYBACK` for background audio. Fill in **App content → Foreground service permissions** with "Media playback: plays Thirukkural recitation audio while the app is in the background or the screen is off."
- **Data safety**: no change. The app collects or shares no user data, and it has no analytics or ads.
- **APK testing**: an APK signed with the upload key can't be installed over the Play-installed app. Uninstall the old app first, or test through an internal testing track.

## Store listing assets (`store-listing/`)

All images are 24-bit PNGs that meet Google Play's size and aspect-ratio rules.

| Folder / file | What it's for | Size |
|---|---|---|
| `phone/` | Phone screenshots (8; Play allows 2–8) | 1080×1920 |
| `phone-captioned/` | The same features as marketing images with Tamil and English captions (8). Upload these *or* `phone/` | 1080×1920 |
| `phone/extra/` | Spare shots: chapter list sheet, offline menu | 1080×1920 |
| `tablet-7inch/` | 7-inch tablet screenshots | 1080×1920 |
| `tablet-10inch/` | 10-inch tablet screenshots (two-pane layout) | 2560×1440 |
| `feature-graphic.png` | Feature graphic (required) | 1024×500 |
| `icon-512.png` | High-res app icon (your current icon) | 512×512 |
| `release-notes.txt` | "What's new" text for Play (ta-IN, en-IN, en-US, each under 500 characters) plus a full changelog | — |
| `privacy-policy.html` | Privacy policy web page to host (self-contained, works on phones, light and dark) | — |

### Privacy policy

- **Where to edit it:** the source is `app/src/main/assets/privacy-policy.html`. The app shows this exact file from **About → தனியுரிமைக் கொள்கை**, and it works offline. Every `.\build-release.ps1` run copies it to `store-listing/privacy-policy.html`, so the two copies always match.
- **For Google Play:** upload `store-listing/privacy-policy.html` to a public address on cict.in, for example `https://cict.in/apps/thirukkural-mutrodhal/privacy-policy.html`. Then paste that address into Play Console under **App content → Privacy policy**. Play only accepts a page anyone can open without signing in.

## Project layout

```
app/src/main/java/com/cict/tirukkural/
  data/Tirukkural.kt          133 chapters, iyals, audio file names and durations
  playback/PlaybackService.kt  Media3 session service (notification, sleep timer, resume)
  playback/AudioCache.kt       shared on-disk cache for streamed audio
  playback/OfflineManager.kt   per-chapter and download-all
  ui/                          Compose screens: home, now playing, mini player
```
