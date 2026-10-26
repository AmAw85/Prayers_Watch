# AGENTS.md — Règles permanentes

## Reprise du projet
1. Lire `PROJECT_STATE.md`, puis `TASKS.md`.
2. Vérifier l'état réel : `git status` si un dépôt existe, fichiers modifiés, artefacts et résultats de build.
3. Lire `DECISIONS.md` avant de modifier une décision architecturale.
4. Ne consulter `DEVLOG.md` que pour retrouver un contexte historique utile.
5. Ne pas relire tout l'historique des conversations ni tous les fichiers du projet.

## Travail
- Prendre la première tâche `IN_PROGRESS`, sinon la prochaine `TODO` indiquée.
- Respecter les fonctionnalités et exigences déjà validées.
- Ne pas écraser les modifications non validées de l'utilisateur.
- Ne pas inventer une API, une source officielle ou une donnée religieuse.
- Vérifier le code et exécuter le test ou la commande du critère d'acceptation avant de déclarer une tâche terminée.
- Ne jamais affirmer qu'une compilation, un test ou une intégration a réussi sans résultat vérifiable.
- Ne pas créer de commit ni initialiser un dépôt Git sans autorisation explicite.
- Si la documentation contredit le code ou les résultats d'exécution, faire foi du code et des commandes, puis corriger la documentation.

## Mise à jour
À la fin de chaque unité de travail significative :
1. Mettre à jour `TASKS.md`.
2. Mettre à jour `PROJECT_STATE.md`.
3. Ajouter une entrée courte à `DEVLOG.md`.
4. Mettre à jour `DECISIONS.md` seulement si une décision change.
5. Vérifier la cohérence des quatre fichiers.
