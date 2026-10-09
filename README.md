# Smart Timing Engine — Optimisation intelligente des campagnes SMS

**Projet de fin d’études (PFE) | Génie Logiciel | EasyBulk**

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Backend-6DB33F)](https://spring.io/projects/spring-boot) [![Python](https://img.shields.io/badge/Python-Machine%20Learning-3776AB)](https://www.python.org/) [![Angular](https://img.shields.io/badge/Angular-Frontend-DD0031)](https://angular.dev/) [![MongoDB](https://img.shields.io/badge/MongoDB-Database-47A248)](https://www.mongodb.com/) [![Docker](https://img.shields.io/badge/Docker-Containerization-2496ED)](https://www.docker.com/)

## 1. Présentation du projet

**Smart Timing Engine** est une solution d’intelligence artificielle conçue pour enrichir EasyBulk, une plateforme de gestion et d’envoi de campagnes SMS en masse.

Le projet vise à améliorer l’efficacité des campagnes de communication en exploitant les données disponibles, les caractéristiques des campagnes et les informations historiques afin de produire des recommandations pertinentes.

La solution repose sur deux microservices complémentaires :

* **AI Timing Service :** analyse les données disponibles et estime les créneaux horaires les plus pertinents pour l’envoi des campagnes SMS.
* **SMS Recommender Service :** facilite la rédaction des messages en proposant des complétions de mots et des suggestions de mots suivants selon le contexte textuel.

L’un des principaux défis consiste à intégrer ces fonctionnalités d’intelligence artificielle dans une architecture applicative existante, sans remettre en cause les fonctionnalités métier déjà disponibles.

## 2. Objectifs

* Optimiser la planification des campagnes SMS à l’aide du Machine Learning.
* Exploiter les données historiques et les caractéristiques des campagnes pour calculer des scores de pertinence temporelle.
* Développer un système de recommandation textuelle adapté à la rédaction des SMS.
* Exposer les fonctionnalités d’IA à travers des API REST.
* Assurer l’intégration avec les services backend et le frontend existants.
* Privilégier une architecture modulaire, maintenable et évolutive.
* Évaluer les modèles à l’aide de données disponibles et de scénarios de simulation lorsque les données réelles d’engagement sont limitées.

## 3. Fonctionnalités principales

### 3.1. AI Timing Service — Prédiction du meilleur moment d’envoi

Ce microservice exploite les techniques de Machine Learning pour estimer les créneaux horaires susceptibles de favoriser l’engagement des destinataires.

**Principales fonctionnalités :**

* Préparation et prétraitement des données.
* Extraction et sélection de caractéristiques pertinentes.
* Exploitation de variables temporelles et de caractéristiques liées aux campagnes.
* Entraînement et évaluation de modèles prédictifs.
* Calcul de scores associés aux créneaux horaires.
* Génération de recommandations temporelles.
* Exposition des prédictions via des endpoints REST, notamment pour le traitement individuel et par lots.

Le pipeline de traitement suit les étapes suivantes :

**Données → Prétraitement → Extraction des caractéristiques → Modélisation → Scores temporels → Recommandation**

L’objectif est de fournir une aide à la décision pour planifier les campagnes plutôt que de choisir arbitrairement une heure d’envoi.

### 3.2. SMS Recommender Service — Assistance intelligente à la rédaction

Ce microservice vise à accélérer la rédaction des SMS grâce à des mécanismes de prédiction et de recommandation textuelle.

**Principales fonctionnalités :**

* Complétion de mots à partir du texte saisi.
* Suggestion de mots suivants en fonction du contexte.
* Exploitation de structures de type Trie pour la recherche de complétions.
* Utilisation de modèles statistiques N-gram pour estimer les continuations textuelles.
* Classement des suggestions selon leur pertinence.
* Prise en charge de contenus en français, en arabe et en derja tunisienne selon les données et les règles disponibles.
* Communication avec le backend via une interface dédiée.

Le service fournit des suggestions à l’utilisateur, qui conserve le contrôle sur le contenu final du message.

## 4. Architecture technique

Le projet s’inscrit dans une architecture microservices séparant les responsabilités métier, les services applicatifs et les composants d’intelligence artificielle.

### Technologies principales

| Composant                 | Technologies                                      |
| ------------------------- | ------------------------------------------------- |
| Frontend                  | Angular                                           |
| Backend                   | Java, Spring Boot, Spring Data                    |
| Intelligence artificielle | Python, Scikit-learn, modèles de Machine Learning |
| Recommandation textuelle  | Trie, N-gram, traitement du langage naturel       |
| Base de données           | MongoDB                                           |
| Cache                     | Redis                                             |
| Communication             | API REST                                          |
| Routage des requêtes      | API Gateway                                       |
| Conteneurisation          | Docker, Docker Compose                            |
| Gestion de versions       | Git, GitHub                                       |

### Organisation logique

Le frontend Angular communique avec les services backend à travers les interfaces applicatives prévues par EasyBulk. L’API Gateway assure le routage des requêtes, tandis que les microservices d’IA fournissent les fonctionnalités de prédiction et de recommandation.

Cette séparation permet de faire évoluer les modèles d’intelligence artificielle indépendamment des principales fonctionnalités métier.

## 5. Technologies et compétences mobilisées

### Développement backend

* Conception de microservices avec Spring Boot.
* Développement et intégration d’API REST.
* Organisation des responsabilités entre les composants applicatifs.
* Communication entre services et intégration dans une architecture existante.

### Intelligence artificielle et traitement des données

* Préparation et transformation des données.
* Feature engineering.
* Entraînement et évaluation de modèles de Machine Learning.
* Calcul de scores prédictifs et génération de recommandations.
* Exploitation de techniques de recommandation textuelle.

### Intégration et infrastructure

* Intégration de services Python dans un environnement applicatif Java.
* Gestion des échanges entre composants.
* Utilisation de MongoDB et Redis.
* Conteneurisation des services avec Docker.
* Gestion du code source et des évolutions avec Git.

## 6. Défis techniques

Le projet aborde plusieurs problématiques d’ingénierie :

* **Intégration :** ajouter des capacités d’IA à une plateforme existante sans perturber ses fonctionnalités.
* **Qualité des données :** exploiter les données disponibles tout en tenant compte des limites des indicateurs d’engagement.
* **Pertinence des recommandations :** transformer les résultats des modèles en recommandations exploitables.
* **Interopérabilité :** faire communiquer des composants développés avec différentes technologies.
* **Maintenabilité :** isoler les fonctionnalités d’IA dans des services indépendants.
* **Évaluation :** utiliser des métriques et des scénarios adaptés pour mesurer les performances des modèles.

## 7. Valeur ajoutée

Ce travail combine le développement logiciel et l’intelligence artificielle dans un contexte applicatif concret.

Il couvre plusieurs dimensions complémentaires : préparation des données, conception de modèles prédictifs, développement de services dédiés, exposition d’API et intégration dans une architecture microservices.

La valeur du projet réside ainsi dans le passage de modèles d’IA à des fonctionnalités intégrables dans une plateforme existante, avec une attention portée à la modularité, à la maintenabilité et aux contraintes opérationnelles.

## 8. Contexte du projet

* **Type :** Projet de fin d’études (PFE).
* **Domaine :** Génie Logiciel et Intelligence Artificielle.
* **Application cible :** EasyBulk.
* **Axes techniques :** Machine Learning, NLP, développement backend et architecture microservices.

## 9. Dépôt du projet

**Code source :** [github.com/ahmedsghaier/pfe_sms](https://github.com/ahmedsghaier/pfe_sms)

