# PROJECT_STATE.md — État du projet

**Dernière mise à jour :** 2026-10-26 22:52 CET  
**Confiance :** élevée pour le code, le build et les tests JVM ; moyenne pour l'état fonctionnel de l'UI, non testée à l'exécution.

## Objectif
Application Android « Horloge des prières » : horloge analogique 12 h, anneaux de prière, dates grégorienne et hégirienne, thème clair/sombre, arabe par défaut avec français et anglais, source de données configurable, stockage hors ligne, corrections utilisateur et calendrier.

## État réel synthétique
- Projet Android natif Java (sans AndroidX/Compose), minSdk 26, target/compileSdk 35.
- Les trois écrans existent : accueil, réglages, calendrier.
- Le code V1 est largement implémenté : moteur temporel, parseur, SQLite, synchronisation, corrections, localisation, thèmes, anneaux et calendrier.
- `assembleDebug` réussit sur l'état courant et produit `app/build/outputs/apk/debug/app-debug.apk`.
- **Règle métier implémentée :** Fajr→Shurūq, chaque prière jusqu'à la suivante, Isha→Fajr du lendemain. Shurūq est exposé comme événement solaire distinct.
- **Les temps optionnels/nécessaires et leur UI sont supprimés** du modèle, du moteur, des réglages et des chaînes.
- **Infrastructure de tests JVM** présente et exécutée avec succès.
- **Aucun test d'exécution sur appareil ou émulateur n'a été effectué.**
- **Dépôt Git** sur `main` avec `origin/main` connu. Aucun commit automatique n'a été créé ici.

## Terminé et vérifié
- `SETUP-01` : squelette Gradle/Android, ressources, manifeste et icônes compilent.
- `API-01` : endpoint AlAdhan réel vérifié pour Alger le 2026-10-26 avec `method=19`.
- `DOC-01` : fichiers de suivi créés.
- `TEST-01` : infrastructure de test JVM compilable.
- Migration de la base vers la version 2 avec colonne `sunrise_epoch` non destructive (colonnes legacy préservées, non lues/écrites).

## Partiel — code présent, validation insuffisante
- Horloge analogique 12 h, secteurs AM/PM, légende, surbrillance et compte à rebours : compilent, jamais ouverts sur un appareil.
- Localisation arabe RTL / français / anglais : ressources complètes, rendu non vérifié.
- Couleurs personnalisables et réinitialisation : logique et écrans présents, non testés.
- Synchronisation AlAdhan, cache SQLite, états frais/périmé/indisponible/non vérifié : compilent, non testés.
- Corrections : valeurs source et corrigées séparées, survie au resync prévue, non testée.
- Changement de ville/fuseau : données d'une autre localisation masquées et message de désaccord ajouté, non testé à l'exécution.
- Calendrier grégorien/hégirien : grille et détail présents, non testés.

## Non commencé
- Tests d'instrumentation ou smoke test sur appareil/émulateur.
- Wrapper Gradle.
- Recherche d'une éventuelle API officielle algérienne réellement documentée.

## Dernière tâche réalisée
- Simplification métier : suppression des temps optionnels/nécessaires, ajout de Shurūq, intervalles de prière dérivés des horaires source réels, mise à jour UI et tests.

## Dernière vérification réussie
```sh
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 ANDROID_HOME=/home/ammi/Android/Sdk /home/ammi/.local/opt/gradle/bin/gradle :app:testDebugUnitTest
```
Résultat : `BUILD SUCCESSFUL`, 7 tests passés, 0 échec (2026-10-26 22:52 CET).
Déployé/compilé aussi : `gradle :app:assembleDebug` -> `BUILD SUCCESSFUL`. 

## Blocages et limitations connues
- **Git :** dépôt synchronisé sur `origin/main`; aucune validation de cette évolution ne doit être poussée sans autorisation explicite.
- **Exécution :** aucun appareil connecté, aucun émulateur ni image système installés. Le comportement réel de l'UI reste inconnu.
- **Source :** AlAdhan est une source **calculée**, jamais officielle. Aucune API publique officielle algérienne documentée n'a été trouvée ; ne pas en inventer.
- **Limite API :** aucune limite de débit explicite trouvée dans la documentation OpenAPI consultée. L'application reste prudente (3 jours par synchronisation, auto-sync espacée).
- **Créneaux religieux :** seuls les intervalles dérivés des données source réelles sont affichés.
- **Build :** Gradle système 8.11.1 est utilisé ; le wrapper n'existe pas encore. `JAVA_HOME` doit pointer vers Temurin 17.

## Fichiers importants
- `AGENTS.md`, `TASKS.md`, `DECISIONS.md`, `DEVLOG.md` : mémoire de développement.
- `app/build.gradle`, `settings.gradle`, `AndroidManifest.xml` : configuration.
- `core/TimeEngine.java` : logique temporelle pure.
- `data/AlAdhanClient.java`, `data/AlAdhanParser.java` : source réseau et validation.
- `data/PrayerRepository.java`, `data/PrayerDatabase.java`, `data/PrayerMerge.java` : cache, corrections et fusion.
- `settings/SettingsRepository.java` : préférences et valeurs par défaut.
- `ui/MainActivity.java`, `ui/SettingsActivity.java`, `ui/CalendarActivity.java` : écrans.
- `app/src/test/java/com/horloge/islamique/core/TimeEnginePrayerIntervalsTest.java` : tests des intervalles.
- `app/src/test/java/com/horloge/islamique/data/AlAdhanParserSunriseTest.java`, `app/src/test/java/com/horloge/islamique/data/PrayerMergeTest.java` : parseur et fusion.
- `app/src/test/resources/aladhan-algiers-2026-10-26.json` : fixture réelle.

## Prochaine action recommandée
**▶ Valider sur appareil/émulateur** : exécuter l'APK, vérifier les écrans et l'affichage du Shurūq, la légende et les intervalles. Puis repousser les changements source avec un commit explicite si demandé.
