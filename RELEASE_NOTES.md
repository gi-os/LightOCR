## Scanner (LightOCR) v1.0.0

First release.

- Take a photo with the camera, or pick an existing image from your files (Storage
  Access Framework document picker -- no Google Play Services needed, no storage
  permission dialog).
- On-device text recognition with ML Kit's bundled Latin-script model. No network
  round-trip, no cloud service, works with the phone offline.
- Copy the recognized text to the clipboard in one tap.
- Every scan is kept in History: a thumbnail, the recognized text, and when it was taken.
  Tap a past scan to reopen it and copy the text again, or delete it.
- LightOS look and feel throughout: Akkurat where the system has it, the real 27x31 grid
  and named type scale, three greys, no ripples, a 45ms buzz on finger-down.
