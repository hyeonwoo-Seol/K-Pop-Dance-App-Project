import unittest

from temporal_filter import (
    apply_error_hysteresis,
    build_temporal_error_flags,
    consolidate_flag_windows,
    exponential_moving_average,
    suppress_short_error_runs,
)


class TemporalFilterTests(unittest.TestCase):
    def test_exponential_moving_average_uses_configured_span(self):
        self.assertEqual(
            exponential_moving_average([100.0, 0.0, 0.0], span=3),
            [100.0, 50.0, 25.0],
        )

    def test_hysteresis_uses_separate_error_enter_and_exit_thresholds(self):
        flags = apply_error_hysteresis(
            [80.0, 68.0, 64.0, 70.0, 76.0],
            error_enter_threshold=65.0,
            error_exit_threshold=75.0,
        )

        self.assertEqual(flags, [0, 0, 1, 1, 0])

    def test_short_error_runs_are_removed_but_long_runs_remain(self):
        flags = [0, 1, 1, 0, 1, 1, 1, 1, 1, 0]

        self.assertEqual(
            suppress_short_error_runs(flags, max_short_error_frames=4),
            [0, 0, 0, 0, 1, 1, 1, 1, 1, 0],
        )

    def test_eight_errors_make_entire_ten_frame_window_an_error(self):
        flags = [1] * 8 + [0] * 2

        self.assertEqual(consolidate_flag_windows(flags), [1] * 10)

    def test_three_normal_flags_make_entire_ten_frame_window_normal(self):
        flags = [1] * 7 + [0] * 3

        self.assertEqual(consolidate_flag_windows(flags), [0] * 10)

    def test_windows_are_consolidated_independently(self):
        flags = [1] * 8 + [0] * 2 + [1] * 7 + [0] * 3

        self.assertEqual(
            consolidate_flag_windows(flags),
            [1] * 10 + [0] * 10,
        )

    def test_partial_window_is_preserved_without_enough_votes(self):
        flags = [1, 1, 0, 0]

        self.assertEqual(consolidate_flag_windows(flags), flags)

    def test_pipeline_filters_each_joint_independently(self):
        joint_scores = [
            [90.0, 90.0],
            [40.0, 90.0],
            [40.0, 90.0],
            [40.0, 90.0],
            [40.0, 90.0],
            [40.0, 90.0],
            [90.0, 90.0],
        ]

        flags = build_temporal_error_flags(
            joint_scores,
            ema_window=1,
            max_short_error_frames=4,
        )

        self.assertEqual([row[0] for row in flags], [0, 1, 1, 1, 1, 1, 0])
        self.assertEqual([row[1] for row in flags], [0, 0, 0, 0, 0, 0, 0])

    def test_pipeline_applies_ten_frame_thresholds(self):
        joint_scores = (
            [[40.0]] * 8
            + [[90.0]] * 2
            + [[40.0]] * 7
            + [[90.0]] * 3
        )

        flags = build_temporal_error_flags(
            joint_scores,
            ema_window=1,
            max_short_error_frames=0,
        )

        self.assertEqual(
            [row[0] for row in flags],
            [1] * 10 + [0] * 10,
        )

    def test_invalid_threshold_order_is_rejected(self):
        with self.assertRaises(ValueError):
            apply_error_hysteresis(
                [70.0],
                error_enter_threshold=75.0,
                error_exit_threshold=65.0,
            )


if __name__ == "__main__":
    unittest.main()
