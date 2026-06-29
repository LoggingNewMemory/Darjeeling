# ☕ Darjeeling

Darjeeling is an elegant, modern, and lightweight Android Quick Settings tile app that keeps your device's screen awake on demand. Inspired by LineageOS's Caffeine tile, but entirely rebuilt from the ground up with a beautiful Material 3 interface, smart dynamic icons, and flexible system permission support.

## ✨ Features

- **Quick Settings Tile**: Keep your screen on without diving into system settings.
- **Smart Dynamic Icons**: The tile icon dynamically changes to boldly display the remaining minutes (5, 10, 15, 30), reverting to a stylish cup icon when off or unlimited.
- **Live Tile Countdown**: Watch the remaining time tick down directly on your Quick Settings panel (updates every second!).
- **In-App Controller**: Perfect for OEM ROMs (like Nubia MyOS) that hide Quick Settings text. Open the app to view live status and cycle timers.
- **Dual Operating Modes**:
  - **Non-Root Method**: Standard API method requiring `WRITE_SETTINGS` permission.
  - **Root Method**: For power users. Bypasses standard restrictions using `su` to directly alter screen timeout settings in the background. Prompts for Magisk/SU access securely and permanently remembers it.
- **Auto-Turn Off**: If your screen turns off naturally or if you press the power button, Darjeeling automatically resets to save battery.

## 🚀 How to Use

1. Install the app on your device.
2. Open your Quick Settings panel, tap the edit (pencil) icon, and drag the **Darjeeling** tile into your active tiles.
3. Tap the tile to cycle through the available screen-on modes: 
   `5m -> 10m -> 15m -> 30m -> Unlimited -> Off`
4. Alternatively, open the Darjeeling app from your launcher to use the **Control Darjeeling** interface.

## 🛠️ Build Instructions

This project is built using Android Studio and Jetpack Compose. 

To build the APK from source:
```bash
./gradlew assembleDebug
```
You can then install it on a connected device via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 👨‍💻 Credits

Developed by **Kanagawa Yamada**
