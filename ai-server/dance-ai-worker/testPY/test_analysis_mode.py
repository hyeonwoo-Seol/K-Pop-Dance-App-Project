import os
import sys
import unittest


WORKER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if WORKER_DIR not in sys.path:
    sys.path.insert(0, WORKER_DIR)

from analysis_mode import (  # noqa: E402
    is_joint_visualization_request,
    prepare_joint_visualization_result,
)


class JointVisualizationModeTest(unittest.TestCase):
    def test_marker_filename_is_visualization_request(self):
        self.assertTrue(
            is_joint_visualization_request(
                "/tmp/user123_jointviz_demo_0_123456789.mp4",
                "jointviz_demo_0",
                "user123",
            )
        )

    def test_regular_practice_filename_is_not_visualization_request(self):
        self.assertFalse(
            is_joint_visualization_request(
                "/tmp/user123_540_원영_1_123456789.mp4",
                "540_원영_1",
                "user123",
            )
        )

    def test_result_has_only_normal_joint_flags(self):
        result = {
            "metadata": {},
            "summary": {"total_score": 77},
            "frames": [{"errors": [1, 1]}, {"errors": []}],
        }

        prepared = prepare_joint_visualization_result(result)

        self.assertEqual("joint_visualization", prepared["metadata"]["analysis_mode"])
        self.assertEqual("NotScored", prepared["summary"]["accuracy_grade"])
        self.assertTrue(
            all(frame["errors"] == [0] * 18 for frame in prepared["frames"])
        )


if __name__ == "__main__":
    unittest.main()
