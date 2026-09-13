"""Temporal filtering utilities for per-frame joint error flags."""

import math


def exponential_moving_average(values, span=12, missing_value=100.0):
    """Return a causal EMA using the conventional ``2 / (span + 1)`` alpha."""
    if span < 1:
        raise ValueError("span must be at least 1")

    alpha = 2.0 / (span + 1.0)
    smoothed = []
    previous = None

    for value in values:
        if value is None or not math.isfinite(float(value)):
            current = previous if previous is not None else float(missing_value)
        else:
            numeric_value = float(value)
            current = (
                numeric_value
                if previous is None
                else alpha * numeric_value + (1.0 - alpha) * previous
            )

        smoothed.append(current)
        previous = current

    return smoothed


def apply_error_hysteresis(
    scores,
    error_enter_threshold=65.0,
    error_exit_threshold=75.0,
):
    """Convert scores to flags where 0 is normal and 1 is an error.

    A normal joint enters the error state below ``error_enter_threshold``.
    Once in the error state, it remains there until the score reaches
    ``error_exit_threshold``. This avoids rapid flag changes near a threshold.
    """
    if error_enter_threshold >= error_exit_threshold:
        raise ValueError("error_enter_threshold must be lower than error_exit_threshold")

    flags = []
    is_error = False

    for score in scores:
        if not is_error and score < error_enter_threshold:
            is_error = True
        elif is_error and score >= error_exit_threshold:
            is_error = False
        flags.append(1 if is_error else 0)

    return flags


def suppress_short_error_runs(flags, max_short_error_frames=4):
    """Replace error runs no longer than the configured frame count with 0."""
    if max_short_error_frames < 0:
        raise ValueError("max_short_error_frames cannot be negative")

    filtered = list(flags)
    run_start = None

    for index in range(len(filtered) + 1):
        is_error = index < len(filtered) and filtered[index] == 1

        if is_error and run_start is None:
            run_start = index
        elif not is_error and run_start is not None:
            if index - run_start <= max_short_error_frames:
                filtered[run_start:index] = [0] * (index - run_start)
            run_start = None

    return filtered


def consolidate_flag_windows(
    flags,
    window_size=10,
    min_error_flags=8,
    min_normal_flags=3,
):
    """Assign one flag to each window using error/normal count thresholds.

    A full 10-frame window becomes all errors only when it contains at least
    eight error flags. Otherwise, three or more normal flags make the entire
    window normal. A trailing partial window is left unchanged when neither
    absolute threshold can be met.
    """
    if window_size < 1:
        raise ValueError("window_size must be at least 1")
    if not 1 <= min_error_flags <= window_size:
        raise ValueError("min_error_flags must be between 1 and window_size")
    if not 1 <= min_normal_flags <= window_size:
        raise ValueError("min_normal_flags must be between 1 and window_size")
    if min_error_flags + min_normal_flags <= window_size:
        raise ValueError("error and normal thresholds must not overlap")
    if any(flag not in (0, 1) for flag in flags):
        raise ValueError("flags must contain only 0 or 1")

    consolidated = list(flags)

    for start in range(0, len(consolidated), window_size):
        end = min(start + window_size, len(consolidated))
        window = consolidated[start:end]
        error_count = sum(window)
        normal_count = len(window) - error_count

        if error_count >= min_error_flags:
            consolidated[start:end] = [1] * len(window)
        elif normal_count >= min_normal_flags:
            consolidated[start:end] = [0] * len(window)

    return consolidated


def build_temporal_error_flags(
    joint_scores_by_frame,
    ema_window=12,
    error_enter_threshold=65.0,
    error_exit_threshold=75.0,
    max_short_error_frames=4,
):
    """Filter frame-by-frame joint scores and return per-joint 0/1 flags."""
    if not joint_scores_by_frame:
        return []

    joint_count = len(joint_scores_by_frame[0])
    if any(len(row) != joint_count for row in joint_scores_by_frame):
        raise ValueError("joint score rows must all have the same length")

    flags_by_frame = [[0] * joint_count for _ in joint_scores_by_frame]

    for joint_index in range(joint_count):
        raw_scores = [row[joint_index] for row in joint_scores_by_frame]
        smoothed_scores = exponential_moving_average(raw_scores, span=ema_window)
        flags = apply_error_hysteresis(
            smoothed_scores,
            error_enter_threshold=error_enter_threshold,
            error_exit_threshold=error_exit_threshold,
        )
        flags = suppress_short_error_runs(
            flags,
            max_short_error_frames=max_short_error_frames,
        )
        flags = consolidate_flag_windows(flags)

        for frame_index, flag in enumerate(flags):
            flags_by_frame[frame_index][joint_index] = flag

    return flags_by_frame
