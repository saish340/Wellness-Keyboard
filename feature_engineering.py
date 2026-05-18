from __future__ import annotations

import pandas as pd


def build_daily_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Build one row per calendar day from a keystroke-event DataFrame.

    Expected input columns:
    - sessionId
    - timestamp
    - holdDuration
    - interKeyInterval
    - isBackspace

    The function returns a DataFrame indexed by date with:
    - avg_iki
    - std_iki
    - backspace_rate
    - avg_hold
    - wpm_estimate
    - late_night_ratio
    - session_count
    - avg_session_duration
    """

    if df.empty:
        return pd.DataFrame(
            columns=[
                "avg_iki",
                "std_iki",
                "backspace_rate",
                "avg_hold",
                "wpm_estimate",
                "late_night_ratio",
                "session_count",
                "avg_session_duration",
            ],
            index=pd.Index([], name="date"),
        )

    frame = df.copy()
    frame["timestamp"] = pd.to_datetime(frame["timestamp"], errors="coerce")
    frame = frame.dropna(subset=["timestamp", "sessionId"])

    frame["date"] = frame["timestamp"].dt.normalize().dt.date
    frame["hour"] = frame["timestamp"].dt.hour
    frame["interKeyInterval"] = pd.to_numeric(frame["interKeyInterval"], errors="coerce")
    frame["holdDuration"] = pd.to_numeric(frame["holdDuration"], errors="coerce")
    frame["isBackspace"] = frame["isBackspace"].astype("boolean")

    session_bounds = (
        frame.groupby(["date", "sessionId"], as_index=False)
        .agg(
            session_start=("timestamp", "min"),
            session_end=("timestamp", "max"),
        )
    )
    session_bounds["session_duration_seconds"] = (
        (session_bounds["session_end"] - session_bounds["session_start"]).dt.total_seconds().clip(lower=0)
    )

    daily = frame.groupby("date").agg(
        avg_iki=("interKeyInterval", "mean"),
        std_iki=("interKeyInterval", "std"),
        backspace_rate=("isBackspace", lambda series: series.fillna(False).mean()),
        avg_hold=("holdDuration", "mean"),
        total_keystrokes=("id", "count") if "id" in frame.columns else ("timestamp", "count"),
        late_night_keystrokes=("hour", lambda hours: hours.isin([23, 0, 1, 2, 3]).sum()),
    )

    daily_sessions = session_bounds.groupby("date").agg(
        session_count=("sessionId", "nunique"),
        avg_session_duration=("session_duration_seconds", "mean"),
    )

    result = daily.join(daily_sessions, how="left")
    result["late_night_ratio"] = result["late_night_keystrokes"] / result["total_keystrokes"]
    result["wpm_estimate"] = (result["total_keystrokes"] / 5.0) / ((frame.groupby("date")["timestamp"].agg(lambda s: (s.max() - s.min()).total_seconds() / 60.0)).replace(0, pd.NA))
    result["wpm_estimate"] = result["wpm_estimate"].fillna(0)

    result = result.drop(columns=["total_keystrokes", "late_night_keystrokes"])
    result = result[[
        "avg_iki",
        "std_iki",
        "backspace_rate",
        "avg_hold",
        "wpm_estimate",
        "late_night_ratio",
        "session_count",
        "avg_session_duration",
    ]]
    result.index = pd.Index(result.index, name="date")
    return result.sort_index()