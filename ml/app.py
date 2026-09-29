from flask import Flask, request, jsonify
from sms_recommender import SMSRecommender
import os

app = Flask(__name__)
engine = SMSRecommender()

MODEL_PATH = "models/sms_model_v2.json"
CSV_PATH   = "data/full_dataset.csv"

# Au démarrage : entraîne si pas de modèle pré-existant
if os.path.exists(CSV_PATH):
    engine.train(CSV_PATH)
else:
    print("[WARN] Aucun dataset trouvé — service démarré sans modèle entraîné.")

@app.get("/health")
def health():
    return jsonify({"status": "UP"}), 200

@app.post("/recommend")
def recommend():
    data = request.json
    result = engine.recommend(data["partialText"], top_k=data.get("topK", 5))
    return jsonify(result)

@app.post("/feedback")
def feedback():
    data = request.json
    engine.record_feedback(data["partialText"], data["selectedWord"])
    return jsonify({"status": "ok"})

@app.post("/retrain")
def retrain():
    entries = request.json.get("entries", [])
    for e in entries:
        engine.record_feedback(e["partialText"], e["selectedWord"])
    return jsonify({"status": "ok", "count": len(entries)})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)