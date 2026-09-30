# Fordix

Mod Minecraft **Forge 1.20.1** (Forge 47.x, Java 17) d'Omneria : bibliothèque d'interface
(`client/gui`, identité visuelle « Voidframe », écrans vanilla redessinés dans
`client/gui/vanilla`), panneaux créés par les admins en jeu (`/fordix panel ...`, `panel/`),
notifications serveur (`/fordix notify`, annonces automatiques, `notification/`) et onglets
Guides de l'inventaire (`client/guide`). Installé côté client **et** serveur (canal réseau).

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
- Test en jeu : `client/devtest/DevTest` (actif seulement si la variable `FORDIX_DEVTEST` est definie) ouvre un
  monde plat, parcourt HUD, ecrans et infobulles et enregistre une capture de chacun dans `run/fordix-shots`.
  En local : `xvfb-run -a -s "-screen 0 1280x720x24" env FORDIX_DEVTEST=1 LIBGL_ALWAYS_SOFTWARE=1
  MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450 ./gradlew runClient` (mettre
  `onboardAccessibility:false` dans `run/options.txt`). La CI le lance (job `ingame-test`) et publie les
  captures en artefact `ingame-screenshots` ; il echoue si le jeu plante. Ajouter un `Step` pour tout nouvel ecran.
- Avec Twilight Forest et Create dans le client de dev : `./gradlew runClient -PdevMods` (télécharge les deux mods
  via cursemaven/Modrinth, uniquement pour ce test local ; jamais utilisé par la CI ni la release).
- Règles de rareté : les règles par défaut de `RarityRules` complètent toujours celles du serveur (`withBuiltins`) ;
  les règles du serveur passent en premier, donc une règle explicite garde la main.
- Dans un conteneur cloud Claude, les dépôts Maven de Forge sont souvent bloqués et seul
  Java 21 est installé : si le build local échoue pour ces raisons, s'appuyer sur la CI.

## Publier une version

1. Passer `mod_version` à la nouvelle version dans `gradle.properties`, committer, pousser.
2. Tagger ce commit `vX.Y.Z` (identique à `mod_version`) et pousser le tag. Les versions
   officielles se taggent sur `main`, après fusion de `dev`, à la demande de l'utilisateur.
3. `.github/workflows/release.yml` publie `fordix-X.Y.Z.jar` + `.sha256` en release GitHub ; le
   VPS l'installe dans le modpack puis sur le serveur (voir `scripts/vps/README.md`).

Changer le protocole réseau (`PROTOCOL_VERSION` dans `PanelNetwork`) oblige joueurs et serveur à
avoir la même version : le serveur ne prend le nouveau jar qu'à son redémarrage.

## Interface Voidframe (client)

- `client/gui/vanilla` : écrans de conteneurs redessinés (`VoidframeScreens`, `VoidframeStations`), HUD
  (`VoidframeHud` vie/faim, placement par glisser-deposer dans `HudLayoutScreen` avec aimant `HudSnap`, `VoidframeGauges` XP/armure/air/boss, `VoidframeDurability`), forme commune des
  barres (`Chevron`, `BarShape`), recoloration des textures vanilla (`TextureRecolor`, `Duotone`).
- `client/tooltip` : infobulles d'objets (`ItemTooltipRenderer`, `TooltipStyle`, `EnchantEffects`,
  `AttributeMath`) ; `/fordixdump` écrit `fordix-analysis.json` (objets, attributs, enchantements des mods).
- Écran d'administration (`FordixAdminScreen`, ops) : envoi de notifications, planification (`AdminStore`,
  `NotificationScheduler`, sauvegardée dans `fordix-admin.json` du monde), modèles et éditeur de panels.
- Guides : onglets de l'inventaire (`GuideTabs`), écran Guides ouvert depuis le menu pause (`GuidesScreen` : guides des mods
  + pages du serveur `GuidePage`/`PageMarkup`, stockées dans `AdminStore`, édités dans l'onglet Pages de l'admin, envoyées par
  `ClientboundGuidePagesPayload`), indice Maj + G dans l'infobulle des objets d'un mod avec guide (`GuideHints`).
- Livre de bienvenue (item `fordix:welcome_book`, `welcome/`) : liste d'étapes `[welcomeBook] steps` de `fordix-common.toml`
  (advancement, command, guides, manual), récompense facultative, donné à la première connexion ou par `/fordix book`.
  L'onglet Livre de l'écran d'administration remplace ces étapes et la récompense (`AdminStore.welcomeSteps/welcomeReward`, sans
  redémarrage) ; « Fichier config » revient à `fordix-common.toml`.
- Les réglages joueur sont éditables en jeu (bouton Config de la liste des mods, `FordixSettingsScreen`).
- Le serveur peut imposer la liste des onglets Guides (`[guides] tabs` dans `fordix-common.toml`).

## Conventions

- Le code serveur (`panel/server`, `panel/PanelSpec`) ne doit pas référencer de classes client ;
  le code client n'est appelé qu'à travers `DistExecutor` (voir `PanelNetwork`).
- Les calculs sans dépendance Minecraft (ex. `client/gui/layout/Layout`) restent en Java pur
  pour être testables avec JUnit sans lancer le jeu.
- Les messages de commande affichés en jeu sont en français, sans accents
  (ex. « Panel 'x' supprime. »).
