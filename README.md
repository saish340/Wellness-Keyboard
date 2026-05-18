# Wellness Keyboard

This repository contains tools and components for a personal wellness keyboard project that measures typing behavior and runs a baseline anomaly detector.

Contents (high-level):
- `keystroke_feature_prep.ipynb` — data preparation, visualization, and training helper functions.
- `export_wellness_tflite.py`, `test_wellness_tflite.py` — export/scoring helpers to create a TensorFlow SavedModel / TFLite fallback.
- `WellnessScreen.tsx` — React Native TypeScript screen for a 30-day wellness dashboard (standalone UI file).
- `app/` — Android native app module with TFLite inference wrapper, WorkManager night worker, Room DB entities, and notification helper.

How to prepare and push this repo to GitHub (local steps performed):

1. Initialize a local git repository (done locally):

   ```powershell
   git init
   git add .
   git commit -m "Initial commit"
   ```

2. Create a new GitHub repository (on github.com or use `gh`), then add the remote and push:

   ```powershell
   git remote add origin https://github.com/USERNAME/REPO.git
   git branch -M main
   git push -u origin main
   ```

Notes:
- The repository includes both Python and Android artifacts. If you intend to push large binaries (e.g., model files), consider using Git LFS.
- The `export_wellness_tflite.py` script may produce a `wellness_model.tflite` file — add it to `.gitignore` or commit explicitly.

If you want, I can create the initial commit locally now and (optionally) push if you provide a remote URL or let me open an authenticated browser flow.
