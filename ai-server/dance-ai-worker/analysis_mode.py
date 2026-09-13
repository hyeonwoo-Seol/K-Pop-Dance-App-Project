import os


JOINT_VISUALIZATION_MARKER = "jointviz"
JOINT_COUNT = 18


def is_joint_visualization_request(video_path, song_id, user_id):
    """Identify the demo mode without changing the existing SQS message contract."""
    video_name = os.path.splitext(os.path.basename(video_path))[0]
    prefix = f"{user_id}_"
    request_identifier = (
        video_name[len(prefix):]
        if video_name.startswith(prefix)
        else str(song_id or "")
    )
    return request_identifier == JOINT_VISUALIZATION_MARKER or request_identifier.startswith(
        f"{JOINT_VISUALIZATION_MARKER}_"
    )


def prepare_joint_visualization_result(result_data):
    """Mark every joint as normal because visualization mode has no expert."""
    result_data["metadata"]["analysis_mode"] = "joint_visualization"
    result_data["summary"] = {
        "total_score": 0,
        "accuracy_grade": "NotScored",
        "part_accuracies": {},
        "worst_points": [],
    }
    for frame in result_data["frames"]:
        # Android overlay contract: 0 = normal, 1 = error.
        frame["errors"] = [0] * JOINT_COUNT
    return result_data
