from __future__ import annotations

import argparse
from pathlib import Path

import joblib
import numpy as np
import tensorflow as tf


FEATURE_COLUMNS = [
    "avg_iki",
    "std_iki",
    "backspace_rate",
    "avg_hold",
    "wpm_estimate",
    "late_night_ratio",
    "session_count",
    "avg_session_duration",
]


def _score_with_sklearn(model, scaler, feature_batch: np.ndarray) -> np.ndarray:
    """Return higher-is-more-anomalous scores using the trained sklearn model.

    This function is used to verify the exported TensorFlow wrapper. The exact
    sklearn IsolationForest logic is not natively supported by TensorFlow Lite,
    so the script exports a SavedModel wrapper and then attempts conversion.
    """
    scaled = scaler.transform(feature_batch)
    decision_scores = model.decision_function(scaled)
    anomaly_scores = (-decision_scores).astype(np.float32)
    return anomaly_scores.reshape(-1, 1)


class WellnessAnomalyModule(tf.Module):
    def __init__(self, model, scaler):
        super().__init__()
        self._model = model
        self._scaler = scaler

    @tf.function(input_signature=[tf.TensorSpec(shape=[None, len(FEATURE_COLUMNS)], dtype=tf.float32, name="features")])
    def score(self, features: tf.Tensor) -> tf.Tensor:
        return tf.numpy_function(
            func=lambda batch: _score_with_sklearn(self._model, self._scaler, batch),
            inp=[features],
            Tout=tf.float32,
            name="sklearn_anomaly_score",
        )


def export_wellness_tflite(
    model_path: str = "wellness_model.pkl",
    scaler_path: str = "scaler.pkl",
    saved_model_dir: str = "wellness_savedmodel",
    tflite_path: str = "wellness_model.tflite",
) -> Path:
    model_file = Path(model_path)
    scaler_file = Path(scaler_path)

    if not model_file.exists():
        raise FileNotFoundError(f"Missing trained model: {model_file}")
    if not scaler_file.exists():
        raise FileNotFoundError(f"Missing fitted scaler: {scaler_file}")

    model = joblib.load(model_file)
    scaler = joblib.load(scaler_file)

    module = WellnessAnomalyModule(model=model, scaler=scaler)

    saved_model_path = Path(saved_model_dir)
    tf.saved_model.save(module, str(saved_model_path), signatures={"serving_default": module.score})

    converter = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_path))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS, tf.lite.OpsSet.SELECT_TF_OPS]

    try:
        tflite_model = converter.convert()
    except Exception as exc:
        raise RuntimeError(
            "TFLite conversion failed. A pickled scikit-learn IsolationForest cannot be lowered "
            "to fully portable TFLite ops through a numpy_function wrapper. To run on-device, "
            "retrain the detector in pure TensorFlow or rewrite the IsolationForest scoring logic "
            "with TensorFlow ops only."
        ) from exc

    tflite_file = Path(tflite_path)
    tflite_file.write_bytes(tflite_model)
    return tflite_file


def main() -> None:
    parser = argparse.ArgumentParser(description="Export the wellness anomaly model to TFLite.")
    parser.add_argument("--model", default="wellness_model.pkl", help="Path to the trained sklearn model")
    parser.add_argument("--scaler", default="scaler.pkl", help="Path to the fitted scaler")
    parser.add_argument("--saved-model-dir", default="wellness_savedmodel", help="Directory for the TensorFlow SavedModel")
    parser.add_argument("--output", default="wellness_model.tflite", help="Path to the TFLite output file")
    args = parser.parse_args()

    output_path = export_wellness_tflite(
        model_path=args.model,
        scaler_path=args.scaler,
        saved_model_dir=args.saved_model_dir,
        tflite_path=args.output,
    )
    print(f"Saved TFLite model to {output_path}")


if __name__ == "__main__":
    main()