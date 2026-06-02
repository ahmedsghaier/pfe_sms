# ml_prediction.py
import pandas as pd
import joblib
from datetime import datetime
import os

# Chemins vers tes fichiers du modèle
MODEL_PATH = r"C:\Users\R.Ben Abdesslem\Documents\pfe_Sms\resultats_phase2\ensemble_phase2_v13.pkl"

TIME_MATRIX_PATH = r"C:\Users\R.Ben Abdesslem\Documents\pfe_Sms\resultats_phase2\time_score_matrix_v13.pkl"

# Chargement une seule fois au démarrage
model = joblib.load(MODEL_PATH)
time_matrix = joblib.load(TIME_MATRIX_PATH)

def get_nlp_type_from_message(message: str):
    msg = message.lower()
    if any(k in msg for k in ['otp', 'code', 'vérif', 'pin']):
        return 'OTP'
    elif any(k in msg for k in ['transaction', 'paiement', 'virement', 'solde']):
        return 'Transaction'
    elif any(k in msg for k in ['promo', 'offre', 'réduction', 'soldes']):
        return 'Promotion'
    elif any(k in msg for k in ['livraison', 'colis', 'tracking']):
        return 'Livraison'
    elif any(k in msg for k in ['alerte', 'sécurité']):
        return 'Alerte'
    return 'Information'

def predict_engagement(message: str, start_date: str = None):
    nlp_type = get_nlp_type_from_message(message)
    
    # Meilleur horaire selon le modèle
    if nlp_type in time_matrix.columns:
        best_hour = int(time_matrix[nlp_type].idxmax())
    else:
        best_hour = 10 if nlp_type in ['OTP', 'Transaction', 'Alerte'] else 19

    best_window = "Matin" if best_hour <= 11 else "Soir" if best_hour >= 17 else "Après-midi"

    # Taux d'engagement de base (simulé à partir de ton modèle)
    base_rates = {
        'OTP': 31.5, 'Transaction': 27.8, 'Alerte': 33.0,
        'Promotion': 19.5, 'Livraison': 24.2, 'Information': 15.8
    }
    base_rate = base_rates.get(nlp_type, 16.0)

    # Ajustement selon l'heure choisie
    date_score = 100
    chosen_hour = None
    if start_date:
        try:
            dt = datetime.fromisoformat(start_date.replace('Z', ''))
            chosen_hour = dt.hour
            if nlp_type in ['OTP', 'Transaction', 'Alerte']:
                date_score = 95 if 7 <= chosen_hour <= 11 else 60 if 12 <= chosen_hour <= 16 else 35
            elif nlp_type in ['Promotion', 'Livraison']:
                date_score = 92 if 17 <= chosen_hour <= 22 else 70 if 12 <= chosen_hour <= 16 else 45
        except:
            pass

    final_rate = round(base_rate * (date_score / 100), 1)

    return {
        "nlp_type": nlp_type,
        "predicted_engagement_rate": final_rate,
        "best_hour": best_hour,
        "best_window": best_window,
        "chosen_hour_score": date_score,
        "chosen_hour": chosen_hour,
        "recommendation": f"Le meilleur moment est {best_hour}h ({best_window})."
    }