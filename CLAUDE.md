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

1. Passer `mod_version` à la nouvelle version dans `gradle.properties`, committer, pousser sur `dev`.
2. À la demande de l'utilisateur, fusionner `dev` dans `main` (avance rapide). Le workflow `release.yml` voit le
   `gradle.properties` poussé sur `main`, crée tout seul le tag `vX.Y.Z` (identique à `mod_version`) et publie la release.
   Un tag poussé à la main reste possible et donne le même résultat ; `workflow_dispatch` sur `main` rattrape une version
   (Actions > Release > Run workflow). Les sessions Claude ne peuvent pas pousser de tag (403) : ne pas essayer.
3. `.github/workflows/release.yml` publie `fordix-X.Y.Z.jar` + `.sha256` en release GitHub ; le
   VPS l'installe dans le modpack puis sur le serveur (voir `scripts/vps/README.md`).

Changer le protocole réseau (`PROTOCOL_VERSION` dans `PanelNetwork`) oblige joueurs et serveur à
avoir la même version : le serveur ne prend le nouveau jar qu'à son redémarrage.

## Interface Voidframe (client)

- `client/gui/vanilla` : écrans de conteneurs redessinés (`VoidframeScreens`, `VoidframeStations`), HUD
  (`VoidframeHud` vie/faim, placement par glisser-deposer dans `HudLayoutScreen` avec aimant `HudSnap`, `VoidframeGauges` XP/armure/air/boss, `VoidframeDurability`), forme commune des
  barres (`Chevron`, `BarShape`), recoloration des textures vanilla (`TextureRecolor`, `Duotone`).
  `HudLayoutScreen` liste aussi le HUD que Fordix ne dessine pas : chat, barre d'objets, scoreboard, effets, titres et tout overlay
  Forge d'un autre mod (`ExtraHud` : mesuré en le dessinant hors écran, déplacé/masqué par ses événements de rendu, sauvé dans
  `fordix-hud-extras.json`), plus la minicarte JourneyMap (`JourneyMapHud`, par réflexion sur ses réglages, position `Custom`).
  `HudSlots` donne un accès unique aux trois. Chaque élément se redimensionne (carré du coin, molette, + / -) : facteur `scale` autour du coin haut-gauche (`HudPlacement.begin/end`, `HudSlot.scale`, taille `sizePercent` pour JourneyMap). Les clics sur un chat déplacé restent décalés (limite connue).
- `client/tooltip` : infobulles d'objets (`ItemTooltipRenderer`, `TooltipStyle`, `EnchantEffects`,
  `AttributeMath`) ; `/fordixdump` écrit `fordix-analysis.json` (objets, attributs, enchantements des mods).
- Écran d'administration (`FordixAdminScreen`, ops) : envoi de notifications, planification (`AdminStore`,
  `NotificationScheduler`, sauvegardée dans `fordix-admin.json` du monde), modèles et éditeur de panels.
- Guides : onglets de l'inventaire (`GuideTabs`), écran Guides ouvert depuis le menu pause (`GuidesScreen` : guides des mods
  + pages du serveur `GuidePage`/`PageMarkup`, stockées dans `AdminStore`, édités dans l'onglet Pages de l'admin, envoyées par
  `ClientboundGuidePagesPayload`), indice Maj + G dans l'infobulle des objets d'un mod avec guide (`GuideHints`) ; Maj + G ouvre la scène Ponder de l'objet (Create 0.5.x, `PonderScenes`, par réflexion) quand il en a une, sinon sa fiche (`ItemSheetScreen`, `ItemFacts` : recettes, usages et caractéristiques lus dans le jeu, sans réseau ; un clic sur un objet ouvre sa fiche). Un lien `url:` peut contenir `{objet}` (page du wiki de l'objet) ; sans entrée « minecraft » dans la liste, `GuideTabs` ajoute un guide vanilla vers minecraft.wiki ; l'option client `guideHints` masque la ligne d'indice.
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
- Tous les textes affichés par le mod passent par `Tr` (`Tr.c` composant traduit côté destinataire, `Tr.s` texte client) et les
  fichiers `assets/fordix/lang/en_us.json` et `fr_fr.json` (clés préfixées `fordix.`) : ne jamais écrire un texte en dur. Les
  retours serveur de l'écran d'administration voyagent sous la forme `Tr.encode(clé, args…)`. Ajouter une clé aux deux fichiers.
  Restent volontairement en français : les valeurs des configs (étapes du livre, libellés de guides) et les étiquettes techniques
  des destinataires dans l'historique des notifications.
