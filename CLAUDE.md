# Fordix

Mod Minecraft **Forge 1.20.1** (Forge 47.x, Java 17) d'Omneria : bibliothèque d'interface
(`client/gui`, identité visuelle « Voidframe ») et panneaux créés par les admins en jeu
(`/fordix panel create|describe|button|send|list|delete`, code dans `panel/`).

## Branches

- **`dev`** : branche de développement. Tout nouveau travail part de `dev` et y est poussé.
- **`main`** : version officielle du projet. Ne jamais y pousser directement ; on n'y fusionne
  `dev` que lorsque l'utilisateur le demande explicitement.

## Build et tests

- `./gradlew build` compile le mod et lance les tests JUnit (`src/test`).
- Le projet cible **MinecraftForge**, pas NeoForge, malgré le plugin Gradle
  `net.neoforged.moddev.legacyforge` : utiliser les API `net.minecraftforge.*` de Forge 47.
- La CI (`.github/workflows/build.yml`) compile à chaque push et publie le jar du mod comme
  artefact `fordix-jar`, à télécharger depuis le run GitHub Actions pour tester en jeu.
- Dans un conteneur cloud Claude, les dépôts Maven de Forge sont souvent bloqués et seul
  Java 21 est installé : si le build local échoue pour ces raisons, s'appuyer sur la CI.

## Conventions

- Le code serveur (`panel/server`, `panel/PanelSpec`) ne doit pas référencer de classes client ;
  le code client n'est appelé qu'à travers `DistExecutor` (voir `PanelNetwork`).
- Les calculs sans dépendance Minecraft (ex. `client/gui/layout/Layout`) restent en Java pur
  pour être testables avec JUnit sans lancer le jeu.
- Les messages de commande affichés en jeu sont en français, sans accents
  (ex. « Panel 'x' supprime. »).
