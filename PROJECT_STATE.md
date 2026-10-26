# PROJECT_STATE.md — État du projet

**Dernière mise à jour :** 2026-10-26 22:36 CET  
**Confiance :** élevée pour le code et le build ; moyenne pour l'état fonctionnel de l'UI, non testée à l'exécution ; aucune suite de tests exécutée.

## Objectif
Application Android « Horloge des prières » : horloge analogique 12 h, anneaux de prière, dates grégorienne et hégirienne, thème clair/sombre, arabe par défaut avec français et anglais, source de données configurable, stockage hors ligne, corrections utilisateur et calendrier.

## État réel synthétique
- Projet Android natif Java (sans AndroidX/Compose), minSdk 26, target/compileSdk 35.
- Les trois écrans existent : accueil, réglages, calendrier.
- Le code V1 est largement implémenté : moteur temporel, parseur, SQLite, synchronisation, corrections, localisation, thèmes, anneaux et calendrier.
- `assembleDebug` réussit sur l'état courant et produit `app/build/outputs/apk/debug/app-debug.apk`.
- **Aucun test JVM avec `@Test` n'existe encore.** Seul un utilitaire de test et une fixture JSON ont été ajoutés.
- **Aucun test d'exécution sur appareil ou émulateur n'a été effectué.**
- **Dépôt Git publié** sur `main` : `git@github.com:AmAw85/Prayers_Watch.git`, commit initial `9382fb6`. Les artefacts et `local.properties` sont ignorés.
- Il n'existe ni wrapper Gradle, ni `README.md`.

## Terminé et vérifié
- `SETUP-01` : squelette Gradle/Android, ressources, manifeste et icônes compilent.
- `API-01` : endpoint AlAdhan réel vérifié pour Alger le 2026-10-26 avec `method=19` ; réponse conservée dans `app/src/test/resources/aladhan-algiers-2026-10-26.json`.
- `DOC-01` : fichiers de suivi créés et cohérents.

## Partiel — code présent, validation insuffisante
- Horloge analogique 12 h, secteurs AM/PM, légende, surbrillance et compte à rebours : compilent, jamais ouverts sur un appareil.
- Anneaux optionnel et nécessaire : affichage « non configuré » prévu ; aucune interface d'import de créneaux explicites.
- Localisation arabe RTL / français / anglais : ressources complètes, rendu non vérifié.
- Couleurs personnalisables et réinitialisation : logique et écrans présents, non testés.
- Synchronisation AlAdhan, cache SQLite, états frais/périmé/indisponible/non vérifié : compilent, non testés.
- Corrections : valeurs source et corrigées séparées, survie au resync prévue, non testée.
- Changement de ville/fuseau : données d'une autre localisation masquées et message de désaccord ajouté, non testé à l'exécution.
- Calendrier grégorien/hégirien : grille et détail présents, non testés.

## Non commencé
- Tests JVM réels et exécution de `testDebugUnitTest`.
- Tests d'instrumentation ou smoke test sur appareil/émulateur.
- Wrapper Gradle.
- `README.md` (build, tests, limites de la source, fonctionnalités partielles).
- Import de créneaux optionnels/nécessaires depuis une source explicite.
- Recherche d'une éventuelle API officielle algérienne réellement documentée.

## Dernière tâche réalisée
- `DOC-01` et `GIT-01` : mémoire de développement créée, dépôt initialisé et poussé sur GitHub. `UI-04` (message ville/fuseau) reste compilé mais non validé à l'exécution.

## Dernière vérification réussie
```sh
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 \
ANDROID_HOME=/home/ammi/Android/Sdk \
/home/ammi/.local/opt/gradle/bin/gradle assembleDebug
```
Résultat : `BUILD SUCCESSFUL` le 2026-10-26 à 22:26 CET, APK généré.  
La compilation des sources de test (`compileDebugUnitTestJavaWithJavac`) réussit à 22:30 CET.  
Le push initial sur `origin/main` réussit (commit `9382fb6`) et `git ls-remote origin` renvoie la même référence.  
Aucun test unitaire avec `@Test` ni test d'interface n'a été exécuté.

## Blocages et limitations connues
- **Git :** dépôt synchronisé sur `origin/main`; commit initial `9382fb6`. Les prochaines modifications doivent être commitées puis poussées, sans commit automatique non demandé.
- **Exécution :** aucun appareil connecté, aucun émulateur ni image système installés. Le comportement réel de l'UI reste inconnu.
- **Source :** AlAdhan est une source **calculée**, jamais officielle. Aucune API publique officielle algérienne documentée n'a été trouvée ; ne pas en inventer.
- **Limite API :** aucune limite de débit explicite trouvée dans la documentation OpenAPI consultée. L'application reste prudente (3 jours par synchronisation, auto-sync espacée).
- **Créneaux religieux :** les temps optionnels/nécessaires ne doivent jamais être déduits des horaires de prière. Sans import explicite, l'UI doit rester sur « non configuré ».
- **Build :** Gradle système 8.11.1 est utilisé ; le wrapper n'existe pas encore. `JAVA_HOME` doit pointer vers Temurin 17.

## Fichiers importants
- `AGENTS.md`, `TASKS.md`, `DECISIONS.md`, `DEVLOG.md` : mémoire de développement.
- `app/build.gradle`, `settings.gradle`, `AndroidManifest.xml` : configuration.
- `core/TimeEngine.java` : logique temporelle pure.
- `data/AlAdhanClient.java`, `data/AlAdhanParser.java` : source réseau et validation.
- `data/PrayerRepository.java`, `data/PrayerDatabase.java`, `data/PrayerMerge.java` : cache, corrections et fusion.
- `settings/SettingsRepository.java` : préférences et valeurs par défaut.
- `ui/MainActivity.java`, `ui/SettingsActivity.java`, `ui/CalendarActivity.java` : écrans.
- `app/src/test/resources/aladhan-algiers-2026-10-26.json` : fixture réelle.

## Prochaine action recommandée
**▶ Commencer `TEST-02`** : écrire les tests JVM des préférences, puis enchaîner `TEST-03` à `TEST-06` et exécuter la suite avant toute nouvelle évolution fonctionnelle.
