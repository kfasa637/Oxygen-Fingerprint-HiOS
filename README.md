# Oxygen Fingerprint for HiOS

Android prototype investigating fingerprint UI compatibility on Transsion/TECNO devices.

## Current prototype
- Reports device model and Android version.
- Checks Android's fingerprint feature flag and BiometricManager status.
- Shows an OxygenOS-inspired animated ring preview.
- Does not modify system partitions, patch SystemUI, spoof biometric success, or interfere with the real unlock prompt.

## Target device
Initial target: TECNO Spark 20 Pro Plus (KJ7), Android 14 / HiOS.

## Compatibility
A normal third-party Android app cannot replace the vendor-protected SystemUI fingerprint animation. The lock-screen UI is controlled by firmware and the biometric implementation. Shizuku shell access is not root and does not grant permission to rewrite protected system components.

This is a diagnostic and visual foundation. A real system integration can only be considered after confirming a supported customization mechanism in the firmware. Device-level compatibility is not claimed until tested on the target phone.

## Build
Open in Android Studio and run the app, or use GitHub Actions to build a debug APK.

## Safety
The app is read-only with respect to system files. It does not alter biometric enrollment or authentication behavior.
