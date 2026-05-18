# Wellness Keyboard

Wellness Keyboard is an Android input-method project that records keystroke timing metadata, stores session history locally, and runs a lightweight anomaly score over daily typing features.

## What It Includes

- `app/` - Android app module with the keyboard service, local database, nightly worker, notifications, and the inference wrapper.
- `keystroke_feature_prep.ipynb` - notebook for feature engineering, exploration, and model prep.
- `export_wellness_tflite.py` - exports the trained detector to a SavedModel and attempts TFLite conversion.
- `test_wellness_tflite.py` - runs a sample inference against the exported model or SavedModel fallback.
- `WellnessScreen.tsx` - standalone React Native screen prototype for a wellness dashboard.

## App Overview

The Android app has two main surfaces:

- A keyboard service that captures metadata such as key press timing, hold duration, inter-key interval, backspace usage, and session boundaries.
- A simple activity that shows the most recent session statistics for debugging and inspection.

The app stores data locally with Room and can run a scheduled nightly inference job.

## Model File

The Android app expects a model asset at:

`app/src/main/assets/wellness_model.tflite`

If you generate a new model, copy it into that assets directory before running the app.

## Requirements

- Android Studio
- Android SDK Platform 34
- JDK 17
- Python 3.10+ if you want to run the feature prep or export scripts

## Build And Run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration on an emulator or device.
4. If you want to inspect the database and UI, open the launcher activity shown in the app module.

From the command line:

```powershell
./gradlew assembleDebug
```

## Python Workflow

Use the Python utilities when you want to prepare features or test the exported model.

```powershell
python export_wellness_tflite.py
python test_wellness_tflite.py
```

The export script expects a trained `wellness_model.pkl` and `scaler.pkl` in the project root unless you pass custom paths.

## Notes

- The project mixes Android and Python tooling, so keep large generated artifacts out of version control unless you intentionally want them tracked.
- If you regenerate the model, verify the APK again in Android Studio so the analyzer uses the latest build output.
