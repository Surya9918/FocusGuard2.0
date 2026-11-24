"""
FocusGuard - Distraction Prediction Model Generator
====================================================
Senior ML Architect Note:
  This script generates a lightweight MLP (Multi-Layer Perceptron) trained on
  synthetic but behaviorally realistic mobile usage data.

  The model takes 3 normalized inputs:
    - app_id       : integer ID mapped to an app category (0–9)
    - hour_of_day  : 0–23, the hour the session started
    - time_spent   : duration in minutes (capped at 120 for normalization)

  Output:
    - A single float in [0.0, 1.0] representing distraction risk.

  The exported distraction_model.tflite file should be placed in:
    app/src/main/assets/distraction_model.tflite

Prerequisites (install once):
  pip install tensorflow numpy scikit-learn pandas matplotlib
"""

import base64
import hashlib
import os
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt
import json

# ─────────────────────────────────────────────────────────────────────────────
# SECTION 1: APP CATEGORY DEFINITIONS
# ─────────────────────────────────────────────────────────────────────────────
# Maps app_id (int) → (category_name, base_distraction_probability)
# These IDs mirror the ones used in the Android app's AppIdMapper.java
APP_CATEGORIES = {
    0: ("social_media",        0.85),   # Instagram, TikTok, Twitter
    1: ("messaging",           0.55),   # WhatsApp, Telegram
    2: ("video_streaming",     0.75),   # YouTube, Netflix, Reels
    3: ("gaming",              0.80),   # Mobile games
    4: ("news_feeds",          0.65),   # News aggregators, Reddit
    5: ("productivity",        0.10),   # Notion, Google Docs, Sheets
    6: ("education",           0.08),   # Duolingo, Coursera
    7: ("navigation",          0.05),   # Google Maps, Waze
    8: ("health_fitness",      0.12),   # Fitbit, MyFitnessPal
    9: ("utilities_system",    0.04),   # Settings, Files, Calculator
}

# ─────────────────────────────────────────────────────────────────────────────
# SECTION 2: SYNTHETIC DATA GENERATION
# ─────────────────────────────────────────────────────────────────────────────
# Behavioral rules encoded here reflect real digital wellbeing research:
#   1. Late-night usage (22:00–02:00) dramatically increases distraction risk
#   2. Long sessions (>30 mins) on entertainment apps are near-certain distractions
#   3. Short utility app checks (<5 mins) are almost never distractions
#   4. Messaging is ambiguous — could be productive or distraction
#   5. Morning productivity windows (06:00–10:00) reduce distraction score

def compute_distraction_probability(app_id: int, hour: int, duration: float) -> float:
    """
    Compute the ground-truth distraction probability using behavioral heuristics.
    This is the 'oracle' used to label synthetic training data.

    Args:
        app_id    : integer 0–9, maps to APP_CATEGORIES
        hour      : integer 0–23
        duration  : float, minutes spent in this session

    Returns:
        float in [0.0, 1.0], the probability this session is a distraction
    """
    base_prob = APP_CATEGORIES[app_id][1]

    # --- Temporal modifiers ---
    # Late night penalty: social/entertainment usage after 10 PM
    late_night = hour >= 22 or hour <= 2
    morning_focus = 6 <= hour <= 10

    if late_night:
        time_modifier = 1.35   # 35% boost to distraction risk
    elif morning_focus:
        time_modifier = 0.70   # 30% reduction during morning focus window
    else:
        time_modifier = 1.00

    # --- Duration modifiers ---
    # Very short sessions are unlikely distractions (quick checks)
    # Very long sessions heavily indicate distraction (bingeing)
    if duration <= 3:
        duration_modifier = 0.50   # Quick check, likely intentional
    elif duration <= 10:
        duration_modifier = 0.85   # Short session
    elif duration <= 30:
        duration_modifier = 1.10   # Medium — slight distraction lean
    elif duration <= 60:
        duration_modifier = 1.40   # Long session, strong distraction signal
    else:
        duration_modifier = 1.70   # Extended binge — very high risk

    # --- Final probability (clamped to [0, 1]) ---
    final_prob = base_prob * time_modifier * duration_modifier
    return float(np.clip(final_prob, 0.0, 1.0))


def generate_dataset(n_samples: int = 20000, random_seed: int = 42) -> pd.DataFrame:
    """
    Generate a synthetic dataset of mobile usage sessions with realistic
    distributions matching real-world app usage patterns.

    Args:
        n_samples   : Total number of sessions to generate
        random_seed : For reproducibility

    Returns:
        pd.DataFrame with columns: app_id, hour_of_day, time_spent_mins,
                                   distraction_prob, is_distraction
    """
    rng = np.random.default_rng(random_seed)

    # Realistic app usage distribution (users spend more time on social/video)
    app_weights = [0.20, 0.15, 0.18, 0.12, 0.08, 0.10, 0.06, 0.04, 0.04, 0.03]
    app_ids = rng.choice(
        list(APP_CATEGORIES.keys()),
        size=n_samples,
        p=app_weights
    )

    # Hour distribution: peak usage at lunch (12–13) and evening (20–23)
    # Modeled with a bimodal distribution skewed toward evening
    hours_base = rng.normal(loc=20, scale=3, size=n_samples)     # Evening peak
    hours_lunch = rng.normal(loc=13, scale=1.5, size=n_samples)  # Lunch peak
    mix_mask = rng.random(n_samples) > 0.65  # 35% lunch, 65% evening
    hours_raw = np.where(mix_mask, hours_base, hours_lunch)
    hours = np.clip(np.round(hours_raw), 0, 23).astype(int)

    # Duration distribution: log-normal (most sessions are short, some are long)
    # Mean ≈ 12 minutes, with a long tail up to 120 minutes
    durations_raw = rng.lognormal(mean=2.3, sigma=0.8, size=n_samples)
    durations = np.clip(durations_raw, 1.0, 120.0)

    # Compute labels using behavioral oracle
    distraction_probs = np.array([
        compute_distraction_probability(app_ids[i], hours[i], durations[i])
        for i in range(n_samples)
    ])

    # Add noise to make the classification boundary realistic (not perfectly clean)
    noise = rng.normal(0, 0.08, n_samples)
    noisy_probs = np.clip(distraction_probs + noise, 0.0, 1.0)

    # Threshold at 0.5 for binary label
    is_distraction = (noisy_probs >= 0.5).astype(int)

    df = pd.DataFrame({
        "app_id":          app_ids,
        "hour_of_day":     hours,
        "time_spent_mins": durations,
        "distraction_prob": noisy_probs,
        "is_distraction":  is_distraction,
    })

    return df


# ─────────────────────────────────────────────────────────────────────────────
# SECTION 3: FEATURE NORMALIZATION
# ─────────────────────────────────────────────────────────────────────────────
def build_features(df: pd.DataFrame):
    """
    Normalize features to [0, 1] range for neural network training.
    Returns scaled X array and the scaler (to save normalization params).
    """
    X_raw = df[["app_id", "hour_of_day", "time_spent_mins"]].values.astype(np.float32)
    y = df["is_distraction"].values.astype(np.float32)

    scaler = MinMaxScaler(feature_range=(0.0, 1.0))
    X_scaled = scaler.fit_transform(X_raw)

    return X_scaled, y, scaler


# ─────────────────────────────────────────────────────────────────────────────
# SECTION 4: MODEL ARCHITECTURE
# ─────────────────────────────────────────────────────────────────────────────
def build_model() -> tf.keras.Model:
    """
    Builds a lightweight MLP optimized for on-device inference.

    Architecture rationale:
    - Small footprint: ~6KB when quantized — negligible on any Android device
    - 3 inputs → 32 → 16 → 8 → 1 output
    - BatchNormalization for training stability with limited data
    - Dropout for regularization (overfitting prevention)
    - Sigmoid output: returns probability in [0.0, 1.0]

    Design philosophy:
      On-device models must be tiny. A model that makes a user's phone
      warm or drains 1% extra battery defeats the purpose of a wellbeing app.
    """
    inputs = tf.keras.Input(shape=(3,), name="usage_features")

    x = tf.keras.layers.Dense(32, activation="relu", name="hidden_1")(inputs)
    x = tf.keras.layers.BatchNormalization(name="bn_1")(x)
    x = tf.keras.layers.Dropout(0.25, name="dropout_1")(x)

    x = tf.keras.layers.Dense(16, activation="relu", name="hidden_2")(x)
    x = tf.keras.layers.BatchNormalization(name="bn_2")(x)
    x = tf.keras.layers.Dropout(0.15, name="dropout_2")(x)

    x = tf.keras.layers.Dense(8, activation="relu", name="hidden_3")(x)

    output = tf.keras.layers.Dense(1, activation="sigmoid", name="distraction_risk")(x)

    model = tf.keras.Model(inputs=inputs, outputs=output, name="FocusGuardMLP")

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss="binary_crossentropy",
        metrics=[
            "accuracy",
            tf.keras.metrics.AUC(name="auc"),
            tf.keras.metrics.Precision(name="precision"),
            tf.keras.metrics.Recall(name="recall"),
        ]
    )

    return model


# ─────────────────────────────────────────────────────────────────────────────
# SECTION 5: TRAINING
# ─────────────────────────────────────────────────────────────────────────────
def train_model(model: tf.keras.Model, X_train, y_train, X_val, y_val):
    """
    Trains the model with early stopping and learning rate reduction.
    """
    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor="val_auc",
            patience=10,
            restore_best_weights=True,
            mode="max",
            verbose=1
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=5,
            min_lr=1e-6,
            verbose=1
        ),
    ]

    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=100,
        batch_size=256,
        callbacks=callbacks,
        verbose=1
    )

    return history


# ─────────────────────────────────────────────────────────────────────────────
# SECTION 6: TFLITE EXPORT WITH QUANTIZATION
# ─────────────────────────────────────────────────────────────────────────────
def export_tflite(model: tf.keras.Model, output_path: str = "distraction_model.tflite"):
    """
    Converts and exports the Keras model to TFLite format with dynamic range
    quantization. This reduces model size by ~75% with negligible accuracy loss.

    Dynamic range quantization converts float32 weights to int8 at export time,
    keeping activations as float32. This is the best balance of size vs. accuracy
    for on-device wellbeing apps.
    """
    converter = tf.lite.TFLiteConverter.from_keras_model(model)

    # Apply dynamic range quantization
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    tflite_model = converter.convert()

    with open(output_path, "wb") as f:
        f.write(tflite_model)

    size_kb = os.path.getsize(output_path) / 1024
    print(f"\n✅ TFLite model exported: '{output_path}'")
    print(f"   File size: {size_kb:.1f} KB")
    print(f"\n📋 Next step:")
    print(f"   Copy '{output_path}' to your Android project at:")
    print(f"   app/src/main/assets/distraction_model.tflite")

    return tflite_model


# ─────────────────────────────────────────────────────────────────────────────
# SECTION 7: NORMALIZATION PARAMS EXPORT
# ─────────────────────────────────────────────────────────────────────────────
def export_scaler_params(
    scaler: MinMaxScaler, model_bytes: bytes, output_path: str = "scaler_params.json"
):
    """
    Exports scaler min/max parameters to JSON so the Android app can
    normalize inputs identically to how training data was normalized.

    The Android app pairs this metadata with the exact model bytes.
    """
    params = {
        "feature_names": ["app_id", "hour_of_day", "time_spent_mins"],
        "model_sha256_base64": base64.b64encode(hashlib.sha256(model_bytes).digest()).decode("ascii"),
        "data_min":  scaler.data_min_.tolist(),
        "data_max":  scaler.data_max_.tolist(),
        "scale":     scaler.scale_.tolist(),
        "min":       scaler.min_.tolist(),
    }

    with open(output_path, "w") as f:
        json.dump(params, f, indent=2)

    print(f"\n📊 Scaler params exported to '{output_path}'")
    print(f"   Use these values in MLPredictor.java for input normalization:")
    for i, name in enumerate(params["feature_names"]):
        print(f"   {name}: min={params['data_min'][i]:.4f}, max={params['data_max'][i]:.4f}")


# ─────────────────────────────────────────────────────────────────────────────
# SECTION 8: EVALUATION & VISUALIZATION
# ─────────────────────────────────────────────────────────────────────────────
def evaluate_and_plot(model, history, X_test, y_test, scaler):
    """
    Evaluates model performance and generates training curve plots.
    """
    print("\n" + "="*60)
    print("  FOCUSGUARD MODEL — EVALUATION REPORT")
    print("="*60)

    # Metric evaluation
    results = model.evaluate(X_test, y_test, verbose=0)
    metric_names = ["Loss", "Accuracy", "AUC", "Precision", "Recall"]
    for name, val in zip(metric_names, results):
        print(f"  {name:<12}: {val:.4f}")

    # Classification report
    y_pred_prob = model.predict(X_test, verbose=0).flatten()
    y_pred = (y_pred_prob >= 0.5).astype(int)
    print("\n  Classification Report:")
    print(classification_report(y_test, y_pred,
                                target_names=["Focused", "Distracted"]))

    # Confusion matrix
    cm = confusion_matrix(y_test, y_pred)
    print(f"  Confusion Matrix:\n  {cm}")

    # ---- Training curves ----
    fig, axes = plt.subplots(1, 3, figsize=(15, 4))
    fig.suptitle("FocusGuard — Model Training Curves", fontsize=14, fontweight="bold")

    axes[0].plot(history.history["loss"],     label="Train Loss", color="#6750A4")
    axes[0].plot(history.history["val_loss"], label="Val Loss",   color="#E8A925")
    axes[0].set_title("Loss")
    axes[0].set_xlabel("Epoch")
    axes[0].legend()
    axes[0].grid(alpha=0.3)

    axes[1].plot(history.history["accuracy"],     label="Train Acc", color="#6750A4")
    axes[1].plot(history.history["val_accuracy"], label="Val Acc",   color="#E8A925")
    axes[1].set_title("Accuracy")
    axes[1].set_xlabel("Epoch")
    axes[1].legend()
    axes[1].grid(alpha=0.3)

    axes[2].plot(history.history["auc"],     label="Train AUC", color="#6750A4")
    axes[2].plot(history.history["val_auc"], label="Val AUC",   color="#E8A925")
    axes[2].set_title("AUC (Model Discrimination)")
    axes[2].set_xlabel("Epoch")
    axes[2].legend()
    axes[2].grid(alpha=0.3)

    plt.tight_layout()
    plt.savefig("training_curves.png", dpi=150, bbox_inches="tight")
    print("\n  📈 Training curves saved to 'training_curves.png'")
    plt.show()

    # ---- Distraction risk vs. hour heatmap ----
    print("\n  Generating distraction risk heatmap...")
    fig2, axes2 = plt.subplots(1, len(APP_CATEGORIES), figsize=(20, 4))
    fig2.suptitle("Predicted Distraction Risk by App & Hour", fontsize=12)

    hours = np.arange(0, 24)
    durations = [5, 15, 30]  # Short, medium, long sessions

    for app_id, (cat_name, _) in APP_CATEGORIES.items():
        risks = []
        for h in hours:
            session_risks = []
            for d in durations:
                raw = np.array([[app_id, h, d]], dtype=np.float32)
                norm = scaler.transform(raw)
                risk = model.predict(norm, verbose=0)[0][0]
                session_risks.append(risk)
            risks.append(np.mean(session_risks))

        axes2[app_id].bar(hours, risks,
                          color="#6750A4",
                          alpha=np.array(risks))
        axes2[app_id].set_title(cat_name.replace("_", "\n"), fontsize=7)
        axes2[app_id].set_ylim(0, 1)
        axes2[app_id].set_xticks([0, 6, 12, 18, 23])
        axes2[app_id].axhline(0.5, color="red", linestyle="--", alpha=0.4)

    plt.tight_layout()
    plt.savefig("distraction_heatmap.png", dpi=150, bbox_inches="tight")
    print("  📊 Heatmap saved to 'distraction_heatmap.png'")
    plt.show()


# ─────────────────────────────────────────────────────────────────────────────
# MAIN PIPELINE
# ─────────────────────────────────────────────────────────────────────────────
def main():
    print("="*60)
    print("  FocusGuard ML Pipeline — Starting")
    print("  TensorFlow version:", tf.__version__)
    print("="*60)

    # Step 1: Generate dataset
    print("\n[1/5] Generating synthetic usage dataset (20,000 sessions)...")
    df = generate_dataset(n_samples=20000, random_seed=42)
    print(f"      Total samples   : {len(df)}")
    print(f"      Distractions    : {df['is_distraction'].sum()} "
          f"({df['is_distraction'].mean()*100:.1f}%)")
    print(f"      Focused sessions: {(~df['is_distraction'].astype(bool)).sum()} "
          f"({(1-df['is_distraction'].mean())*100:.1f}%)")

    # Step 2: Build features
    print("\n[2/5] Normalizing features...")
    X, y, scaler = build_features(df)

    # Step 3: Split
    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y, test_size=0.30, random_state=42, stratify=y
    )
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=0.50, random_state=42, stratify=y_temp
    )
    print(f"      Train: {len(X_train):,} | Val: {len(X_val):,} | Test: {len(X_test):,}")

    # Step 4: Build and train
    print("\n[3/5] Building model architecture...")
    model = build_model()
    model.summary()

    print("\n[4/5] Training model...")
    history = train_model(model, X_train, y_train, X_val, y_val)

    # Step 5: Export
    print("\n[5/5] Exporting artifacts...")
    model_bytes = export_tflite(model, "distraction_model.tflite")
    export_scaler_params(scaler, model_bytes, "scaler_params.json")

    # Evaluation
    evaluate_and_plot(model, history, X_test, y_test, scaler)

    print("\n" + "="*60)
    print("  ✅ FocusGuard ML Pipeline Complete!")
    print("="*60)
    print("\nFiles generated:")
    print("  1. distraction_model.tflite  → Copy to app/src/main/assets/")
    print("  2. scaler_params.json        → Copy alongside the model to app/src/main/assets/")
    print("  3. training_curves.png       → Training visualization")
    print("  4. distraction_heatmap.png   → Risk prediction heatmap")


if __name__ == "__main__":
    main()
