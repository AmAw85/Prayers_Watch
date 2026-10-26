# TASKS.md — Backlog

**Dernière mise à jour :** 2026-10-26 22:36 CET  
**Prochaine tâche : `TEST-02`**  
Légende : une tâche n'est `DONE` qu'après vérification de son critère d'acceptation.

## DONE

### SETUP-01 — Squelette Android compilable
- **Objectif :** projet Gradle Android natif Java, manifeste, thèmes, ressources et icônes.
- **Dépendances :** aucune.
- **Critère :** `gradle assembleDebug` réussit et produit un APK.
- **Vérification :** `BUILD SUCCESSFUL`, 2026-10-26 22:26 CET, APK dans `app/build/outputs/apk/debug/`.

### API-01 — Vérifier la source réelle
- **Objectif :** confirmer l'endpoint AlAdhan, la méthode 19 (Algérie) et le format de réponse.
- **Dépendances :** accès réseau.
- **Critère :** appel réel réussi et fixture conservée.
- **Vérification :** appel du 2026-10-26 pour Alger, fixture `app/src/test/resources/aladhan-algiers-2026-10-26.json`. Source classée **calculée**, non officielle.

### TEST-01 — Préparer l'infrastructure de test JVM
- **Objectif :** source set de test compilable, faux stockage de préférences et fixture réseau réutilisable.
- **Dépendances :** aucune.
- **Critère :** `compileDebugUnitTestJavaWithJavac` réussit.
- **Vérification :** `BUILD SUCCESSFUL`, 2026-10-26 22:30 CET. Aucun `@Test` réel n'existe encore.

### DOC-01 — Mémoire de développement persistante
- **Objectif :** `AGENTS.md`, `PROJECT_STATE.md`, `TASKS.md`, `DECISIONS.md`, `DEVLOG.md`.
- **Dépendances :** inspection du code et du build réel.
- **Critère :** les cinq fichiers existent, sont cohérents et ne déclarent pas de test non exécuté.
- **Vérification :** relecture croisée après création ; push initial `9382fb6` sur `origin/main`.

### GIT-01 — Initialiser le suivi Git et publier
- **Objectif :** créer le dépôt, un commit de référence et pousser sur GitHub sans écraser de travail.
- **Dépendances :** autorisation utilisateur (accordée).
- **Critère :** `git status` fonctionne et le dépôt distant contient la branche `main`.
- **Vérification :** commit `9382fb6`, `git push -u origin main`, puis `git ls-remote origin` renvoie la même référence.

## IN_PROGRESS

Aucune tâche en cours. La prochaine unité de développement est `TEST-02`.

## TODO

### TEST-02 — Tester les préférences et valeurs par défaut  ← PROCHAINE
- **Objectif :** couvrir langue arabe par défaut, persistance, couleurs par défaut, personnalisation, reset, validation du fuseau, fournisseur et ville.
- **Dépendances :** `TEST-01`.
- **Critère :** tests JUnit ciblés dans `settings`, exécutés et verts.
- **Vérification :** `gradle testDebugUnitTest --tests '*SettingsRepositoryTest'`.

### TEST-03 — Tester le moteur temporel
- **Objectif :** couvrir prière courante/suivante, minuit, compte à rebours, secteurs AM/PM, fuseaux avec heure inexistante ou ambiguë.
- **Dépendances :** `TEST-01`.
- **Critère :** cas limites testés, dont passage de minuit et intervalles semi-ouverts.
- **Vérification :** `gradle testDebugUnitTest --tests '*TimeEngineTest'`.

### TEST-04 — Tester validation et parseur
- **Objectif :** couvrir horaires manquants, doublons, ordre invalide, chevauchement, fusion, parseur de la fixture, mauvaise date, mauvais fuseau et JSON invalide.
- **Dépendances :** `TEST-01`, fixture.
- **Critère :** les erreurs produisent le bon code métier sans acceptation silencieuse.
- **Vérification :** `gradle testDebugUnitTest --tests '*ScheduleValidatorTest' --tests '*AlAdhanParserTest'`.

### TEST-05 — Tester cache, resync et corrections
- **Objectif :** prouver la conservation des corrections au resync, la séparation source/corrigé, le repli hors ligne et le masquage lors d'un changement de ville/fuseau.
- **Dépendances :** `TEST-01`.
- **Critère :** un échec réseau conserve les données ; une correction survit à une nouvelle synchronisation.
- **Vérification :** `gradle testDebugUnitTest --tests '*PrayerRepositoryTest' --tests '*PrayerMergeTest'`.

### TEST-06 — Exécuter toute la suite
- **Objectif :** exécuter la suite JVM complète et consigner le nombre réel de tests.
- **Dépendances :** `TEST-02` à `TEST-05`.
- **Critère :** `BUILD SUCCESSFUL`, zéro échec, nombre de tests > 0.
- **Vérification :** `gradle testDebugUnitTest` et rapport `app/build/reports/tests/testDebugUnitTest/`.

### BUILD-01 — Générer le wrapper Gradle
- **Objectif :** rendre le build reproductible avec `./gradlew`.
- **Dépendances :** aucune.
- **Critère :** `./gradlew assembleDebug` réussit sans variable Gradle système.
- **Vérification :** `gradle wrapper --gradle-version 8.11.1`, puis `./gradlew assembleDebug`.

### DOC-02 — Rédiger le README
- **Objectif :** commandes de build/test, architecture, source AlAdhan calculée, absence d'API officielle documentée, limites et fonctionnalités partielles.
- **Dépendances :** `TEST-06`, `BUILD-01`.
- **Critère :** aucune affirmation non vérifiée ; commandes réellement exécutables.
- **Vérification :** relecture et exécution au moins du build documenté.

### FEAT-01 — Définir l'import de créneaux explicites
- **Objectif :** format d'import documenté (source, fuseau, début/fin, provenance) pour les temps optionnels/nécessaires.
- **Dépendances :** aucune.
- **Critère :** parseur pur, validation des chevauchements et tests ; aucune inférence depuis les cinq prières.
- **Vérification :** tests JVM dédiés.

### FEAT-02 — Ajouter l'interface d'import
- **Objectif :** importer, prévisualiser et confirmer les créneaux ; persister et afficher leur provenance.
- **Dépendances :** `FEAT-01`, `TEST-06`.
- **Critère :** import explicite seulement, erreur claire si non configuré, corrections et resync préservés.
- **Vérification :** tests JVM + smoke test UI.

### QA-02 — Vérifier l'interface réelle
- **Objectif :** vérifier accueil, réglages, calendrier, RTL/LTR, thèmes, insets système et lisibilité des anneaux.
- **Dépendances :** `QA-01`.
- **Critère :** aucun chevauchement système, aucun crash, parcours principaux fonctionnels.
- **Vérification :** captures et scénario manuel sur appareil/émulateur.

### DATA-01 — Rechercher une source officielle algérienne
- **Objectif :** vérifier l'existence d'une API publique officielle réellement documentée.
- **Dépendances :** aucune.
- **Critère :** conclusion sourcée ; si aucune API, conserver AlAdhan clairement étiqueté calculé.
- **Vérification :** documentation/URL officielle ajoutée au README ; aucune source inventée.

## BLOCKED

### QA-01 — Smoke test sur appareil ou émulateur
- **Objectif :** installer l'APK et exécuter les parcours principaux.
- **Raison :** aucun appareil connecté, aucun émulateur ni image système installés.
- **Déblocage :** connecter un appareil ou installer un émulateur/AVD.
- **Critère :** APK lancé, écrans ouverts, aucune erreur fatale.
