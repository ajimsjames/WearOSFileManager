# 📁 Wear OS File Manager (v1.2.0)

**Complete Standalone File Manager & Storage Explorer for Wear OS (Samsung Galaxy Watch 6)**

Developed by **Aju George**.

---

## ✨ Features

- 📁 **Internal Storage File Explorer**: Browse folders, files, hidden files, and storage directories (`/sdcard`, `/storage/emulated/0`) directly on smartwatch hardware.
- 👁️ **Native Built-in Viewers**:
  - 📝 **Text Viewer**: Built-in viewer for `.txt`, `.log`, `.json`, `.csv`, `.xml`, `.html`, `.md`, `.py`, `.sh`, `.gradle`.
  - 🖼️ **Image Canvas Viewer**: Built-in viewer with pan & zoom for `.png`, `.jpg`, `.jpeg`, `.bmp`, `.webp`, `.gif`.
  - 🎵 **Audio Player**: Native audio player for `.mp3`, `.wav`, `.m4a`, `.ogg`, `.aac`, `.flac`.
  - 📄 **PDF Integration**: Opens PDF documents directly via `Wear OS PDF Reader` FileProvider.
- 📊 **Storage Analytics Breakdown**: Quick storage info dialog displaying used/free memory capacity and partition stats.
- ⭕ **Bezel-Aligned Navigation & About Dialog**: Curved top navigation bar (`CurvedLayout`) featuring `📁 Files`, `📊 Storage`, and `⚙️ About` bezel buttons.

---

## 🛠️ Architecture & Tech Stack

- **Framework**: Android Wear OS (Min SDK 30 / Target SDK 33)
- **UI Engine**: Wear Compose + Jetpack Compose + CurvedLayout
- **File Access**: FileProvider + Android Storage APIs + StatFs.

---

## 📦 Installation

```bash
# Connect to Galaxy Watch 6 via Wireless ADB
adb connect <WATCH_IP>:<PORT>

# Build and Install Release APK
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📄 License & Credits

Created and maintained by **Aju George**. Distributed for Wear OS devices.
