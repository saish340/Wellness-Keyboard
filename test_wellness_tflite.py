from __future__ import annotations

import argparse
from pathlib import Path

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


def make_sample_features() -> np.ndarray:
    return np.array(
        [[
            180.0,
            35.0,
            0.08,
            140.0,
            42.0,
            0.12,
            4.0,
            820.0,
        ]],
        dtype=np.float32,
    )


def run_tflite_inference(tflite_path: Path, sample_features: np.ndarray) -> float:
    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()[0]
    output_details = interpreter.get_output_details()[0]

    input_array = sample_features.astype(input_details["dtype"])
    interpreter.set_tensor(input_details["index"], input_array)
    interpreter.invoke()

    output_array = interpreter.get_tensor(output_details["index"])
    return float(np.asarray(output_array).reshape(-1)[0])


def run_saved_model_inference(saved_model_dir: Path, sample_features: np.ndarray) -> float:
    loaded_model = tf.saved_model.load(str(saved_model_dir))
    signature = loaded_model.signatures["serving_default"]
    input_name = next(iter(signature.structured_input_signature[1].keys()))
    output_key = next(iter(signature.structured_outputs.keys()))
    output = signature(**{input_name: tf.constant(sample_features)})[output_key]
    return float(np.asarray(output).reshape(-1)[0])


def main() -> None:
    parser = argparse.ArgumentParser(description="Run a sample inference on the exported TFLite wellness model.")
    parser.add_argument("--model", default="wellness_model.tflite", help="Path to the TFLite model")
    parser.add_argument("--saved-model-dir", default="wellness_savedmodel", help="Fallback TensorFlow SavedModel directory")
    args = parser.parse_args()

    model_path = Path(args.model)

    sample_features = make_sample_features()
    if model_path.exists():
        anomaly_score = run_tflite_inference(model_path, sample_features)
        print(f"Sample anomaly score from TFLite: {anomaly_score:.6f}")
        return

    saved_model_path = Path(args.saved_model_dir)
    if not saved_model_path.exists():
        raise FileNotFoundError(
            f"Neither TFLite model ({model_path}) nor SavedModel fallback ({saved_model_path}) was found."
        )

    anomaly_score = run_saved_model_inference(saved_model_path, sample_features)
    print(f"Sample anomaly score from SavedModel fallback: {anomaly_score:.6f}")


if __name__ == "__main__":
    main()