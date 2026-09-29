#!/usr/bin/env python3
"""
nlp_detector.py — Détection NLP basée EXACTEMENT sur prediction_engine.py
Utilise preprocess_multilingual() et apply_post_corrections() du vrai modèle.
S'exécute en < 1 seconde (pas d'entraînement ML).

Usage: python nlp_detector.py input.json output.json
"""

import sys
import json
import re
import os


# ══════════════════════════════════════════════════════════════
# 1. PRÉTRAITEMENT — COPIÉ EXACTEMENT DE prediction_engine.py
# ══════════════════════════════════════════════════════════════

def preprocess_multilingual(text):
    if not isinstance(text, str):
        return ""

    text = text.lower()

    # URL
    text = re.sub(
        r'https?://\S+|www\.\S+|bit\.ly/\S+|tinyurl\.com/\S+|rb\.gy/\S+|tn\.gl/\S+',
        ' URL ', text
    )

    # Montants (DT, Euro, Dollar, Millimes)
    text = re.sub(
        r'\d+[\.,]?\d*\s*(€|dt|dtw|dtn|eur|usd|dollar|dinar|د\.ت|دينار|مليم|millimes?|points?|pt)',
        ' MONTANT ', text
    )

    # Codes (OTP, codes promo, références)
    text = re.sub(r'\b\d{4,8}\b', ' CODE ', text)

    # Numéros de téléphone tunisiens
    text = re.sub(r'\b(216)?[2459]\d{7}\b', ' TEL ', text)

    # BANQUE & FINANCE
    text = re.sub(
        r'\b(carte|card|karta|virement|virtech|solde|sold|balance|compte|account|'
        r'bna|stb|biat|attijari|uib|d17|flouci|ziggy|sobflous|konnect|paymee|paycard|'
        r'amen|bh|zitouna|wifak|bt|btl|bte|abc|comar|'
        r'bank|banque|crédit|prêt|épargne|retrait|dépôt|withdraw|transfer|deposit|'
        r'dinar|millimes|dab|guichet|agence|rib|iban|e-dinar|dinarclick|'
        r'بطاقة|حساب|رصيد|تحويل|بنك|قرض|ادخار|دفعة|سحب|إيداع|دينار|مليم|صراف|محفظة|'
        r'flouss|flouci|virma|crédi|solda)\b',
        ' BANK_KW ', text
    )

    # TÉLÉCOM
    text = re.sub(
        r'\b(recharge|sob|sobli|chihna|forfait|forfé|pack|bundle|internet|interné|net|data|'
        r'mega|megas|giga|jigas|ko|mo|go|illimité|unlimited|'
        r'ooredoo|oredo|orange|tt|telecom|tunisie_telecom|elissa|lycamobile|'
        r'adsl|vdsl|fibre|fiber|ftth|4g|5g|3g|2g|lte|edge|wifi|'
        r'topnet|globalnet|hexabyte|planet|satoripop|'
        r'sim|puce|ligne|line|activation|résiliation|portabilité|roaming|'
        r'appels|calls|sms|mms|balance|crédit|credit|bonus|promotion|offre|'
        r'réseau|network|couverture|débit|connexion|modem|routeur|box|ussd|'
        r'تليكوم|اتصالات|اورنج|اوريدو|انترنات|انترنت|شحن|رصيد|شريحة|عرض|باقة|'
        r'ميغا|جيجا|مكالمات|رسائل|خط|شبكة|واي_فاي|مودم|فايبر|صيانة|'
        r'nta9s|khdem|ma5demnich|cher7i)\b',
        ' TEL_KW ', text
    )

    # IMMOBILIER
    text = re.sub(
        r'\b(appartement|appart|villa|studio|duplex|maison|dar|terrain|local|bureau|'
        r's\+0|s\+1|s\+2|s\+3|s\+4|s\+5|s\+6|'
        r'location|louer|loyer|vente|acheter|rent|sale|buy|krira|'
        r'standing|haut_standing|luxe|moderne|neuf|meublé|équipé|climatisé|'
        r'résidence|cité|lotissement|jardin|terrasse|piscine|vue_mer|ascenseur|'
        r'tunis|ariana|sousse|sfax|nabeul|hammamet|lac1|lac2|menzah|manar|ennasr|'
        r'agence|promoteur|titre|cadastre|contrat|bail|caution|'
        r'عقار|شقة|دار|منزل|فيلا|ستوديو|كراء|بيع|اجار|راقي|مفروش|حديقة|مصعد|'
        r'9a3a|bit|koujina|manta|mekla|sana)\b',
        ' IMMO_KW ', text
    )

    # LOGISTIQUE
    text = re.sub(
        r'\b(colis|livraison|suivi|shipping|delivery|tracking|order|commande|'
        r'aramex|jumia|toussil|express|ups|dhl|fedex|package|expédition|'
        r'transporteur|coursier|adresse|destinataire|réception|retour|'
        r'طرد|توصيل|ارسال|طلب|تتبع|عنوان|استلام|'
        r'tousil|wsel|wassal)\b',
        ' LOG_KW ', text
    )

    # ADMINISTRATION
    text = re.sub(
        r'\b(steg|sonede|cnam|cnss|cnrps|aneti|ministere|ministère|mairie|baladiya|'
        r'municipalité|extrait|naissance|madhmoun|impôt|taxe|vignette|facture|'
        r'cin|passeport|permis|tribunal|amende|convocation|guichet|'
        r'وزارة|بلدية|قباضة|فاتورة|كنام|مضمون|ولادة|محكمة|قانون|مرسوم|'
        r'جواز_سفر|بطاقة_تعريف|رخصة|أداءات|ضرائب|منحة|'
        r'wra9|bt3|mathmoun|baladeya)\b',
        ' ADMIN_KW ', text
    )

    # DARIJA TUNISIENNE
    darija_norm = {
        'asslema': 'BONJOUR', 'salam': 'BONJOUR', 'salem': 'BONJOUR',
        'chokran': 'MERCI', 'merci': 'MERCI',
        '3afak': 'SVP', 'raja': 'SVP',
        'lyoum': 'AUJOURDHUI', 'ghodwa': 'DEMAIN',
        'tawa': 'MAINTENANT', 'taw': 'MAINTENANT',
        'fisa3': 'URGENT', '3ajel': 'URGENT',
        'sob': 'RECHARGER', 'sobli': 'RECHARGER', 'chihna': 'RECHARGER',
        'flouss': 'ARGENT', 'flouci': 'ARGENT',
        'gratui': 'GRATUIT', 'cadou': 'CADEAU',
        'nta9s': 'CREDIT_INSUFFISANT',
        'jigas': 'GIGAS', 'megas': 'MEGAS',
        'forfé': 'FORFAIT', 'interné': 'INTERNET',
        'khdem': 'FONCTIONNE', 'ma5demnich': 'NE_FONCTIONNE_PAS',
        'mabrouk': 'FELICITATIONS',
    }
    for darija, norm in darija_norm.items():
        text = re.sub(r'\b' + re.escape(darija) + r'\b', norm, text)

    text = re.sub(r'[^\w\s\u0600-\u06FF]', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()

    return text


# ══════════════════════════════════════════════════════════════
# 2. DÉTECTION NLP — BASÉE SUR apply_post_corrections() + extract_nlp_features()
# ══════════════════════════════════════════════════════════════

def detect_nlp_type(message):
    """
    Détecte le type NLP d'un message SMS tunisien.
    Logique extraite de apply_post_corrections() et extract_nlp_features()
    de prediction_engine.py — MÊME priorité, MÊMES règles.
    """
    if not message or not isinstance(message, str):
        return "Information", "Support"

    text_raw = message.lower()
    clean    = preprocess_multilingual(message)

    # ── PRIORITÉ 1 : OTP ─────────────────────────────────────
    # Extrait de apply_post_corrections() section BANQUE type OTP
    # et extract_nlp_features() is_otp
    if re.search(r'\b(code.*verif|otp|code.*\d{4,6}|pin.*\d|vérification|'
                 r'code|validation|confirmer|secret|numéro de sécurité|'
                 r'mot de passe temporaire)\b', text_raw):
        # Vérifier que ce n'est pas une transaction (priorité transaction > OTP
        # seulement si mouvement d'argent explicite)
        is_transaction = bool(re.search(
            r'\b(retrait|paiement effectué|virement effectué|versé|alimenté|'
            r'tpe|prélèvement|échéance|achat confirmé)\b', text_raw))
        if not is_transaction:
            return "OTP", "Banque"

    # ── PRIORITÉ 2 : ALERTE ───────────────────────────────────
    if re.search(r'\b(alerte|connexion suspecte|sécurité|suspect|'
                 r'détecté|activité inhabituelle|attention|warning|'
                 r'tentative|bloqué)\b', text_raw):
        return "Alerte", "Banque"

    # ── PRIORITÉ 3 : TRANSACTION ─────────────────────────────
    # apply_post_corrections() BANQUE → type Transaction
    if (re.search(r' BANK_KW ', clean) or re.search(r' MONTANT ', clean)) and \
            re.search(r'\b(retrait|paiement|virement|versé|alimenté|tpe|'
                      r'prélèvement|échéance|achat|débit|crédit|opération|'
                      r'transaction|transfert|reçu|envoyé)\b', text_raw):
        return "Transaction", "Banque"

    # Transaction sans BANK_KW mais avec MONTANT + verbe financier
    if re.search(r' MONTANT ', clean) and \
            re.search(r'\b(paiement|virement|retrait|dépôt|reçu|envoyé|débité|crédité)\b', text_raw):
        return "Transaction", "Banque"

    # ── PRIORITÉ 4 : LIVRAISON ───────────────────────────────
    # apply_post_corrections() LOGISTIQUE
    if re.search(r' LOG_KW ', clean) or \
            re.search(r'\b(colis|livraison|tracking|delivery|expédition|'
                      r'aramex|dhl|fedex|jumia|toussil)\b', text_raw):
        return "Livraison", "Logistique"

    # ── PRIORITÉ 5 : PROMOTION ───────────────────────────────
    # apply_post_corrections() MARKETING promotions
    if re.search(r'\b(promo|promotion|offre spéciale|soldes|réduction|'
                 r'remise|discount|gratuit|cadeau|bon de réduction|'
                 r'-\d+%|jusqu\'à \d+%|code promo|coupon)\b', text_raw):
        return "Promotion", "Marketing"

    # ── PRIORITÉ 6 : RAPPEL / SANTÉ ──────────────────────────
    # apply_post_corrections() SANTÉ + rappels
    if re.search(r'\b(rendez-vous|rdv|consultation|rappel|confirmer votre|'
                 r'n\'oubliez pas|demain à|médecin|docteur|clinique|'
                 r'vaccin|analyse|résultat médical)\b', text_raw):
        domaine = "Santé" if re.search(
            r'\b(médecin|docteur|clinique|hôpital|pharmacie|vaccin|analyse)\b',
            text_raw) else "Administration"
        return "Rappel", domaine

    # ── PRIORITÉ 7 : VŒUX ────────────────────────────────────
    # apply_post_corrections() MARKETING vœux
    if re.search(r'\b(mabrouk|3id|aid|ramadan|bonne année|anniversaire|'
                 r'joyeux|félicitations|bon courage|meilleurs vœux|'
                 r'عيد|رمضان|مبروك)\b', text_raw):
        return "Felicitation", "Marketing"

    # ── PRIORITÉ 8 : TÉLÉCOM → Information ───────────────────
    # apply_post_corrections() TÉLÉCOM
    if re.search(r' TEL_KW ', clean):
        return "Information", "Télécom"

    # ── PRIORITÉ 9 : IMMOBILIER → Information ────────────────
    if re.search(r' IMMO_KW ', clean):
        return "Information", "Immobilier"

    # ── PRIORITÉ 10 : ADMINISTRATION ─────────────────────────
    if re.search(r' ADMIN_KW ', clean):
        return "Information", "Administration"

    # ── DÉFAUT ────────────────────────────────────────────────
    return "Information", "Support"


# ══════════════════════════════════════════════════════════════
# 3. MAIN
# ══════════════════════════════════════════════════════════════

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(json.dumps({"error": "Usage: nlp_detector.py input.json output.json"}))
        sys.exit(1)

    input_file  = sys.argv[1]
    output_file = sys.argv[2]

    with open(input_file, 'r', encoding='utf-8-sig') as f:
        data = json.load(f)

    message = data.get("message", "")

    nlp_type, domaine = detect_nlp_type(message)

    result = {
        "nlp_type":       nlp_type,
        "domaine":        domaine,
        "message_length": len(message),
        "clean_preview":  preprocess_multilingual(message)[:100]
    }

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False)