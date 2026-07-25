# 📁 Watch File Manager for Wear OS

**Developer & Author**: Aju George  
**Target Device**: Samsung Galaxy Watch 6 (Wear OS 4 / Android 13+)

A standalone, feature-packed file explorer and file manager engineered specifically for Wear OS 4 smartwatches. Built with a 60 FPS dark theme UI tailored for circular AMOLED displays, it allows full navigation of internal watch storage, inline file viewing, and quick file management directly on your wrist.

---

### ✨ Key Features

- **📊 Real-Time Storage Usage Bar**: Displays real-time watch storage statistics (Used / Total capacity).
- **📁 Complete Directory Navigation**: Browse all internal directories (`/sdcard`, `Download`, `Documents`, `DCIM`, `Pictures`, `Music`, `Android/data`, etc.).
- **👁️ Intelligent Inline File Viewers**:
  - **PDF Documents (`.pdf`)**: Launches **Wear OS PDF Reader** directly to read target documents.
  - **Text / Log / Code Files (`.txt`, `.log`, `.json`, `.csv`, `.xml`, `.md`, `.py`, `.sh`)**: Monospace text viewer with scroll support.
  - **Images (`.png`, `.jpg`, `.jpeg`, `.bmp`, `.webp`)**: Skia hardware-accelerated image viewer with pinch-to-zoom and double-tap scaling.
- **🗑️ Wrist File Operations**: Inspect file size, last modified date, and delete files directly to free up watch storage.
- **🚫 Anti-Gesture Swipe Protection**: Custom Wear OS theme with `android:windowSwipeToDismiss = false` prevents folder navigation swipes from accidentally closing the app.

---

### 👨‍💻 Author & Maintainer

Created with ❤️ by **Aju George**.

---

### 🛠️ Built With

- **Target OS**: Wear OS 4 / Android 13+ (API 33+)
- **Target Hardware**: Samsung Galaxy Watch 6 44mm (`SM-R940`), 480×480 px circular display
- **Language**: Kotlin 1.9 & Java 21
- **UI Framework**: Android Jetpack Compose for Wear OS & AndroidX FileProvider
- **Optimization**: R8 Bytecode Shrinking
