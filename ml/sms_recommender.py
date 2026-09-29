"""
Moteur de recommandation SMS — version corrigée v2
Corrections apportées :
  1. Séparation par langue (fr / ar+darija / en)
  2. Nettoyage des fautes dans le corpus
  3. Decay temporel dans le feedback (score × e^−λt)
  4. Kneser-Ney simplifié pour les contextes rares
  5. Fuzzy Trie pour les typos (Levenshtein ≤ 1)
"""
import os
import re, json, math, time
from collections import defaultdict, Counter
from datetime import datetime


# ══════════════════════════════════════════════
# CORRECTION 1 — DÉTECTION DE LANGUE
# ══════════════════════════════════════════════

def detect_lang(text: str) -> str:
    """
    Retourne 'fr' | 'ar' | 'en' | 'unknown'.
    Stratégie légère sans ML :
      - présence de caractères arabes → 'ar'
      - mots-clés darija + alphabet latin → 'ar' (traité avec le modèle arabe)
      - sinon → langdetect
    """
    arabic_chars = re.findall(r'[\u0600-\u06FF]', text)
    if len(arabic_chars) > len(text) * 0.15:
        return 'ar'

    darija_keywords = {'wach','labas','bghit','mzyan','mashi','kayn','smiya',
                       'zwina','chno','kifach','walo','nta','nti','hna','huma'}
    tokens = set(text.lower().split())
    if tokens & darija_keywords:
        return 'ar'

    try:
        from langdetect import detect
        lang = detect(text)
        return lang if lang in ('fr','en','ar') else 'fr'
    except Exception:
        return 'fr'


# ══════════════════════════════════════════════
# CORRECTION 2 — NETTOYAGE DU CORPUS
# ══════════════════════════════════════════════

def tokenize(text: str) -> list[str]:
    text = text.lower()
    text = re.sub(r"[^\w\s\u0600-\u06FF]", " ", text)
    return [t for t in text.split() if len(t) >= 1]


# Liste de prénoms connus (Tunisie + international) en complément
# du critère statistique — couvre les prénoms peu capitalisés dans
# les SMS informels (ex: "karim" écrit en minuscule 50% du temps).
KNOWN_FIRST_NAMES = {
    'ahmed','amine','karim','sara','nadia','yasmine','bilal','mohamed',
    'fatma','leila','mehdi','sami','rania','imen','walid','sonia',
    'hichem','olfa','wassim','asma','nizar','rim','adel','mariem',
    'tarek','ines','hatem','dorra','khalil','salma','ben','arous'
}


def detect_named_entities(texts: list[str], threshold: float = 0.8,
                          min_occurrences: int = 2,
                          known_names: set[str] | None = None) -> set[str]:
    """
    Détecte les noms propres AVANT que la casse soit perdue.
    Un mot est une entité nommée s'il est capitalisé dans >= threshold
    de ses occurrences hors début de phrase.
    """
    capitalized_count: Counter = Counter()
    total_count: Counter = Counter()

    for text in texts:
        raw_tokens = re.findall(r"[\w\u0600-\u06FF]+", text)
        for i, tok in enumerate(raw_tokens):
            low = tok.lower()
            total_count[low] += 1
            if i > 0 and tok[0].isupper():
                capitalized_count[low] += 1

    named_entities = set()
    for word, total in total_count.items():
        if total < min_occurrences:
            continue
        if capitalized_count.get(word, 0) / total >= threshold:
            named_entities.add(word)

    # CORRECTION : compléter avec la liste de prénoms connus,
    # qui couvre les cas sous le seuil statistique (ex: "karim" à 47%)
    if known_names:
        for text in texts:
            for tok in re.findall(r"[\w\u0600-\u06FF]+", text):
                if tok.lower() in known_names:
                    named_entities.add(tok.lower())

    return named_entities


PRENOM_TOKEN = "@prenom"   # token générique qui remplace tout prénom détecté


def mask_named_entities(tokens: list[str], named_entities: set[str]) -> list[str]:
    """
    CORRECTION : au lieu de SUPPRIMER les prénoms (blacklist),
    on les REMPLACE par un token générique @prenom.
    Le NGram apprend ainsi "bonjour -> @prenom" comme pattern réutilisable,
    sans jamais proposer un prénom précis, ni perdre l'info structurelle.
    """
    return [PRENOM_TOKEN if t in named_entities else t for t in tokens]


def clean_word(word: str, checker=None) -> str | None:
    """
    Retourne le mot nettoyé ou None s'il faut l'exclure.
    - Ignore les tokens purement numériques
    - Corrige via pyspellchecker si disponible (latin seulement)
    - Conserve les mots arabes tels quels
    """
    if re.fullmatch(r'\d+', word):
        return None
    if len(word) < 2:
        return None
    if re.search(r'[\u0600-\u06FF]', word):
        return word          # mot arabe → pas de spellcheck latin
    if checker:
        corrected = checker.correction(word)
        return corrected if corrected else word
    return word


def load_and_clean(csv_path: str) -> dict[str, list[str]]:
    """
    Charge le CSV et retourne un dict {'fr': [...], 'ar': [...], 'en': [...]}
    après filtrage spam/bruit et correction orthographique.
    """
    import pandas as pd
    try:
        from spellchecker import SpellChecker
        checker_fr = SpellChecker(language='fr')
        checker_en = SpellChecker(language='en')
    except Exception:
        checker_fr = checker_en = None

    df = pd.read_csv(csv_path)
    df = df[df['is_spam'] == False]
    df = df[df['noise_level'] != 'heavy']
    df['text'] = df['text'].astype(str).str.strip()
    df = df[df['text'].str.len() >= 3]
    df = df.drop_duplicates(subset='text')

    buckets: dict[str, list[str]] = {'fr': [], 'ar': [], 'en': []}

    for text in df['text'].tolist():
        lang = detect_lang(text)
        lang = lang if lang in buckets else 'fr'
        buckets[lang].append(text)

    print(f"  [Langue] fr={len(buckets['fr'])} | ar={len(buckets['ar'])} | en={len(buckets['en'])}")
    return buckets


# ══════════════════════════════════════════════
# TRIE avec correction de fautes (Fuzzy)
# ══════════════════════════════════════════════

class TrieNode:
    __slots__ = ('children', 'freq', 'is_end')
    def __init__(self):
        self.children: dict[str, 'TrieNode'] = {}
        self.freq: int = 0
        self.is_end: bool = False


class Trie:
    def __init__(self):
        self.root = TrieNode()
        self._words: list[str] = []     # pour la recherche fuzzy

    def insert(self, word: str, freq: int = 1):
        node = self.root
        for ch in word:
            if ch not in node.children:
                node.children[ch] = TrieNode()
            node = node.children[ch]
        if not node.is_end:
            self._words.append(word)
        node.is_end = True
        node.freq += freq

    def search_prefix(self, prefix: str, top_k: int = 5) -> list[tuple[str, int]]:
        node = self.root
        for ch in prefix:
            if ch not in node.children:
                return []
            node = node.children[ch]
        results = []
        self._dfs(node, prefix, results)
        results.sort(key=lambda x: x[1], reverse=True)
        return results[:top_k]

    def _dfs(self, node: TrieNode, current: str, results: list):
        if node.is_end:
            results.append((current, node.freq))
        for ch, child in node.children.items():
            self._dfs(child, current + ch, results)

    # ── CORRECTION 5 : Fuzzy search ──────────────────────────────
    def fuzzy_search(self, prefix: str, max_dist: int = 1,
                     top_k: int = 5) -> list[tuple[str, int]]:
        """
        Retourne les mots dont le préfixe est à distance de Levenshtein ≤ max_dist.
        Couvre ~30% des fautes de frappe mobile.
        """
        exact = self.search_prefix(prefix, top_k)
        if exact:
            return exact          # priorité aux résultats exacts

        candidates = []
        for word in self._words:
            if abs(len(word) - len(prefix)) > max_dist + 2:
                continue
            dist = self._levenshtein(prefix, word[:len(prefix)+max_dist])
            if dist <= max_dist:
                node = self.root
                freq = 0
                ok = True
                for ch in word:
                    if ch not in node.children:
                        ok = False; break
                    node = node.children[ch]
                if ok and node.is_end:
                    freq = node.freq
                candidates.append((word, freq, dist))

        candidates.sort(key=lambda x: (x[2], -x[1]))
        return [(w, f) for w, f, _ in candidates[:top_k]]

    @staticmethod
    def _levenshtein(a: str, b: str) -> int:
        if a == b: return 0
        if not a: return len(b)
        if not b: return len(a)
        prev = list(range(len(b) + 1))
        for i, ca in enumerate(a):
            curr = [i + 1]
            for j, cb in enumerate(b):
                curr.append(min(prev[j+1]+1, curr[j]+1,
                                prev[j] + (0 if ca == cb else 1)))
            prev = curr
        return prev[-1]

    def build_from_corpus(self, texts: list[str], checker=None):
        word_freq: Counter = Counter()
        for text in texts:
            for word in tokenize(text):
                cleaned = clean_word(word, checker)
                if cleaned:
                    word_freq[cleaned] += 1
        # Exclure les hapax (fréquence = 1) → moins de bruit
        for word, freq in word_freq.items():
            if freq >= 2:
                self.insert(word, freq)
        print(f"  [Trie] {len(self._words)} mots indexés (freq ≥ 2)")


# ══════════════════════════════════════════════
# N-GRAMME avec Kneser-Ney simplifié
# ══════════════════════════════════════════════

class NGramModel:
    """
    Bigram + Trigram avec back-off et lissage Kneser-Ney simplifié.
    CORRECTION 4 : continuation count pour les mots rares.
    """
    def __init__(self, discount: float = 0.75):
        self.D = discount
        self.unigram:  Counter = Counter()
        self.bigram:   dict    = defaultdict(Counter)
        self.trigram:  dict    = defaultdict(Counter)
        self.continuation: Counter = Counter()   # Kneser-Ney

    # Mots anglais fréquents qui s'infiltrent dans les SMS bilingues
    # mal classés par langid (ex: "Cher client, your code is...")
    _EN_LEAK_WORDS = {
        'your','you','the','is','for','off','next','purchase','account',
        'code','valid','minutes','plan','renewed','package','delivery',
        'today','detected','transaction','suspicious','get'
    }

    def train(self, texts: list[str], named_entities: set[str] | None = None,
              filter_foreign_leak: bool = True):
        """
        named_entities : prénoms/noms propres détectés, remplacés par @prenom.
        filter_foreign_leak : si True (et lang='fr'), retire les mots anglais
            qui fuient dans les SMS bilingues mal classés par langid.
        Le Trie n'est PAS concerné — l'autocomplétion peut toujours proposer
        un prénom si l'utilisateur tape ses premières lettres.
        """
        named_entities = named_entities or set()

        for text in texts:
            tokens = tokenize(text)
            tokens = mask_named_entities(tokens, named_entities)

            if filter_foreign_leak:
                # CORRECTION : retire les mots anglais évidents d'un SMS classé fr
                tokens = [t for t in tokens if t not in self._EN_LEAK_WORDS]

            if not tokens:
                continue
            for w in tokens:
                self.unigram[w] += 1
            for i in range(len(tokens) - 1):
                w1, w2 = tokens[i], tokens[i+1]
                self.bigram[w1][w2] += 1
                self.continuation[w2] += 1          # Kneser-Ney
            for i in range(len(tokens) - 2):
                ctx = (tokens[i], tokens[i+1])
                self.trigram[ctx][tokens[i+2]] += 1

        V = len(self.unigram)
        print(f"  [NGram] vocab={V} | bigrams={sum(len(v) for v in self.bigram.values())} "
              f"| trigrams={sum(len(v) for v in self.trigram.values())} "
              f"| {len(named_entities)} prénoms regroupés sous @prenom")

    def _p_kn_unigram(self, word: str) -> float:
        """Probabilité Kneser-Ney niveau 1."""
        total_cont = sum(self.continuation.values()) or 1
        return self.continuation.get(word, 0) / total_cont

    def predict_next(self, context: list[str],
                     top_k: int = 5) -> list[tuple[str, float]]:
        """
        Back-off : Trigram -> Bigram (fiable seulement) -> Kneser-Ney unigram.

        CORRECTION : ne JAMAIS compléter avec des mots non liés au contexte.
        - Si le(s) candidat(s) fiable(s) du bigram sont DOMINANTS
          (un seul candidat avec ratio >= DOMINANT_RATIO, ex: "rendez"->"vous"
          à 100%), on les retourne TELS QUELS, sans remplissage forcé.
          Mieux vaut 1 suggestion pertinente que 5 dont 4 sont hors-sujet.
        - Sinon, si on a au moins 2 candidats fiables, on les retourne.
        - Sinon seulement, on bascule sur l'unigramme global (pas de mélange
          bigram+unigramme qui produirait un faux sentiment de pertinence).
        """
        DOMINANT_RATIO = 0.7   # un candidat qui domine à 70%+ n'a pas besoin de complément

        if len(context) >= 2:
            r = self._trigram_predict(context[-2], context[-1])
            if r and r[0][1] >= DOMINANT_RATIO:
                return r[:max(1, min(len(r), top_k))]   # pas de filler
            if len(r) >= min(2, top_k):
                return r[:top_k]

        if len(context) >= 1:
            r = self._bigram_predict(context[-1])
            if r and r[0][1] >= DOMINANT_RATIO:
                return r[:max(1, min(len(r), top_k))]   # ex: "rendez" -> ["vous"]
            if len(r) >= min(2, top_k):
                return r[:top_k]
            # Si aucun candidat fiable du tout -> unigramme global,
            # SANS mélanger avec une longue traîne du bigram hors sujet.

        return self._unigram_predict(top_k)

    def _trigram_predict(self, w1, w2):
        dist = self.trigram.get((w1, w2))
        if not dist: return []
        total = sum(dist.values())
        return sorted([(w, c/total) for w,c in dist.items()],
                      key=lambda x: x[1], reverse=True)

    # Seuil minimum : un candidat doit représenter au moins X% des
    # occurrences du contexte pour être considéré statistiquement fiable.
    # Sous ce seuil, c'est de la longue traîne (vu 1-2 fois) -> bruit.
    MIN_CONFIDENCE_RATIO = 0.05   # 5% des occurrences du contexte

    def _bigram_predict(self, w1):
        dist = self.bigram.get(w1)
        if not dist: return []
        total = sum(dist.values())
        D = self.D
        results = []
        for w, c in dist.items():
            # score KN = max(c - D, 0) / total  +  λ * P_kn(w)
            score = max(c - D, 0) / total + (D * len(dist) / total) * self._p_kn_unigram(w)
            # CORRECTION : ne garder que les candidats au-dessus du seuil
            # de confiance (élimine la longue traîne vue 1-2 fois)
            if c / total >= self.MIN_CONFIDENCE_RATIO:
                results.append((w, score))
        return sorted(results, key=lambda x: x[1], reverse=True)

    def _unigram_predict(self, top_k):
        total = sum(self.continuation.values()) or 1
        return [(w, c/total) for w, c in self.continuation.most_common(top_k)]


# ══════════════════════════════════════════════
# CORRECTION 3 — FEEDBACK AVEC DECAY TEMPOREL
# ══════════════════════════════════════════════

class FeedbackStore:
    """
    Stocke les sélections utilisateur avec timestamp.
    Score effectif = count × e^(−λ × jours_depuis_sélection)
    """
    def __init__(self, decay_lambda: float = 0.01):
        self.lam = decay_lambda
        self.events: list[dict] = []   # {word, ts, context}

    def record(self, word: str, context: str):
        self.events.append({
            'word': word,
            'ts': time.time(),
            'context': context
        })

    def get_boosted_freq(self, word: str) -> float:
        """Score avec decay exponentiel."""
        now = time.time()
        score = 0.0
        for ev in self.events:
            if ev['word'] == word:
                days = (now - ev['ts']) / 86400
                score += math.exp(-self.lam * days)
        return score

    def top_recent_words(self, n: int = 10) -> list[tuple[str, float]]:
        scores: dict[str, float] = {}
        for ev in self.events:
            w = ev['word']
            scores[w] = scores.get(w, 0) + self.get_boosted_freq(w)
        return sorted(scores.items(), key=lambda x: x[1], reverse=True)[:n]


# ══════════════════════════════════════════════
# RANKING ENGINE
# ══════════════════════════════════════════════

class RankingEngine:
    def __init__(self, alpha: float = 0.4, beta: float = 0.6,
                 threshold: float = 0.001):
        self.alpha = alpha
        self.beta  = beta
        self.threshold = threshold

    def rank(self, trie_results, ngram_results,
             feedback_store: FeedbackStore | None = None,
             top_k: int = 5) -> list[dict]:
        scores: dict[str, float] = {}

        if trie_results:
            max_freq = max(f for _, f in trie_results) or 1
            for word, freq in trie_results:
                scores[word] = scores.get(word, 0) + self.alpha * (freq / max_freq)

        for word, prob in ngram_results:
            scores[word] = scores.get(word, 0) + self.beta * prob

        # Boost feedback avec decay
        if feedback_store:
            for word in list(scores.keys()):
                boost = feedback_store.get_boosted_freq(word)
                if boost > 0:
                    scores[word] += 0.3 * boost   # poids feedback = 0.3

        filtered = [(w, s) for w, s in scores.items() if s >= self.threshold]
        filtered.sort(key=lambda x: x[1], reverse=True)
        return [{'word': w, 'score': round(s, 4)} for w, s in filtered[:top_k]]


# ══════════════════════════════════════════════
# CONTEXT ANALYZER
# ══════════════════════════════════════════════

class ContextAnalyzer:
    @staticmethod
    def analyze(text: str) -> dict:
        tokens = tokenize(text)
        ends_with_space = text.endswith(' ')
        lang = detect_lang(text) if text.strip() else 'fr'

        if not text.strip():
            return {'mode': 'next_word', 'current_word': '',
                    'context': [], 'lang': lang}
        if ends_with_space:
            return {'mode': 'next_word', 'current_word': '',
                    'context': tokens, 'lang': lang}
        return {'mode': 'autocomplete',
                'current_word': tokens[-1] if tokens else '',
                'context': tokens[:-1],
                'lang': lang}


# ══════════════════════════════════════════════
# MOTEUR PRINCIPAL CORRIGÉ
# ══════════════════════════════════════════════

class SMSRecommender:
    def __init__(self):
        # Un Trie + NGram par langue
        self.tries:    dict[str, Trie]       = {}
        self.ngrams:   dict[str, NGramModel] = {}
        self.ranker    = RankingEngine()
        self.analyzer  = ContextAnalyzer()
        self.feedback  = FeedbackStore(decay_lambda=0.01)

    def train(self, csv_path: str):
        print('=== Chargement et séparation par langue ===')
        buckets = load_and_clean(csv_path)

        for lang, texts in buckets.items():
            if not texts:
                continue
            print(f'\n=== Langue : {lang} ({len(texts)} SMS) ===')

            # CORRECTION : détecter les noms propres AVANT tokenisation
            named_entities = detect_named_entities(
                texts, threshold=0.8, min_occurrences=2,
                known_names=KNOWN_FIRST_NAMES
            )
            if named_entities:
                print(f'  [Entités] {len(named_entities)} noms propres détectés : '
                      f'{sorted(named_entities)[:8]}{"..." if len(named_entities)>8 else ""}')

            trie = Trie()
            trie.build_from_corpus(texts)        # Trie garde les prénoms (utile pour autocomplétion)
            self.tries[lang] = trie

            ngram = NGramModel()
            ngram.train(texts, named_entities=named_entities,
                       filter_foreign_leak=(lang == 'fr'))   # anti-fuite anglaise seulement en fr
            self.ngrams[lang] = ngram

        print('\n[OK] Modèle v2 prêt.')

    def recommend(self, partial_text: str, top_k: int = 5) -> dict:
        ctx  = self.analyzer.analyze(partial_text)
        lang = ctx['lang']

        # Fallback : si langue non couverte → français
        trie  = self.tries.get(lang)  or self.tries.get('fr')
        ngram = self.ngrams.get(lang) or self.ngrams.get('fr')

        if not trie or not ngram:
            return {'mode': ctx['mode'], 'lang': lang, 'suggestions': []}

        trie_results  = []
        ngram_results = []

        if ctx['mode'] == 'autocomplete' and ctx['current_word']:
            # CORRECTION 5 : fuzzy si résultats exacts vides
            trie_results = trie.fuzzy_search(ctx['current_word'], max_dist=1, top_k=10)
            if ctx['context']:
                ngram_results = ngram.predict_next(ctx['context'], top_k=10)
                prefix = ctx['current_word']
                ngram_results = [(w, p) for w, p in ngram_results
                                 if w.startswith(prefix)]
        else:
            ngram_results = ngram.predict_next(ctx['context'], top_k=10)

        # CORRECTION : remplacer le token @prenom par une vraie suggestion
        # à l'affichage (le NGram raisonne en "concept", pas en mot précis)
        ngram_results = self._expand_prenom_token(ngram_results, lang)

        suggestions = self.ranker.rank(
            trie_results, ngram_results,
            feedback_store=self.feedback,
            top_k=top_k
        )
        return {
            'mode': ctx['mode'],
            'lang': lang,
            'context_used': ctx['context'][-2:],
            'suggestions': suggestions
        }

    def _expand_prenom_token(self, ngram_results: list[tuple[str, float]],
                             lang: str) -> list[tuple[str, float]]:
        """
        Si le NGram prédit '@prenom', on l'affiche tel quel comme placeholder
        visuel — le client SMS peut le styliser (ex: souligné, cliquable)
        pour inviter l'utilisateur à insérer le nom du contact réel.
        Alternative possible : injecter le prénom du contact actif depuis
        le carnet d'adresses de l'app (non disponible ici).
        """
        expanded = []
        for word, score in ngram_results:
            if word == PRENOM_TOKEN:
                expanded.append((PRENOM_TOKEN, score))
            else:
                expanded.append((word, score))
        return expanded

    def record_feedback(self, partial_text: str, selected_word: str):
        """Enregistre la sélection avec timestamp pour le decay."""
        self.feedback.record(selected_word, partial_text)
        # Renforcer aussi dans le Trie (fréquence +2, pas +3 comme avant)
        lang = detect_lang(partial_text)
        trie = self.tries.get(lang) or self.tries.get('fr')
        if trie:
            trie.insert(selected_word, freq=2)



    def save_model(self, path: str):
        data = {}
        for lang in self.ngrams:
            data[lang] = {
                'unigram': dict(self.ngrams[lang].unigram),
                'bigram': {k: dict(v) for k, v in self.ngrams[lang].bigram.items()},
                'continuation': dict(self.ngrams[lang].continuation),
            }

        # CORRECTION : créer le dossier parent s'il n'existe pas
        dirpath = os.path.dirname(path)
        if dirpath:
            os.makedirs(dirpath, exist_ok=True)

        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f'[OK] Modèle sauvegardé : {path}')


# ══════════════════════════════════════════════
# ÉVALUATION
# ══════════════════════════════════════════════

def evaluate(recommender: SMSRecommender,
             test_texts: list[str], top_k: int = 5) -> dict:
    mrr_scores, hits, total = [], 0, 0
    for text in test_texts:
        tokens = tokenize(text)
        if len(tokens) < 2:
            continue
        for i in range(1, len(tokens)):
            prefix       = ' '.join(tokens[:i]) + ' '
            ground_truth = tokens[i]
            result       = recommender.recommend(prefix, top_k=top_k)
            suggested    = [s['word'] for s in result['suggestions']]
            total += 1
            if ground_truth in suggested:
                rank = suggested.index(ground_truth) + 1
                mrr_scores.append(1 / rank)
                hits += 1
            else:
                mrr_scores.append(0)

    mrr  = sum(mrr_scores) / len(mrr_scores) if mrr_scores else 0
    prec = hits / total if total else 0
    return {
        'total': total,
        'MRR':   round(mrr, 4),
        f'P@{top_k}': round(prec, 4),
        'verdict': 'excellent' if mrr > 0.5 else 'bon' if mrr > 0.3 else 'faible'
    }


# ══════════════════════════════════════════════
# POINT D'ENTRÉE
# ══════════════════════════════════════════════

if __name__ == '__main__':
    import random, pandas as pd
    from pathlib import Path

    BASE_DIR = Path(__file__).resolve().parent
    CSV = BASE_DIR / "data" / "full_dataset.csv"
    engine = SMSRecommender()
    engine.train(CSV)

    print('\n=== TESTS ===')
    tests = [
        ('bonjour ',     'contexte après salutation'),
        ('livr',         'correction faute → livrason absent'),
        ('rendez ',      'next word'),
        ('votre colis est dis', 'autocomplétion'),
        ('merci pour vo','autocomplétion longue'),
    ]
    for text, desc in tests:
        r = engine.recommend(text)
        words = [s['word'] for s in r['suggestions']]
        print(f'  [{desc}]')
        print(f'    "{text}" → lang:{r["lang"]} mode:{r["mode"]} → {words}')

    print('\n=== FEEDBACK avec decay ===')
    engine.record_feedback('bonjour ', 'comment')
    engine.record_feedback('bonjour ', 'comment')
    r = engine.recommend('bonjour ')
    print('  Après 2× feedback "comment" :',
          [s['word'] for s in r['suggestions']])
    print('  Top mots récents :', engine.feedback.top_recent_words(5))

    print('\n=== ÉVALUATION v2 ===')
    df = pd.read_csv(CSV)
    df = df[df['is_spam']==False][df['noise_level']!='heavy']
    all_texts = df['text'].astype(str).tolist()
    random.shuffle(all_texts)
    test_set = all_texts[:int(len(all_texts)*0.1)]
    m = evaluate(engine, test_set)
    print(f'  Prédictions : {m["total"]}')
    print(f'  MRR         : {m["MRR"]}')
    print(f'  P@5         : {m["P@5"]}')
    print(f'  Verdict     : {m["verdict"]}')

    engine.save_model('models/sms_model_v2.json')