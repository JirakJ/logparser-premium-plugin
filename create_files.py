import os

os.makedirs("src/main/kotlin/com/jakubjirak/logparser/premium/aggregation", exist_ok=True)
os.makedirs("src/main/kotlin/com/jakubjirak/logparser/premium/routing", exist_ok=True)
os.makedirs("src/main/kotlin/com/jakubjirak/logparser/premium/incident", exist_ok=True)
os.makedirs("src/main/kotlin/com/jakubjirak/logparser/premium/heatmap", exist_ok=True)
print("Directories created successfully")
