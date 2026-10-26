# DECISIONS.md — Décisions structurantes

**Dernière mise à jour :** 2026-10-26  
Les décisions ci-dessous sont validées sauf mention contraire. Toute modification doit être justifiée et répercutée dans `PROJECT_STATE.md` et `DEVLOG.md`.

## D-001 — Android natif Java, sans AndroidX/Compose
- **Décision :** Java 17, `minSdk 26`, `compileSdk/targetSdk 35`, widgets Android natifs.
- **Motif :** application légère, dépendances minimales, pas de chaîne Compose/AndroidX.
- **Conséquences :** Material Components indisponible ; thèmes, insets et composants gérés manuellement. `JAVA_HOME` doit pointer vers Temurin 17.

## D-002 — Moteur temporel pur et indépendant
- **Décision :** `core/TimeEngine` ne dépend ni de l'UI ni du réseau ; il manipule des `Instant` et des `ZoneId` IANA.
- **Motif :** rendre les calculs testables et éviter les ambiguïtés locales/DST.
- **Conséquences :** les intervalles sont semi-ouverts `[début, fin)`. Les heures locales inexistantes ou ambiguës doivent être détectées, pas devinées.

## D-003 — Séparer source, correction et intervalles affichés
- **Décision :** conserver les horaires source et les corrections dans des champs distincts ; une correction référence sa valeur d'origine, sa date, sa raison et sa provenance.
- **Motif :** ne jamais perdre la donnée d'origine et permettre l'audit.
- **Conséquences :** une resynchronisation remplace les valeurs source mais conserve les corrections. Les intervalles de prière affichés sont déduits des horaires réellement disponibles.

## D-004 — AlAdhan comme source calculée, jamais officielle
- **Décision :** utiliser `https://api.aladhan.com/v1/timings/{jj-MM-aaaa}` avec `method=19` (Algérie), latitude/longitude et `timezonestring`.
- **Motif :** source réelle, documentée et accessible ; aucune API officielle algérienne publique documentée n'a été identifiée.
- **Conséquences :** l'UI et la documentation doivent afficher « calculé / non officiel ». Interdiction de présenter ces horaires comme une publication officielle.

## D-005 — Anneaux et intervalles de prière
- **Décision :** un seul anneau de prière affiche les intervalles réels : Fajr→Shurūq, chaque prière jusqu'à la suivante, et Isha jusqu'au Fajr du lendemain. Shurūq est un événement solaire distinct, pas une sixième prière.
- **Motif :** refléter les horaires réellement disponibles sans inventer de temps religieux.
- **Conséquences :** un intervalle n'est affiché que si sa borne de fin existe réellement ; sinon rien n'est inventé. Les temps optionnels/nécessaires sont **supprimés** du modèle, du moteur et de l'UI.

## D-006 — Cache SQLite et affichage hors ligne
- **Décision :** persister les jours localement et afficher l'état `CURRENT`, `STALE`, `UNAVAILABLE` ou `UNVERIFIED`.
- **Motif :** l'application doit rester consultable sans réseau sans masquer l'incertitude.
- **Conséquences :** un échec réseau conserve les données précédentes avec un avertissement. Un jour incomplet, sans hégire plausible ou avec erreur fatale n'est pas présenté comme fiable.

## D-007 — Ville et fuseau contrôlent la validité locale
- **Décision :** liste curée de villes algériennes ; chaque jour stocke `locationId` et fuseau ; les données d'une autre ville/fuseau sont masquées.
- **Motif :** éviter d'afficher des horaires valides pour un autre lieu.
- **Conséquences :** changer de ville ou de fuseau peut rendre le cache inutilisable jusqu'à synchronisation ; l'UI doit expliquer ce désaccord.

## D-008 — Arabe par défaut, français et anglais
- **Décision :** arabe par défaut, RTL ; français et anglais en LTR ; choix persisté.
- **Motif :** exigence utilisateur et accessibilité.
- **Conséquences :** ressources `values`, `values-fr`, `values-en` ; `supportsRtl` et `localeConfig` ; le rendu réel des trois langues reste à vérifier.

## D-009 — Couleurs personnalisables sans dépendre uniquement de la couleur
- **Décision :** couleurs par défaut modifiables individuellement et réinitialisables : Fajr `#315C9B`, Dohr `#2D8069`, Asr `#7956A8`, Maghrib `#C87532`, Isha `#34495E`.
- **Motif :** personnalisation visuelle et lisibilité clair/sombre.
- **Conséquences :** légende, libellés et symboles accompagnent toujours les couleurs ; un avertissement de contraste est prévu dans l'éditeur. Shurūq est affiché comme événement solaire distinct, sans couleur de prière.

## D-010 — Horloge analogique 12 heures
- **Décision :** cadran 12 h avec AM/PM explicites et deux voies par anneau : AM pointillés, PM continus.
- **Motif :** lisibilité et distinction des prières avant/après midi.
- **Conséquences :** la logique temporelle reste en 24 h ; seule la présentation est en 12 h.

## D-011 — Hégire fournie par la source
- **Décision :** afficher la date hégirienne telle que fournie par AlAdhan ; ne pas la recalculer localement.
- **Motif :** éviter une conversion religieusement sensible non validée.
- **Conséquences :** absence ou date invraisemblable ⇒ état non vérifié.

## D-012 — Synchronisation prudente
- **Décision :** charger hier, aujourd'hui et demain en une synchronisation ; auto-sync au maximum toutes les 12 h après succès et 15 min après tentative.
- **Motif :** limiter les appels réseau et disposer du contexte autour de minuit.
- **Conséquences :** aucune limite de débit officielle n'étant documentée dans l'OpenAPI consulté, rester conservateur. Un échec ne doit pas effacer le cache.

## D-013 — Preuve avant déclaration
- **Décision :** ne déclarer build, test ou intégration réussis qu'après exécution et résultat visible.
- **Motif :** éviter les faux états « terminé ».
- **Conséquences :** les tâches `DONE` de `TASKS.md` citent une commande ou une vérification réelle.

## D-014 — Import de créneaux explicites (supprimé)
- **Décision :** aucun import d'intervalles supplémentaires n'est prévu. Les temps optionnels/nécessaires et leur interface d'import sont supprimés.
- **Motif :** simplification métier et suppression de la distinction optionnel/nécessaire.
- **Conséquences :** la base conserve des colonnes legacy pour compatibilité, mais elles ne sont plus lues ni écrites. Pas d'interface d'import.
