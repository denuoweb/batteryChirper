
# BatteryMonitorApp

## Directory Structure
```
BatteryMonitorApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/batterymonitor/
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml
│   │   │   │   └── raw/
│   │   │   │       └── chirp_sound.mp3  <-- Your chirp sound file
│   │   │   └── AndroidManifest.xml
│   └── build.gradle
└── build.gradle
```

## Prerequisites

- **Android Studio:** Latest stable version is recommended.
- **Android SDK:** API level 33 (or higher) for compiling and testing.
- **Device/Emulator:** A non-rooted Android device or an emulator running Android API level 21 or higher.
- **Basic Knowledge:** Familiarity with Android development and Kotlin programming.

## Installation and Setup

1. **Clone or Download the Repository:**
   If the project is hosted in a version control repository (e.g., GitHub), clone the repository:
   ```bash
   git clone https://github.com/yourusername/BatteryMonitorApp.git
   cd BatteryMonitorApp
   ```

   Alternatively, download the project ZIP and extract it.

2. **Open in Android Studio:**
   Launch Android Studio and choose "Open an existing Android Studio project". Navigate to the project directory and open it.

3. **Sync Gradle:**
   Once the project is loaded, ensure that Gradle syncs successfully by clicking on "Sync Now" if prompted.

## Compile and Build the Application

1. **Check Configuration:**
   Open the `build.gradle` (Module: app) file to make sure the `compileSdkVersion` and `targetSdkVersion` are set to 33 or whatever is the latest that you are using.

2. **Resolve Dependencies:**
   Ensure all dependencies are correctly configured in your `build.gradle` files and sync the project again if you make any changes.

3. **Build the Project:**
   You can build the project by selecting "Build > Make Project" from the top menu, or by pressing Ctrl+F9 (Cmd+F9 on Mac). This will compile the Kotlin code, XML layouts, and all resources into an Android APK.

## Deploy and Run the Application

1. **Prepare Your Device:**
   - Ensure that your Android device has USB debugging enabled (found in Developer Options).
   - Connect your device to your computer via USB. You might need to confirm the connection on your device to allow USB debugging.
   - Make sure your device is detected by running `adb devices` from your command line or terminal. This should list the connected devices.

2. **Install the APK:**
   - In Android Studio, select "Run > Run 'app'" or click the Run icon in the toolbar.
   - Android Studio will prompt you to select a deployment target. Choose your connected device or a suitable emulator.
   - The application will be built and installed on your selected device.

3. **Run the Application:**
   - Once installed, the app should start automatically on your device.
   - You can also manually start it by finding the app icon in your device’s app drawer.

## Monitoring Battery Status

- Upon app launch, the main activity will display the current battery percentage and discharge rate. If the battery level drops rapidly, it will trigger an audible chirp sound to alert you, which you can test by altering the battery settings in your emulator or by using the app over time on a physical device.

## Code Explanation

### MainActivity.kt

- **BroadcastReceiver Registration:**
  The app registers a dynamic BroadcastReceiver to listen for the ACTION_BATTERY_CHANGED intent. This provides periodic updates on battery status.

- **Discharge Rate Calculation:**
  The app stores the previous battery percentage and timestamp. With each update, it calculates the difference in battery level over time (in minutes) to determine the discharge rate.

- **Audio Alert Mechanism:**
  If the calculated discharge rate exceeds the threshold (and if a cooldown period has passed), the app uses the MediaPlayer API to play a chirp sound stored in the res/raw directory.

- **User Feedback:**
  The battery percentage and discharge rate are updated on the user interface via a simple TextView.

### AndroidManifest.xml

- **Component Declaration:**
  Declares the main activity and includes the necessary intent filters to start the app.
- **Permissions:**
  No special permissions are required for this battery monitoring functionality.

## Build Configuration

- **Gradle Build Files:**
  The build.gradle files define the compile SDK version, target SDK version, and other configurations. The project uses minimal dependencies to ensure broad compatibility.

## Customization

- **Adjusting the Discharge Threshold:**
  Modify the dischargeThreshold variable in MainActivity.kt to change what is considered a rapid battery drop.

- **Configuring the Cooldown Period:**
  The cooldown period (to prevent repeated alerts) is defined by chirpCooldown. Adjust this value as needed.

- **Custom Chirp Sound:**
  Replace the chirp_sound.mp3 file in the res/raw folder with your own audio file if desired. Ensure the filename and resource reference in MainActivity.kt are updated accordingly.

## Troubleshooting

- **No Audio Alert:**
  Verify that the chirp sound file exists in the correct directory and that the MediaPlayer is properly initialized.

- **Inaccurate Battery Monitoring:**
  Some devices or emulators may not simulate battery status changes accurately. Testing on a physical device is recommended.

- **Gradle Sync Issues:**
  Ensure that your Android Studio and SDK installations are up to date to avoid compatibility issues.

## Future Enhancements

- **Background Monitoring:**
  Consider implementing a foreground service for continuous battery monitoring even when the app is not active.

- **User Settings:**
  Provide an interface for users to adjust the discharge threshold and cooldown period.

- **Logging and Analytics:**
  Integrate logging for battery performance and user notifications to better understand battery behavior over time.

## License

This project is released under the MIT License. See the LICENSE file for additional details.

## Contact

For any queries or contributions, please contact the project maintainer or open an issue on the repository.