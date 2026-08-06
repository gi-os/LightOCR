> [!IMPORTANT]
> # Deprecated — this is part of Roll now
>
> **Reading text out of a photograph has moved into
> [gi-os/LightCamera](https://github.com/gi-os/LightCamera)** (launcher label **Roll**), and this
> repository is no longer developed. Roll replaces both the stock Camera and Album on the LPIII, so
> the camera it needed was already open and the photographs it wanted to read were already in front
> of you.
>
> It arrived in two pieces:
>
> - **v2.40 — a TEXT button on any photograph on the roll.** Same bundled
>   `com.google.mlkit:text-recognition` this app used, for the same reason: the model ships inside
>   the APK, where the unbundled variant would ask Play Services for it and never finish.
> - **v2.41 — TEXT as a capture mode**, one turn of the wheel past QR. The frame comes off the
>   panel rather than the sensor, so there is no shutter lag at all; if that frame turns out too
>   coarse for the print, it takes one real exposure and reads that instead. Nothing lands on the
>   roll — a reading is not a photograph.
>
> **What Roll does that this did not:**
>
> - **The findings are shaped like QR payloads**, so a phone number photographed off a business
>   card gets the same sheet and the same verbs as one inside a QR code — call it, write to it,
>   open it. Everything that decides what a payload *is* was already written and tested for QR
>   mode, so text got it for free.
> - **v2.43 draws the boxes on the frame**, so you can see which line said what before you commit
>   to anything. Lines carrying something worth pressing are marked differently from lines that are
>   just words.
> - **Nothing is spell-corrected.** `O`/`0` and `l`/`1` are most of the difference between a
>   company's website and a domain bought to catch the typo, so the reading is shown as it was
>   read.
>
> **Not carried over:** the scan history with its thumbnails, and the SAF document picker for a
> file outside the media store. Roll lists every image on the device by default rather than only
> `DCIM`, so an ordinary photograph is still reachable — a file the media store does not know about
> is not.
>
> The APK on the releases page keeps working. It simply will not be updated again.

# LightOCR (Scanner)

A plain sideloaded Android app for the Light Phone III that reads text out of photos.
Take a picture or pick one from your files, and it turns any recognizable text on the
page into text you can copy and reuse -- entirely on-device.

Package: `com.gios.lightocr`. Launcher label: **Scanner**.

## Why a plain APK, not a Light SDK tool

The official `com.thelightphone.light-sdk` Gradle plugin enforces build-time allowlists
(`ALLOWED_DEPENDENCIES`, `BLOCKED_IMPORTS`, `ALLOWED_PERMISSIONS`) that exclude CameraX,
ML Kit, `READ_MEDIA_IMAGES`, `Context`, `Intent`, and `startActivity`. `CAMERA` is a
permitted permission, but there is no permitted way to use it from inside an SDK tool.
So, like `gi-os/LightPass` and `gi-os/LightCamera`, this is a plain single-module Android
project instead.

## What it does

1. **Capture, two ways.** An in-app CameraX preview and shutter, or the Storage Access
   Framework's document picker (`ActivityResultContracts.OpenDocument()`) for an existing
   photo. Deliberately not the Android Photo Picker and not `READ_MEDIA_IMAGES`: the Photo
   Picker's implementation on some OS builds depends on a Google Play system module, and
   the LPIII has no Google Play Services at all. SAF needs no runtime permission.
2. **On-device OCR.** `com.google.mlkit:text-recognition` -- the **bundled** variant. The
   recognition model ships inside the APK and never touches Google Play Services at
   runtime, unlike `com.google.android.gms:play-services-mlkit-text-recognition`, which
   downloads its model through Play Services and would simply never finish on this phone.
3. **Copy, and a history.** One tap copies the recognized text to the clipboard. Every
   scan is recorded to a local Room database: a downscaled JPEG thumbnail on disk (never a
   DB blob), the text, and a timestamp. The History tab lists past scans; tapping one
   reopens it.

## Design language

Ported from `lightphone/light-sdk` (MIT): the real 27x31 `LightGrid`, the named type scale
(scaled against a 477px baseline -- the SDK's own 600px baseline renders about 21% too
small on the LPIII's actual panel, a correction carried over from `gi-os/LightCamera`),
three colours (background/content/contentSecondary), `lightClickable` (no ripple, a 45ms
buzz on finger-down), and vector icons copied from the SDK's own set
(`res/drawable/ic_*_white.xml` -- see `LICENSE-light-sdk`).

## Signing

Every build is signed with one stable key (`keystore/lightocr.jks`, committed -- only the
passwords are secret, sourced from GitHub Actions secrets in CI). `signing-fingerprint.txt`
pins the certificate's SHA-256 digest; `build.yml` fails the build if a release APK's
certificate doesn't match it, or if any of the four signing secrets is missing.

## CI

- `build.yml` -- push to `main`: tests, signed release build, signing-fingerprint check,
  launcher-icon check, manifest sanity check, one APK published as a GitHub Release.
- `check.yml` -- every other branch: tests and an unsigned debug compile, no release. Used
  to verify a change compiles on GitHub's real (x86_64) runners before it ever reaches
  `main`, since `main` is what cuts a release.
