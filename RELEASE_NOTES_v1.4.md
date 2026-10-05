Release **v1.4** of **Custos** - Onboarding reordering, battery settings fix, whitelist quarantine protections, and performance improvements.

### What's New in v1.4

* **Onboarding & Permissions Order**:
  * Accessibility service is now strictly placed as the **final step** with a prominent warning banner. Activating Accessibility first previously blocked users from accessing Settings to grant other permissions.
  * Clear, tailored guides for Android 13+ "Restricted Settings" (Google Pixel, Samsung One UI, Xiaomi HyperOS/MIUI, and generic Android).

* **Battery Optimization Fix**:
  * Added missing `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission in `AndroidManifest.xml`.
  * Added graceful fallbacks: direct prompt → system battery optimization list → App Info details page.

* **Clean Factory Defaults (No Hardcoded Restrictions)**:
  * Instagram has **no native blocking** out of the box. Instagram is only redirected to inbox if a dedicated Screen Rule is actively created.
  * Whitelist is optional and disabled by default. The Delays Overview dialog no longer displays a phantom 24h quarantine delay when Whitelist is disabled.

* **Whitelist Quarantine & Deactivation Protection**:
  * Disabling the Whitelist now strictly respects the quarantine delay to prevent impulse deactivations.
  * Improved app isolation and hidden state management.

* **App Picker Optimization**:
  * App selection in App Limits no longer freezes or stutters on large app libraries.
  * Fixed first added app not displaying until a second app was added.

---

### Install / Update

Download `custos-v1.4.apk` below, copy it to your Android phone (8.0+) and open it.
> **Note**: This APK is signed with the official release key. You can install it straight over previous versions without uninstalling, preserving your data and settings!
