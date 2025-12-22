from ultralytics import YOLO

def print_model_layers():
    # 1. 모델을 불러온다.
    model = YOLO("yolo11l-pose.pt")
    
    # 2. 모델 구조를 출력한다.
    print(f"{'Idx':<5} {'Module':<20} {'Arguments'}")
    print("-" * 50)
    
    for i, (name, params) in enumerate(model.model.named_modules()):
        # 최상위 레이어만 필터링하여 출력한다.
        if name.startswith("model.") and name.count(".") == 1:
            idx = int(name.split(".")[1])
            module_type = params.__class__.__name__
            print(f"{idx:<5} {module_type:<20}")

            # Backbone의 끝을 찾는다.
            if module_type == "SPPF":
                print(f"   >>> [Backbone Ends Here] Index: {idx}")
            
            # Head를 찾는다.
            if module_type == "Pose":
                print(f"   >>> [Head Starts Here] Index: {idx}")

if __name__ == "__main__":
    print_model_layers()
