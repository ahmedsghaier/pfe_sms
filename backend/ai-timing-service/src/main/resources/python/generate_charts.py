# ml/generate_charts.py
import json
import sys
import matplotlib
matplotlib.use('Agg')  # pas d'affichage interactif, juste export fichier
import matplotlib.pyplot as plt

OUTPUT_DIR = "/app/models/charts"

def generate_hourly_curve_chart(timing_results_path):
    with open(timing_results_path, "r", encoding="utf-8") as f:
        data = json.load(f)["timing_results"]

    by_type = {}
    for entry in data:
        nlp_type = entry["nlp_type"]
        by_type.setdefault(nlp_type, entry.get("hourly_curve", []))

    plt.figure(figsize=(8, 4))
    for nlp_type, curve in by_type.items():
        hours = [p["hour"] for p in curve]
        scores = [p["time_score_norm"] for p in curve]
        plt.plot(hours, scores, label=nlp_type)

    plt.title("time_score_norm par type")
    plt.xlabel("Heure")
    plt.ylabel("time_score_normalisé")
    plt.legend()
    plt.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig(f"{OUTPUT_DIR}/hourly_curve_by_type.png", dpi=100)
    plt.close()

def generate_engagement_tier_chart(engagement_by_tier_path):
    with open(engagement_by_tier_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    tiers = [d["tier"] for d in data]
    rates = [d["engagementRate"] * 100 for d in data]

    plt.figure(figsize=(5, 4))
    bars = plt.bar(tiers, rates, color=["#9ecae1", "#4292c6", "#08519c"])
    for bar, rate in zip(bars, rates):
        plt.text(bar.get_x() + bar.get_width()/2, rate + 1, f"{rate:.1f}%", ha='center')
    plt.title("Engagement par tier MSISDN")
    plt.ylabel("Taux engagement (%)")
    plt.ylim(0, 60)
    plt.tight_layout()
    plt.savefig(f"{OUTPUT_DIR}/engagement_by_tier.png", dpi=100)
    plt.close()

if __name__ == "__main__":
    generate_hourly_curve_chart(sys.argv[1])
    generate_engagement_tier_chart(sys.argv[2])