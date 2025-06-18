# Manga Reader App

A modern Android manga reading application built with Jetpack Compose and Kotlin.

## Features

- 📖 Clean and intuitive reading interface
- 🎨 Multiple reading themes (Light, Dark, Sepia)
- ⚙️ Customizable reading settings
- 📱 Modern Material Design 3 UI
- 🔧 Adjustable text size and line spacing
- 💾 Reading preferences persistence with DataStore

## Screenshots

*Screenshots will be added here*

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Data Storage**: DataStore Preferences
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Dependencies

- Jetpack Compose UI
- Material Design 3
- Navigation Compose
- Lifecycle ViewModel Compose
- DataStore Preferences
- Material Icons Extended

## Getting Started

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or later
- JDK 8 or higher
- Android SDK with API level 34
- Gradle 8.0+

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/manga-reader-app.git
   cd manga-reader-app
   ```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Run the app on an emulator or physical device

### Building

#### Debug Build
```bash
./gradlew assembleDebug
```

#### Release Build
```bash
./gradlew assembleRelease
```

## Project Structure

```
app/src/main/java/com/example/manga_apk/
├── MainActivity.kt                 # Main activity
├── data/
│   └── ReadingPreferences.kt      # Data models and preferences
├── ui/
│   ├── ReadingScreen.kt           # Main reading screen
│   ├── ReadingSettingsPanel.kt    # Settings panel UI
│   └── theme/                     # App theming
└── viewmodel/
    └── ReadingViewModel.kt        # ViewModel for reading screen
```

## Features Overview

### Reading Interface
- Smooth scrolling text display
- Tap to show/hide UI controls
- Settings panel with real-time preview

### Customization Options
- **Themes**: Light, Dark, Sepia
- **Text Size**: Adjustable from 12sp to 24sp
- **Line Spacing**: Configurable line height multiplier
- **Font Family**: Support for different font families

### Data Persistence
- User preferences are automatically saved
- Settings persist across app restarts
- Uses Android DataStore for efficient storage

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Guidelines

1. Follow Kotlin coding conventions
2. Use Jetpack Compose best practices
3. Maintain MVVM architecture pattern
4. Add appropriate comments for complex logic
5. Test your changes thoroughly

### Submitting Changes

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Uses [Material Design 3](https://m3.material.io/)
- Icons from [Material Icons](https://fonts.google.com/icons)

## Support

If you encounter any issues or have questions, please [open an issue](https://github.com/yourusername/manga-reader-app/issues) on GitHub.

## Roadmap

- [ ] Add manga library management
- [ ] Implement chapter navigation
- [ ] Add bookmark functionality
- [ ] Support for different manga formats
- [ ] Online manga source integration
- [ ] Reading progress tracking
- [ ] Dark mode improvements
- [ ] Accessibility enhancements

---

**Note**: This is a sample manga reader app for educational purposes. Make sure to comply with copyright laws when using with actual manga content.