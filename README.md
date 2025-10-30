# 🗺️ AR Avatar Guide

**An AR-based campus navigation system with voice-guided virtual avatar assistance**

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![ARCore](https://img.shields.io/badge/ARCore-4285F4?style=for-the-badge&logo=google&logoColor=white)

---

## 📖 Overview

AR Avatar Guide is an innovative Android application that combines **Augmented Reality (AR)** and **Voice Recognition** to provide an intuitive indoor/outdoor navigation experience. Perfect for large campuses, malls, museums, or any complex indoor spaces.

### ✨ Key Features

- 🎯 **Dual Mode System**: Host mode for creating paths, Visitor mode for navigation
- 🗣️ **Voice Commands**: Natural language destination selection
- 👤 **3D Avatar Guide**: Virtual assistant that appears in AR space
- 🧭 **Visual Navigation**: Color-coded waypoints and directional arrows
- 📍 **Auto Path Recording**: Captures waypoints every 30cm while walking
- 🔊 **Text-to-Speech**: Audio guidance and confirmations
- 📱 **Real-time AR Tracking**: Powered by Google ARCore

---

## 🎬 How It Works

### 🏗️ Host Mode (Path Creation)

1. **Start Recording**: Enter a starting point name (e.g., "Main Gate")
2. **Walk the Path**: Move slowly towards your destination
3. **Auto-Capture**: App automatically records waypoints every 30cm
4. **Save Path**: Enter destination name and stop recording

The system saves the complete path with all waypoints for future navigation.

### 👥 Visitor Mode (Navigation)

1. **Voice Activation**: Tap the microphone button
2. **Speak Destination**: Say where you want to go (e.g., "Library")
3. **Follow the Guide**: 
   - 3D avatar appears 2 meters ahead
   - Green circles mark the path
   - Golden arrow points to next waypoint
   - Distance updates in real-time
4. **Arrive**: Voice confirmation when you reach your destination

---

## 🛠️ Technical Stack

### Core Technologies
- **Language**: Kotlin
- **AR Framework**: Google ARCore
- **Graphics**: OpenGL ES 3.0
- **Voice Recognition**: Android SpeechRecognizer
- **Text-to-Speech**: Android TTS Engine

### Key Components

| Component | Purpose |
|-----------|---------|
| `ARActivity` | Host mode - Path recording with AR visualization |
| `VisitorActivity` | Visitor mode - AR navigation with voice control |
| `PathRecorder` | Captures and manages waypoint recording |
| `PathManager` | File-based persistence for saved paths |
| `NavigationHelper` | Distance calculation and arrow positioning |
| `ModelLoader` | 3D model rendering (arrows and avatar) |
| `BackgroundRenderer` | AR camera feed rendering |
| `SimpleRenderer` | Waypoint circle visualization |

---

## 📋 Requirements

### Minimum Requirements
- Android 7.0 (API 24) or higher
- ARCore supported device ([Check compatibility](https://developers.google.com/ar/devices))
- Camera with AR capabilities
- Microphone for voice commands

### Permissions
- `CAMERA` - AR tracking and visualization
- `RECORD_AUDIO` - Voice recognition

---

## 🚀 Getting Started

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Akshay-jk2004/ARAvatarGuide.git
   cd ARAvatarGuide
   ```

2. **Open in Android Studio**
   - Android Studio Arctic Fox (2020.3.1) or newer recommended
   - Gradle will automatically sync dependencies

3. **Install ARCore**
   - ARCore will be automatically installed on first app launch
   - Or install manually from [Google Play Store](https://play.google.com/store/apps/details?id=com.google.ar.core)

4. **Build and Run**
   ```bash
   ./gradlew installDebug
   ```

### First-Time Setup

1. **Create Your First Path** (Host Mode):
   - Grant camera permission when prompted
   - Select "Host Mode" from main menu
   - Enter starting point name
   - Walk slowly (normal walking pace) to destination
   - Enter destination name and save

2. **Navigate** (Visitor Mode):
   - Grant camera and microphone permissions
   - Select "Visitor Mode" from main menu
   - Tap microphone and say your destination
   - Follow the AR guide!

---

## 🎨 Visual Indicators

| Color | Meaning |
|-------|---------|
| 🔵 Blue Circle | Starting point of path |
| 🟢 Green Circle | Path waypoint |
| 🔴 Red Circle | Destination point |
| 🟡 Golden Arrow | Direction indicator |

---

## 📁 Project Structure

```
ARAvatarGuide/
├── app/src/main/java/com/example/aravatarguide/
│   ├── MainActivity.kt              # App entry point
│   ├── ARActivity.kt                # Host mode - path recording
│   ├── VisitorActivity.kt           # Visitor mode - navigation
│   ├── Waypoint.kt                  # Waypoint data model
│   ├── PathRecorder.kt              # Recording logic
│   ├── PathManager.kt               # File storage management
│   ├── NavigationHelper.kt          # Navigation calculations
│   ├── PathFinder.kt                # Pathfinding algorithms
│   ├── ModelLoader.kt               # 3D model rendering
│   ├── AvatarRenderer.kt            # Avatar visualization
│   ├── BackgroundRenderer.kt        # AR camera background
│   └── SimpleRenderer.kt            # Waypoint circles
├── app/src/main/res/layout/
│   ├── activity_main.xml            # Main menu UI
│   ├── activity_aractivity.xml      # Host mode UI
│   └── activity_visitor.xml         # Visitor mode UI
└── app/src/main/AndroidManifest.xml # App configuration
```

---

## 🔧 Configuration

### Waypoint Recording Settings

Edit `PathRecorder.kt` to adjust recording behavior:

```kotlin
companion object {
    private const val MIN_DISTANCE_BETWEEN_POINTS = 0.3f // 30cm spacing
}
```

### Navigation Threshold

Edit `VisitorActivity.kt` to change when waypoints are marked as "reached":

```kotlin
companion object {
    private const val WAYPOINT_REACHED_DISTANCE = 0.8f // 80cm threshold
}
```

---

## 🎯 Use Cases

- 🏫 **University Campuses**: Guide new students to classrooms
- 🏢 **Corporate Offices**: Navigate large office complexes
- 🏥 **Hospitals**: Help visitors find departments
- 🏛️ **Museums**: Interactive exhibit tours
- 🏪 **Shopping Malls**: Store navigation
- 🏨 **Hotels**: Guest wayfinding

---

## 🐛 Known Limitations

- Single path storage (overwrites previous paths)
- No multi-path selection interface
- Basic pathfinding (direct waypoint sequence)
- Requires good lighting for AR tracking
- Indoor GPS may be unreliable

---

## 🔮 Future Enhancements

- [ ] Multiple path storage and selection
- [ ] Cloud-based path sharing
- [ ] Animated 3D avatar with gestures
- [ ] Offline map integration
- [ ] Multi-language support
- [ ] Path optimization algorithms
- [ ] Obstacle detection and rerouting
- [ ] Indoor positioning system (IPS) integration
- [ ] Analytics dashboard for path usage

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Contribution Ideas
- Add support for multiple saved paths
- Implement path editing functionality
- Create a web dashboard for path management
- Add AR anchors for persistent paths
- Improve avatar animations

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Akshay JK**
- GitHub: [@Akshay-jk2004](https://github.com/Akshay-jk2004)
- Project Link: [https://github.com/Akshay-jk2004/ARAvatarGuide](https://github.com/Akshay-jk2004/ARAvatarGuide)

---

## 🙏 Acknowledgments

- [Google ARCore](https://developers.google.com/ar) - AR foundation
- [Android Speech Recognition](https://developer.android.com/reference/android/speech/SpeechRecognizer) - Voice input
- OpenGL ES - 3D rendering
- Kotlin Community - Language support

---

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/Akshay-jk2004/ARAvatarGuide/issues) page
2. Create a new issue with detailed description
3. Join discussions in the [Discussions](https://github.com/Akshay-jk2004/ARAvatarGuide/discussions) tab

---

## 📸 Screenshots

> **Note**: Add screenshots of your app in action here
> - Main menu
> - Host mode recording
> - Visitor mode navigation
> - AR waypoints visualization

---

## ⭐ Show Your Support

Give a ⭐️ if this project helped you!

---

<div align="center">

**Made with ❤️ and AR magic**

[Report Bug](https://github.com/Akshay-jk2004/ARAvatarGuide/issues) · [Request Feature](https://github.com/Akshay-jk2004/ARAvatarGuide/issues)

</div>
