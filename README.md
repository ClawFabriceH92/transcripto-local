# Transcripto Local

**Application Android 100% locale** d'enregistrement vocal, transcription et analyse par IA — conçue pour les professionnels soumis au **secret professionnel** (CAC, audit, conseil).

Aucune donnée audio, transcription ou analyse **ne quitte l'appareil**. Zéro serveur, zéro cloud, zéro télémétrie.

---

## Fonctionnalités

- 🎙️ **Enregistrement audio** (format AAC) avec pause/reprise, gestion des longs enregistrements, indicateur de niveau sonore
- 📝 **Transcription Speech-to-Text** via **whisper.cpp** — français prioritaire, horodatage des segments
- 🧠 **Analyse LLM** via **llama.cpp** — résumé, points clés, actions à suivre, mode question/réponse libre
- 🔒 **Sécurité** — chiffrement AES/GCM via Android Keystore, suppression sécurisée, verrouillage PIN/biométrie
- 📊 **Profil automatique** — sélection du couple de modèles (STT + LLM) adapté à la RAM disponible
- 📁 **Export local** des résultats en texte brut

## Confidentialité

| Fonctionnalité | Comportement |
|---|---|
| Réseau | **Aucune permission Internet** en usage courant |
| Données | Tout reste sur l'appareil (chiffré au repos) |
| Télémétrie | Aucun tracker, aucun SDK, aucune analytics |
| Modèles | Téléchargement unique et vérifié (hash SHA-256) |

## Profils matériels

| RAM | Modèle STT | Taille | Modèle LLM | Taille |
|---|---|---|---|---|
| < 4 Go | Whisper Tiny | ~75 Mo | Qwen2.5 1.5B | ~1,0 Go |
| 4-6 Go | Whisper Base | ~142 Mo | Llama 3.2 3B | ~2,0 Go |
| 6-8 Go | Whisper Small | ~466 Mo | Phi-4 Mini 3.8B | ~2,7 Go |
| 8-12 Go | Whisper Small | ~466 Mo | Mistral 7B | ~4,4 Go |
| > 12 Go | Whisper Medium | ~1,5 Go | Qwen2.5 7B | ~5,5 Go |

*Marge système ~1,5–2 Go réservée avant attribution du profil.*

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ Enregistrement│────▶│ whisper.cpp  │────▶│  llama.cpp  │
│ (MediaRecorder)│    │    (STT)     │     │   (LLM)     │
└─────────────┘     └──────────────┘     └─────────────┘
       │                    │                    │
       ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────┐
│              Stockage chiffré (Keystore)             │
│         + Room (SQLite) — métadonnées               │
└─────────────────────────────────────────────────────┘
```

### Composants techniques

- **Kotlin** + **Jetpack Compose** (UI)
- **whisper.cpp** via JNI/NDK (transcription)
- **llama.cpp** via JNI/NDK (analyse LLM)
- **Room** (métadonnées et cache)
- **Android Keystore** + **security-crypto** (chiffrement AES/GCM)
- **DataStore Preferences** (paramètres)
- **Accélération GPU** via Vulkan (repli CPU)

## Prérequis

- Android Studio Ladybug (2024.3) ou plus récent
- Android SDK (API 29-35)
- NDK 27+ (pour whisper.cpp / llama.cpp)
- Gradle 8.9
- Kotlin 2.0.21

## Build

```bash
git clone https://github.com/ClawFabriceH92/transcripto-local.git
cd transcripto-local
# Compiler les bibliothèques natives (voir docs/native-build.md)
./gradlew assembleDebug
```

L'APK signé se trouve dans `app/build/outputs/apk/debug/`.

## Structure du projet

```
transcripto-local/
├── app/
│   ├── src/main/
│   │   ├── java/com/transcripto/local/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/              # Écrans Compose
│   │   │   ├── audio/           # Enregistrement
│   │   │   ├── stt/             # Moteur STT (whisper.cpp)
│   │   │   ├── llm/             # Moteur LLM (llama.cpp)
│   │   │   ├── models/          # Gestionnaire de modèles
│   │   │   ├── security/        # Chiffrement, verrouillage
│   │   │   ├── db/              # Room database
│   │   │   └── export/          # Export texte
│   │   ├── res/                 # Ressources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── scripts/                     # Scripts de build natifs
├── docs/                        # Documentation
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Roadmap

| Phase | Contenu | Durée |
|---|---|---|
| 1 — Socle | Intégration whisper.cpp + llama.cpp, détection RAM | 2-3 sem. |
| 2 — Pipeline | Enregistrement, STT, édition transcription | 1-2 sem. |
| 3 — Analyse | Prompts LLM, résumé, extraction, Q/R | 2 sem. |
| 4 — Sécurité | Chiffrement, suppression, verrouillage | 1 sem. |
| 5 — Tests | Multi-appareils, optimisation, profils RAM | 2 sem. |

## Licence

Ce projet est sous licence **GNU Affero General Public License v3.0 (AGPL-3.0)**  
— garantissant que toute modification ou utilisation via réseau reste libre.

---

*Pour les CAC, auditeurs et professionnels qui traitent des données confidentielles.*
