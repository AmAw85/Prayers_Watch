# DEVLOG.md — Journal technique

## 2026-10-26 22:52 — Simplification métier et Shurūq
- **Travail :** suppression des temps optionnels/nécessaires du modèle, du moteur, de l'UI, des réglages et des chaînes ; ajout de Shurūq (Fajr→Shurūq, chaque prière jusqu'à la suivante, Isha→Fajr du lendemain) ; mise à jour accueil, calendrier, horloge et couleurs.
- **Fichiers :** `core/PrayerDay.java`, `core/TimeEngine.java`, `core/ScheduleValidator.java`, `data/AlAdhanParser.java`, `data/PrayerDatabase.java`, `data/PrayerMerge.java`, `settings/ColorSlot.java`, `ui/MainActivity.java`, `ui/CalendarActivity.java`, `ui/PrayerClockView.java`, `ui/SettingsActivity.java`, `res/values*/strings.xml`.
- **Vérification :** `testDebugUnitTest` réussi (7 tests, 0 échec) ; `assembleDebug` réussi à 22:52 CET. Migration de base version 2 avec colonne `sunrise_epoch` non destructive.
- **Limite :** comportement réel de l'UI non testé sur appareil/émulateur.
- **Suite :** validation sur appareil/émulateur, puis commit explicite si demandé.

## 2026-10-26 — Socle V1
- **Travail :** création du projet Android, modèles, moteur temporel, parseur AlAdhan, SQLite, synchronisation, préférences, localisation, horloge à trois anneaux, réglages et calendrier.
- **Fichiers principaux :** `app/src/main/java/com/horloge/islamique/**`, `app/src/main/res/**`, `app/build.gradle`.
- **Vérification :** `assembleDebug` réussi ; APK `app/build/outputs/apk/debug/app-debug.apk`.
- **Limite :** aucun test JVM ni test d'exécution. UI non ouverte.
- **Suite :** vérifier la source réseau et préparer les tests.

## 2026-10-26 — Vérification AlAdhan
- **Travail :** appel réel de l'endpoint AlAdhan pour Alger avec `method=19` et `timezonestring=Africa/Algiers`.
- **Vérification :** réponse 200 conservée dans `app/src/test/resources/aladhan-algiers-2026-10-26.json`.
- **Décision :** source classée calculée, jamais officielle ; aucune API officielle algérienne inventée.
- **Suite :** parser la fixture dans un test JVM.

## 2026-10-26 22:21 — Indicateur de ville/fuseau incohérent
- **Travail :** ajout de `PrayerRepository.hasMismatchedStoredData()` et affichage de `status_city_mismatch` dans l'accueil et les réglages.
- **Fichiers :** `data/PrayerRepository.java`, `ui/MainActivity.java`, `ui/SettingsActivity.java`.
- **Vérification :** `assembleDebug` réussi à 22:26 CET.
- **Limite :** comportement non testé à l'exécution.
- **Suite :** tests de préférences et de cache.

## 2026-10-26 22:30 — Infrastructure de test
- **Travail :** ajout de `MemoryKeyValueStore`, fixture AlAdhan et dépendances JUnit/org.json.
- **Fichiers :** `app/src/test/java/com/horloge/islamique/settings/MemoryKeyValueStore.java`, `app/src/test/resources/aladhan-algiers-2026-10-26.json`, `app/build.gradle`.
- **Vérification :** `compileDebugUnitTestJavaWithJavac` réussi à 22:30 CET.
- **Limite :** aucun `@Test` réel ; la suite n'a pas été exécutée.
- **Suite :** écrire `TEST-02` (préférences).

## 2026-10-26 22:31 — Mémoire de développement
- **Travail :** création de `AGENTS.md`, `PROJECT_STATE.md`, `TASKS.md`, `DECISIONS.md`, `DEVLOG.md`.
- **Vérification :** état recoupé avec le code, le dernier build et l'absence de tests.
- **Limite :** aucun émulateur/appareil disponible.
- **Suite :** `TEST-02`.

## 2026-10-26 22:34 — Initialisation Git et publication
- **Travail :** `git init -b main`, identité locale, remote SSH `git@github.com:AmAw85/Prayers_Watch.git`.
- **Vérification :** SSH GitHub authentifié pour `AmAw85`; dépôt distant accessible et vide. Aucun fichier ignoré (`local.properties`, builds) n'est destiné au commit.
- **Limite :** commit et push initial encore à effectuer.
- **Suite :** commit de l'état vérifié, push, puis contrôle `git ls-remote`.

## 2026-10-26 22:36 — Publication GitHub
- **Travail :** commit initial `9382fb6` (72 fichiers), push de `main` vers `origin`.
- **Vérification :** `git push -u origin main` réussi ; `git ls-remote origin` renvoie `9382fb6` pour `HEAD` et `refs/heads/main`.
- **État :** dépôt distant à jour. Aucun test supplémentaire exécuté.
- **Suite :** `TEST-02`.
