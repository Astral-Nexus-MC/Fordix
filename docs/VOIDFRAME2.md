# Voidframe 2 : cahier des charges

Document de référence de l'identité visuelle et de l'interface de Fordix. Il décrit ce que Voidframe 2 est, comment on
le construit et comment on le fait évoluer. **Toute production d'interface (écran, composant, HUD, notification) doit le
respecter** ; toute évolution du langage visuel commence par une modification de ce document. Il sert aussi de base de départ
à Voidframe 3 (voir la section 15).

- Version du document : **2.0** (octobre 2026, mod Fordix 0.1.71).
- Public : développeurs du mod, intégrateurs du serveur Astral Nexus, toute personne ou assistant qui produit de l'interface.
- Convention : **DOIT** = obligatoire, **DEVRAIT** = attendu sauf raison écrite, **PEUT** = libre.
- Maquettes de référence (privées, à rouvrir avant tout travail visuel) :
  HUD en simulation <https://claude.ai/artifact/3eZxLusogipD4cpyhWbgQR>,
  catalogue des éléments du HUD <https://claude.ai/artifact/8foPWT3XWoePLwAeFeZPzY>,
  écran de réglages <https://claude.ai/artifact/TJXmiFhGETeWMP4poog4qn>,
  audit et écrans du jeu <https://claude.ai/artifact/YPdMR7SLicdmtJN7AnqBWS>.

---

## 1. Vision

Fordix est le mod d'interface du serveur Astral Nexus (Minecraft Forge 1.20.1). Voidframe est son identité : une interface
sombre, nette et technique, qui se pose sur le jeu sans le cacher.

**Voidframe 2 en une phrase : du verre sombre sur une nuit bleue, un seul accent lumineux, et du mouvement seulement quand il
informe.**

Principes, par ordre de priorité quand ils s'opposent :

1. **Le jeu d'abord.** L'interface laisse voir le monde. Les panneaux sont translucides, les plus petits possibles, et
   rien ne bouge en continu sauf un danger (vie critique).
2. **Lisible en un coup d'œil.** L'état se lit par la forme et la couleur *et* par un mot ou un chiffre. Jamais par la couleur seule.
3. **Une seule grammaire.** Un bouton, une case, un panneau, un champ se dessinent partout de la même façon. Aucun écran n'invente
   son propre bouton.
4. **Un accent, pas un arc-en-ciel.** L'accent du thème (cyan par défaut) désigne ce qui est actif ou principal. Les couleurs
   sémantiques (vert, ambre, rouge) disent un état. Les couleurs de rareté et d'attribut disent une nature. Rien d'autre n'est coloré.
5. **Le serveur pilote.** Ce qui touche le jeu collectif (disposition du HUD, éléments obligatoires ou interdits, situations)
   est décidé par le serveur, jamais par un réglage joueur.
6. **Robuste aux mises à jour.** On remplace un écran du jeu seulement quand c'est sûr ; sinon on l'habille. Aucune dépendance à
   un nom de champ obfusqué.

## 2. Périmètre

### 2.1 Ce qui est en Voidframe 2

| Domaine | Éléments | Référence code |
|---|---|---|
| HUD | barres de vie et de faim, jauges (XP, armure, air, monture, boss), barre d'objets, panneaux d'information, cibles, effets, chat, liste des joueurs, notifications, alertes, scoreboard, titre à l'écran, message de la barre d'action | `client/hud`, `client/gui/vanilla/Voidframe*`, `client/chat` |
| Conteneurs | inventaire du joueur, coffres, tonneaux, shulkers, entonnoir, distributeur, établi, fourneaux | `VoidframeScreens` |
| Stations | enclume, meule, alambic, balise, enchantement, villageois, tailleur de pierre, forge, cheval | `VoidframeStations` |
| Objets | infobulle, liseré de rareté, durabilité | `client/tooltip`, `RarityFrames`, `VoidframeDurability` |
| Écrans Fordix | réglages, éditeur de HUD, administration, guides, wiki, fiches d'objet et de créature, livre de bienvenue, avancements, roue des animaux, panels générés | `client/gui`, `client/pet` |
| Menus du jeu | menu pause, écran de mort, habillage de tous les écrans du jeu (boutons, fond) | `PauseSkin`, `VoidframeDeathScreen`, `ScreenBackdrop`, `VoidframeButtons` |

### 2.2 Ce qui n'y est pas, et pourquoi

| Élément | Raison |
|---|---|
| **Écran titre et liste des mondes** | Cachés par le mod **Astral-launcher** : le joueur n'y passe pas. C'est la situation normale, ne pas y investir. Ils gardent seulement la recoloration des boutons. |
| Métier à tisser, cartographie, créatif | En verre depuis la 0.1.78 : le corps de la texture du jeu est vidé (`TextureRecolor`), le panneau, les puits et les onglets sont dessinés par `VoidframeStations`. |
| Sous-titres sonores, F3 | Le jeu ne publie aucun événement exploitable pour les sous-titres ; F3 est un outil de diagnostic. Restent vanilla (voir la dette, section 14). |
| Signes, livres, blocs de commande, cartes | Ce sont des objets du monde, pas de l'interface. |
| Écrans des autres mods (Create, Twilight Forest, MineColonies, JourneyMap) | Dessinés par ces mods. Fordix ne touche qu'aux écrans du jeu, de Forge et les siens (`VoidframeButtons.isGameScreen`). |

## 3. Langage visuel

### 3.1 La matière : le verre

Un panneau Voidframe 2 est une plaque de verre sombre. Il est dessiné par `HudStyle.panel` (HUD) et `Glass.panel` (écrans), qui
sont le même dessin. De bas en haut :

1. **Ombre portée** : trois cadres noirs translucides (14, 18, 22 sur 255) qui grandissent de 1 à 3 px et descendent de 2 px.
2. **Corps** : dégradé vertical du haut (teinte de base éclaircie de 6 %) au bas (teinte de base assombrie de 50 %). Opacité
   du corps : 238 pour un écran, celle choisie par le joueur pour un panneau de HUD.
3. **Lumière haute** : une ligne blanche à 14 % d'opacité sur le bord supérieur, retirée d'un pixel de chaque côté.
4. **Ombre basse** : une ligne noire à 40 % sur le bord inférieur.
5. **Reflet** : dégradé blanc de 6 % à 0 sur le tiers supérieur.
6. **Liseré d'accent** : bande de 2 px sur le bord gauche, pleine en haut et fondue vers 19 % en bas, doublée d'une lueur de 3 px à
   16 % qui s'éteint. C'est la signature de Voidframe 2.

Le liseré peut prendre une autre couleur quand le panneau porte un sens (rouge pour la mort, couleur de rareté pour une
infobulle). Il n'y a **pas** de panneau à bords biseautés (le biseau est Voidframe 1).

**Cartes internes** (`Glass.card`) : fond noir à 18 %, ligne claire à 5 % en haut, ligne sombre en bas, sans liseré. Elles groupent
des lignes dans un panneau. On n'empile pas de verre sur du verre.

### 3.2 Palette et tokens

Les couleurs viennent du thème actif (`Theme`, appliqué à `FordixPalette`) ; **aucune couleur de structure ne s'écrit en dur dans un
écran**. Le thème par défaut est `voidframe`.

Thème `voidframe` (ARGB) :

| Rôle | Token | Valeur |
|---|---|---|
| Fond de panneau | `PANEL_FILL` | `#131A22` |
| Surbrillance / ombre de bord | `BEVEL_HIGHLIGHT` / `BEVEL_SHADOW` | `#24313D` / `#05070A` |
| Bouton / bouton survolé | `BUTTON_FILL` / `BUTTON_FILL_HOVERED` | `#16202A` / `#1C2830` |
| **Accent** | `ACCENT` | `#3FD1C7` |
| Texte / texte vif | `TEXT` / `TEXT_BRIGHT` | `#9FB3C0` / `#C9E9E5` |
| Puits (cases, pistes) | `SLOT_FILL` / `PROGRESS_TRACK` | `#0B1016` / `#1E2A35` |
| Flamme / erreur | `FLAME` / `ERROR` | `#E39B45` / `#E0524A` |

Trois autres thèmes existent (`ember`, `verdant`, `amethyst`) ; ils ne changent que ces tokens. Tout composant DOIT donc dériver ses
nuances du token (mélange avec le blanc ou le noir : `HudTokens.mix`), pas d'une valeur fixe.

Couleurs **sémantiques**, communes à tous les thèmes :

| Sens | Valeur | Usage |
|---|---|---|
| Bon / réussi / vie confortable | `#5ED68A` | succès, durabilité pleine, ping bon, coût payable |
| Attention | `#E8A64A` | faim, alerte, coût proche, ping moyen |
| Danger | `#E5534B` | vie basse ou critique, destruction, ping mauvais, mort |
| Eau / information froide | `#4DA3FF` | air, vitesse de minage |
| Boss / violet | `#B45FE0` | barre de boss, défi, enchantement |

Couleurs de **rareté** (`RarityPalette`) : commun `#EDF2F5`, peu commun `#5ED68A`, rare `#4EA3FF`, épique `#C58BFF`, légendaire
`#FF9A2E`. Couleurs d'**attribut** (infobulle) : dégâts et vie rouge, vitesse d'attaque ambre, armure gris-bleu, robustesse
bleu clair, résistance au recul violet, vitesse verte. Couleurs d'**action** de la roue : suivre cyan, rester gris, nourrir
vert, inventaire ambre, rappeler bleu, renommer violet, relâcher rouge.

### 3.3 Typographie

La police est celle du jeu (pixel, 8 px de haut) : un mod ne peut pas en livrer une autre sans casser la cohérence avec les mods
installés. La hiérarchie se fait donc par la **casse, la couleur et l'échelle**.

| Rôle | Rendu | API |
|---|---|---|
| Titre d'écran, de panneau, de section forte | capitales, accent éclairci de 35 % | `Glass.title` |
| Étiquette de groupe | capitales, ton atténué du texte (`Glass.dim`) | `Glass.label` |
| Texte courant | casse normale, `TEXT` | `drawString` |
| Valeur importante | casse normale, `TEXT_BRIGHT` ou couleur sémantique | `drawString` |
| Texte secondaire (aide, source) | `Glass.dim()` | `drawString` |
| Grand titre (mort, nom du monde dans la pause) | échelle 1,8 ou 3, avec ombre | `pose().scale` |

Règles : jamais plus de trois échelles par écran ; pas de texte justifié ; pas de gras ni de soulignement simulés ; un titre tient sur
une ligne (utiliser `font.plainSubstrByWidth`).

### 3.4 Géométrie et espacement

- Unité de base : **2 px** (en pixels d'interface, après l'échelle choisie par le joueur).
- Marge intérieure d'un panneau : **12 px** (14 px pour un panneau de réglages) ; 7 px pour une infobulle ou le scoreboard.
- Écart entre deux lignes d'une liste : **4 à 6 px**. Écart entre deux groupes : 8 à 12 px.
- Case d'objet : **20 px** (pas de 22 px) dans la disposition large, 18 px dans la compacte. Hauteurs de contrôle : bouton 24 (principal 26 à 28), ligne de réglage 28, ligne de liste 12 à 14, champ 16 à 22, pastille 11,
  barre fine 5 à 7.
- Coins : **carrés** (le jeu n'a pas de coins arrondis). Les pastilles ont leurs quatre coins coupés d'un pixel.
- Les panneaux se centrent ; la colonne du menu pause se place à 32 px du bord gauche.
- Aucun écran ne DOIT dépasser l'écran à l'échelle d'interface 2 (854 x 480) ni tronquer un texte : on raccourcit le texte (lang) ou
  on coupe à la largeur.

### 3.5 Profondeur

Ordre de dessin, du fond vers l'avant : monde, HUD, fond d'écran assombri, panneau, contrôles, infobulle (z +400), notifications
(z +450). Un élément qui doit passer au-dessus de tout (notification, panneau de l'éditeur) se translate en z (`pose().translate`)
et ne s'appuie jamais sur l'ordre d'enregistrement.

### 3.6 Iconographie

Pas de texture ajoutée pour l'interface : les icônes sont les objets du jeu (`renderItem`) ou de petits glyphes dessinés avec
`fill` (livre, trophée, croix, flèche, flamme). Un glyphe DEVRAIT tenir dans 12 x 12 px et se colorer avec l'accent.

## 4. Mouvement

Le mouvement confirme une action ou signale un danger ; il ne décore pas.

| Mouvement | Durée | Courbe | Où |
|---|---|---|---|
| Survol d'un bouton ou d'une ligne | 120 ms | linéaire | `Glass.Fade` |
| Fondu d'un panneau de HUD (changement de situation) | 200 ms | linéaire | `HudTokens.FADE_MS` |
| Ouverture d'un écran de conteneur | 160 ms, glissement vertical de 8 px | `easeOutCubic` | `VoidframeChrome` |
| Halo de case survolée | 150 ms | linéaire | `VoidframeChrome` |
| Notification | entrée 200 ms, maintien 2,2 s, sortie 300 ms | linéaire | `HudTokens.toastAlpha` |
| Encre qui glisse sous un onglet | easing exponentiel | `Smoothing` | `TabButton` |
| Interrupteur | bille qui glisse, 120 ms | `easeOutCubic` | `ToggleRow` |
| Reflet qui balaie une barre | cycle de 3,2 s, balayage de 0,8 s | linéaire | `HudTokens.glint` |
| **Pulsation de danger** | 1 Hz, sinusoïdale | cosinus | `HudTokens.pulse` |

Règles : **la pulsation est le seul mouvement continu autorisé** (vie sous 15 %, mort). Pas de zoom d'un panneau (il floute le
texte) : on glisse. Pas d'animation qui retarde une action : un clic agit tout de suite. Toute durée nouvelle s'ajoute à ce
tableau avant d'être codée. Voidframe 3 DEVRAIT ajouter une option « réduire les animations » (section 15).

## 5. Composants

Chaque composant a une seule implémentation de référence. Un écran qui en a besoin l'appelle ; il ne le recopie pas.

### 5.1 Surfaces

| Composant | Rôle | API |
|---|---|---|
| Panneau | fond d'un écran ou d'une fenêtre | `Glass.panel(g, x, y, w, h[, accent])` ; `Panel.draw` y mène |
| Carte | groupe interne sans liseré | `Glass.card` |
| Fond d'écran | assombrit le monde derrière un écran plein | `Glass.backdrop` ; `FordixScreen.renderBackground` |
| Puits | creux : case d'objet, piste, aperçu | `Glass.well` ; `VoidframeDraw.well` y mène |

Le puits est plus sombre en haut (ombre intérieure) avec un liseré clair en bas : il donne l'impression d'un creux, jamais d'un bouton.

### 5.2 Boutons

`FordixButton` (écrans Fordix) et `VoidframeButtons.StandIn` (boutons du jeu, repris à l'ouverture de l'écran) dessinent avec
`Glass.button(g, x, y, w, h, kind, hover, active)`.

| Genre | Usage | Aspect |
|---|---|---|
| `NORMAL` | tout par défaut | verre, bord blanc à 11 %, ligne basse d'accent au survol |
| `PRIMARY` | **une seule action principale par écran** (Terminé, Retour au jeu, Réapparaître) | dégradé d'accent, texte sombre |
| `DANGER` | action destructive (quitter, supprimer) | bord et texte rouges |
| `selected` | choix courant d'un groupe d'onglets ou d'une liste | ligne d'accent allumée, libellé d'accent clair |

États : repos, survol (lueur d'accent de 1 px à 14 % + ligne basse, fondu 120 ms), focus clavier (même rendu que le survol),
inactif (voile sombre à 44 %, texte à 60 %). Un bouton inactif DOIT garder son libellé lisible (il dit pourquoi il est grisé par
infobulle quand c'est possible). Les boutons du jeu reçoivent leur genre d'après la clé de traduction (`VoidframeButtons.kindOf`).

### 5.3 Champs de saisie

`FieldBox` (sous-classe d'`EditBox`) : fond noir à 47 %, bord clair à 12 %, ombre intérieure en haut ; au focus, bord d'accent et
lueur. Texte inséré de 5 px. Tout champ du mod DOIT être un `FieldBox`.

### 5.4 Lignes de réglage

`SettingRow` et ses trois formes : `ToggleRow` (interrupteur), `ChoiceRow` (sélecteur par segments), `NavRow` (chevron ou mot).
Anatomie : icône dans un carré teinté d'accent, titre en Chakra-style capitales douces, explication courte en dessous, contrôle
à droite. Hauteur 28. Ce sont les seules lignes à utiliser pour un réglage, un choix de mode ou une navigation.

### 5.5 Onglets et segments

`TabButton` : onglet de texte avec une encre d'accent qui glisse sous l'onglet choisi. Pour une liste d'onglets d'écran, utiliser
`FordixButton.selected`. Un groupe de choix exclusifs de quelques mots utilise le sélecteur par segments de `ChoiceRow`.

### 5.6 Listes

Ligne de liste : `Glass.row(g, x, y, w, h, selected, hovered)` (voile à 8 % au survol ; voile d'accent à 16 % avec liseré gauche quand
elle est choisie). Séparateur de colonnes : `Glass.divider`. En-tête de section dans une liste : `Glass.title` suivi d'un filet
fin. Une liste défile à la molette et montre sa position ; elle ne fait pas de glissement tactile.

### 5.7 Petites marques

| Marque | API | Usage |
|---|---|---|
| Pastille | `Glass.pill(g, font, texte, x, y, couleur)` | état, compteur, rareté, « Nouveau », dimension |
| Capsule de touche | `Glass.keyCap` | raccourci affiché (« G ») |
| Filet d'accent | `Glass.rule(g, x, y, w[, couleur])` | sous un titre ; s'estompe vers la droite |
| Bouton de texte | `Glass.textButton` | action dans une page ou une liste, sans widget |

### 5.8 Jauges

| Jauge | API | Aspect |
|---|---|---|
| Barre fine (HUD, infobulle, scoreboard) | `HudStyle.bar`, `Chevron.draw` | rail noir à 60 %, remplissage en dégradé clair vers la couleur, reflet |
| Flèche de progression (fabrication, fourneau) | `VoidframeDraw.arrow` | contour en piste, remplissage d'accent en dégradé |
| Flamme (carburant) | `VoidframeDraw.flame` | remplissage du bas, clair vers sombre |
| Jauge simple | `VoidframeDraw.gauge`, `gaugeDown` | piste + dégradé |

Seuils communs (`HudTokens`) : bas à **30 %**, critique à **15 %**. Sous le seuil bas la couleur change ; sous le seuil critique la
jauge pulse et l'écran avertit.

### 5.9 Cases d'objets

Une case est un puits de 18 x 18 px. Survol : voile d'accent à 16 %, contour d'accent, lueur extérieure de 1 px (`VoidframeChrome`).
Rareté : **un liseré lumineux de 2 px sous l'objet avec un voile qui monte de 5 px** (`RarityFrames`), jamais un cadre plein.
Durabilité : barre à reflet sous l'objet (`VoidframeDurability`).

### 5.10 Infobulle d'objet

`ItemTooltipRenderer` : panneau de verre au liseré de la couleur de rareté ; en-tête (icône, nom à la couleur de rareté, ligne
« TYPE · RARETÉ · MOD » en capitales) ; filet de rareté ; sections par emplacement d'équipement (étiquette en capitales) avec une
barre `Chevron` par attribut, la valeur et l'écart avec l'objet porté ; verdict global (▲ ▼ ◆) ; enchantements en pastilles avec leur
effet en chiffres ; durabilité en barre. L'infobulle se positionne comme celle du jeu (`positionTooltip`) et ne dépasse jamais l'écran.

### 5.11 Notifications

`HudToast` (HUD) et `NotificationOverlay` (serveur) : **toujours en haut à droite, au-dessus de tout** ; celles du HUD pendent sous
celles du serveur. Verre, liseré de la couleur du type (info, succès, alerte, erreur), titre, détail, barre de temps. Durées de
`HudTokens`. Une notification n'a jamais de bouton ; elle ne vole jamais le focus.

## 6. Les écrans, par gabarit

### 6.1 Conteneur (inventaire, coffres, fourneaux…)

Sous-classe de l'écran du jeu, remplacée à l'ouverture (`VanillaScreens`) **par classe exacte** : une sous-classe d'un autre mod
signale une personnalisation et n'est jamais remplacée. Chaque conteneur a **deux dispositions** :

- **Large (celle de la maquette)** quand le panneau tient dans l'écran avec 8 px de marge de chaque côté
  (`ContainerLayout.fits`) : cases de 20 px au pas de 22 px, titre à (14, 9) en capitales, compteur ou état à droite du titre, outils
  (recherche, tri, livre) **dans** le panneau, filet entre le conteneur et l'inventaire du joueur, bloc de statistiques dans
  l'inventaire. Les positions des cases sont déplacées côté client (`ContainerLayout.put`, par réflexion validée) ; le serveur ne les
  connaît que par leur numéro, rien ne change pour lui.
- **Compacte** quand l'écran est trop petit : les cases gardent leurs positions du jeu (18 px), le style V2 s'applique et les
  éléments ajoutés se placent à côté du panneau. La disposition large ne se choisit jamais sur la taille de la fenêtre mais sur celle
  de l'écran d'interface (`screen.width`, `screen.height`), donc l'échelle d'interface du joueur compte.

Panneaux larges : inventaire 500 x 348, coffre 218 x (300 pour trois rangées), fourneaux 218 x 252, enclume 218 x 330, établi
218 x 244, entonnoir et distributeur sur le gabarit du coffre. Chaque écran implémente `WideLayout` (`wide()`, `boxOf(slot)`,
`searchRect()`, `sortRect()`) pour que le halo de survol, le liseré de rareté et les outils sachent où est chaque case.
`VoidframeDraw.wideBackground` dessine le panneau et un puits par case ; `ContainerLayout.grid` et `playerBlock` placent les cases.
Ouverture : glissement de 8 px en 160 ms.

### 6.2 Station (enclume, alambic, balise…)

Même gabarit ; en plus : flèches et jauges de progression `VoidframeDraw`, **coût en pastille** (vert si le joueur peut payer, rouge sinon,
avec le manque), aperçu du résultat. Un choix dans une grille (tailleur de pierre) se dessine avec un puits par choix et `Glass.row`.

### 6.3 Écran Fordix plein (administration, wiki, fiches, avancements…)

Sous-classe de `FordixScreen` : fond `Glass.backdrop`, un panneau central de `Panel`, un titre `Glass.title` en haut à gauche
suivi d'un filet `Glass.rule`, contenu en deux colonnes séparées par `Glass.divider` quand il y a une liste et un détail, actions en
bas (`PRIMARY` à droite). Les écrans se ferment avec Échap et rendent le focus au jeu.

### 6.4 Écran de réglages

Panneau de 430 px, onglets `TabButton` (HUD, Jeu, Administration réservé aux opérateurs avec pastille OP), lignes `SettingRow`, aperçu en
direct des réglages visuels, encadré orange « Réglé par le serveur » pour ce que le serveur impose, bouton « Réinitialiser l'onglet ».

### 6.5 Menu du jeu

- **Menu pause** (`PauseSkin`) : l'écran du jeu est conservé ; ses boutons sont **réarrangés** en colonne de verre à gauche (une ligne par
  rangée du jeu, deux boutons côte à côte quand le jeu les met côte à côte, premier bouton en `PRIMARY` plus haut, dernier en `DANGER`
  séparé), titre « PAUSE » et nom du monde ou du serveur à l'échelle 1,8, carte de session à droite (joueur, coordonnées, jour, latence
  colorée, solo ou en ligne). Le titre « Game Menu » du jeu est vidé par réflexion (champ unique de type `Component`).
- **Écran de mort** (`VoidframeDeathScreen`) : sous-classe de `DeathScreen` (la logique de réapparition, de confirmation et de mode
  hardcore reste celle du jeu), voile rouge pulsé, grand titre, cause, carte (score, niveau, lieu et dimension), deux boutons.
- **Autres écrans du jeu** : boutons du jeu remplacés par des `StandIn` ; fond `ScreenBackdrop` (dégradé presque opaque s'il n'y a pas de
  monde derrière, simple teinte navy sinon).

### 6.6 Roue d'interaction

`PetWheelScreen` : sept secteurs de verre, la couleur de chaque action sur le bord et le halo du secteur visé, carte centrale (nom,
propriétaire, vie, état), aide en bas. Un secteur inutilisable est grisé et dit pourquoi.

### 6.7 Overlays du HUD

Un overlay du jeu qu'on remplace (`ScoreboardHud`, `ActionBarHud`, `TitleHud`) : s'enregistre sur `RenderGuiOverlayEvent.Pre`, annule celui du
jeu **seulement quand il a quelque chose à dessiner**, dessine au même endroit, et reste donc déplaçable et redimensionnable par
l'éditeur. Le HUD complet (barres, panneaux, situations, règles serveur) est décrit dans `CLAUDE.md`, section « HUD Voidframe 2 ».

## 7. Le HUD, rappel des règles

- **Situations** : exploration, combat, minage, boss, monture ; décidées par `HudContext`, jamais par un réglage joueur ; maintien 8 s en
  combat, 6 s en minage, 3 s minimum entre deux changements (sauf boss et combat) ; chaque changement est annoncé par une notification.
- **Éléments** : requis (jamais masquables : vie, barre d'objets), interdits (toujours masqués), proposés (code `FXH1-…` appliqué une
  fois), table de pourcentage par situation, dispositions par situation. Tout est posé par le serveur (`[hud]` de
  `fordix-common.toml`).
- **Éditeur** : panneaux repliables, jamais plus que leur propre surface, pour que tout élément reste déplaçable.
- **Imposé** : le HUD Voidframe, les nombres de dégâts, la liste Tab, l'exigence de boussole et d'horloge. Aucune option joueur.

## 8. Architecture

```
client/gui/Glass.java            primitives des écrans (panneau, puits, bouton, champ, ligne, pastille…)
client/gui/FordixPalette.java    couleurs du thème actif (statiques, relues à chaque dessin)
client/gui/Theme.java            les thèmes (pur, testé)
client/hud/HudStyle.java         panneau de verre du HUD, barres, vignette
client/hud/HudTokens.java        seuils, durées, courbes (pur, testé)
client/gui/widget/               FordixButton, FieldBox, Panel, SettingRow et ses formes, TabButton
client/gui/vanilla/              écrans du jeu redessinés, habillage, éléments de HUD du jeu repris
client/hud/                      HUD de Fordix, notifications, overlays remplacés
```

Règles d'architecture :

1. **Tout dessin d'écran passe par `Glass`** (ou `HudStyle` pour le HUD). Interdit : `fill` avec `BEVEL_*`, `BUTTON_FILL*` ou une
   couleur de structure en dur dans un écran. Les couleurs sémantiques, de rareté et d'attribut peuvent s'écrire dans le code
   qui les emploie, une fois, avec un nom.
2. **Une primitive modifiée change tous les écrans** : c'est voulu. On la modifie ici, jamais en la contournant.
3. **Remplacer un écran du jeu** : par sous-classe, sur sa **classe exacte**, en réutilisant son menu (le serveur n'est pas touché). Si
   un champ privé est nécessaire, le lire par **type** (`Reflect.uniqueField`), avec repli sur l'écran du jeu quand il est introuvable.
4. **Habiller plutôt que remplacer** quand l'écran est complexe, change à chaque version ou vient d'une extension : repositionner ou
   remplacer ses boutons, dessiner le fond par `ScreenEvent.BackgroundRendered` (`PauseSkin`, `ScreenBackdrop`, `VoidframeButtons`).
5. **Ordre des événements** : `ExtraHud` à la plus haute priorité (il pousse la matrice et la rend), le dessin de Fordix à la priorité par
   défaut, l'habillage des boutons à `LOW` (après que chaque module a ajouté les siens).
6. **Pur d'abord** : tout calcul sans Minecraft (seuils, courbes, formes, codes de partage, tables) vit en Java pur dans `hud`,
   `gui/layout` ou `gui/anim`, avec un test JUnit.
7. **Le serveur et le client ne se mélangent pas** : le code serveur ne référence aucune classe client ; le client est appelé par
   `DistExecutor`.
8. **Les thèmes** : une couleur nouvelle de structure s'ajoute à `Theme` et `FordixPalette`, pas à un écran.

## 9. Contenu et langue

- Tous les textes passent par `Tr` et les fichiers `assets/fordix/lang/en_us.json` et `fr_fr.json` (clés `fordix.*`). Une clé s'ajoute aux
  deux fichiers. Jamais de texte en dur.
- Ton : direct, sans formule de politesse, verbe d'action pour un bouton (« Envoyer », « Réinitialiser l'onglet »), nom court pour un
  titre. Pas de point final dans un libellé ; point dans une phrase d'aide.
- Longueur : un libellé de bouton tient dans 100 px, un titre de ligne de réglage dans 160 px, une aide dans 220 px. Au-delà on
  raccourcit le texte (jamais la police).
- Capitales : seulement par `Glass.title` et `Glass.label`, qui mettent en capitales à l'affichage ; les clés restent en casse normale.
- Nombres : séparateur du jeu, unités collées par une espace (« 31 ms »), pourcentages sans espace insécable.

## 10. Accessibilité et confort

- **Contraste** : le texte courant sur le verre garde un rapport d'au moins 4,5:1 (`TEXT` `#9FB3C0` sur `#131A22` : 7,7:1). Le texte
  atténué (`Glass.dim`) ne porte jamais seul une information.
- **Couleur** : un état critique s'accompagne toujours d'un mot, d'une forme ou d'un chiffre (« Vie critique », pastille, valeur).
- **Taille** : aucun texte plus petit que la police du jeu ; l'échelle d'interface du joueur est respectée partout.
- **Opacité** : l'opacité des panneaux de HUD est un réglage joueur (`hudOpacity`), le mode compact aussi (`hudCompact`).
- **Clavier** : tout écran Fordix se parcourt au clavier (Tab, Entrée, Échap) ; les touches du mod sont des `FordixKeyMapping`
  réglables dans Options > Contrôles.
- **Mouvement** : voir la section 4 ; à terme une option « réduire les animations ».
- **Daltonisme** : les alertes ne reposent jamais sur le rouge seul (texte « Vie critique », pastille).

## 11. Performance

Le HUD est dessiné à chaque image, donc :

- Uniquement des `fill` et des `fillGradient` : pas de shader, pas de texture ajoutée, pas de flou (le flou du mockup n'existe pas en jeu).
- Aucune allocation dans une boucle de dessin chaude (listes, chaînes construites image par image) quand elle peut se mettre en cache
  (`ChatLog`, `ContainerMemory`).
- Un panneau coûte environ une dizaine de `fill`. Un écran plein reste sous 400 `fill`. Au-delà, regrouper ou mettre en cache.
- Les signaux du jeu (cible, situation) se calculent à intervalle (toutes les 5 ticks), pas à chaque image.
- Mesurer avec le test en jeu (`DevTest`) : les captures doivent se produire sans à-coup visible sur un rendu logiciel.

## 12. Serveur et Astral-launcher

- Le serveur Astral Nexus impose les règles du HUD (`[hud]`), le protocole réseau (`PROTOCOL_VERSION`, actuellement 11), et peut imposer la
  liste des onglets Guides. Un changement de format de paquet change la version de protocole : le serveur ne prend le nouveau jar qu'à
  son redémarrage, les joueurs mettent à jour en même temps.
- **Astral-launcher cache l'écran titre et la liste des mondes** : ces deux écrans ne sont ni conçus ni testés (section 2.2).
- Un élément que le serveur interdit n'est jamais dessiné ; un élément requis est dessiné même si le joueur l'a masqué.

## 13. Qualité : tests et définition de « terminé »

Une production d'interface est **terminée** quand :

1. Elle utilise uniquement les composants de la section 5 (ou ajoute le sien à ce document avec son API).
2. Tous ses textes sont dans les deux fichiers de langue et tiennent aux largeurs de la section 9.
3. Elle est visible et correcte à l'échelle d'interface 2 (854 x 480) et reste utilisable à l'échelle 3 et 4.
4. `./gradlew build` passe (compilation et tests JUnit).
5. Un `Step` du `DevTest` la parcourt et en capture l'état (voir « Test en jeu » dans `CLAUDE.md`) ; les captures ont été regardées, pas
   seulement produites.
6. Sa logique sans Minecraft a un test JUnit.
7. Elle ne casse aucun écran voisin (vérifier les captures des écrans qui partagent la primitive modifiée).
8. `CLAUDE.md` et ce document sont à jour.

Check-list de revue visuelle : alignements sur la grille de 2 px ; un seul bouton `PRIMARY` ; aucun texte tronqué ; liseré d'accent à
gauche des panneaux ; états de survol, focus, inactif présents ; pas de bord biseauté ; aucune couleur de structure en dur.

## 14. Décisions et dette

Décisions prises (à ne pas rouvrir sans raison écrite) :

| N° | Décision | Raison |
|---|---|---|
| D1 | Les primitives de Voidframe 1 (`Panel.draw`, `VoidframeDraw.well`, `FordixButton`, `StandIn`) sont **redirigées** vers `Glass` plutôt que supprimées. | Un seul changement bascule tous les écrans, sans risque d'oubli. |
| D2 | L'ouverture des conteneurs **glisse** de 8 px au lieu de zoomer. | Le zoom flouait le texte et les cases. |
| D3 | La rareté d'un objet est un **liseré sous l'objet**, pas un cadre. | Le cadre plein masquait l'objet et se confondait avec la sélection. |
| D4 | Les boutons du jeu sont remplacés par classe exacte `Button` ; les boutons à icône ne le sont que dans nos conteneurs. | Un `ImageButton` d'un autre écran est une icône (langue, accessibilité) qui doit garder sa texture. |
| D5 | Le menu pause est **habillé** (boutons réarrangés), pas réécrit. | Realms, LAN, déconnexion et les boutons d'autres mods continuent de marcher. |
| D6 | L'écran de mort est une **sous-classe** de `DeathScreen`. | La confirmation, le délai et le mode hardcore restent ceux du jeu. |
| D7 | Les écrans titre et liste des mondes ne sont **pas refaits**. | Cachés par Astral-launcher. |
| D8 | Les overlays remplacés (scoreboard, barre d'action) annulent celui du jeu seulement quand ils ont du contenu. | Le message « disque en lecture » du jeu continue de s'afficher. |
| D10 | Les conteneurs de la maquette ont une **disposition large** (cases déplacées côté client) et une disposition compacte de repli. | La maquette impose ses positions ; les petits écrans ne peuvent pas la contenir. Le repli garde JEI, le livre de recettes et les autres mods intacts. |
| D11 | Le menu pause **ne remet pas** la ligne Avancements (retirée en 0.1.59, décision du propriétaire) et masque « Donner un avis » et « Signaler un bug » du jeu. | Les avancements restent à la touche L et dans l'inventaire ; les deux liens vers Mojang ne servent pas sur Astral Nexus. La maquette montrait la ligne : c'est la décision du propriétaire qui prévaut. |
| D12 | **Toutes les tailles d'interface** : le jeu ne descend jamais sous 320 x 240 pixels d'interface (ses 6 niveaux d'échelle sur les écrans de 1280 x 720 à 4K donnent de 320 x 240 à plus de 1900 x 1000). Les écrans Fordix déclarent un minimum lisible (`FordixScreen.minWidth/minHeight`) ; en dessous, l'échelle de l'interface est abaissée le temps de l'écran (`fitInterface`, événement `ScreenEvent.Init.Pre`, échelle rendue dans `removed`). Les écrans du jeu ont une disposition compacte (D10) et des lignes sans détail quand la place manque (options). | Un écran ne doit jamais sortir de la fenêtre ni se chevaucher. Test : `FORDIX_DEVTEST_SIZE=LxH` et `FORDIX_DEVTEST_ONLY=écrans` du DevTest, tailles 320x240, 427x240, 640x360, 854x480, 1920x1080. |
| D13 | Les **sous-écrans d'options du jeu** (vidéo, sons, souris, chat, accessibilité, langue, touches, contrôles, apparence, statistiques) sont posés sur un panneau de verre avec leur titre en capitales d'accent (`SubOptionsSkin`) ; leurs listes cessent de tapisser la terre du jeu (`setRenderBackground(false)`), sans remplacer les écrans. Restent en style du jeu : packs de ressources, crédits, télémétrie, création de monde, multijoueur (dette). | Même langage que le menu d'options, aucun écran du jeu remplacé, donc compatible avec les autres mods. |
| D14 | La **rareté d'un objet se lit sur le fond de sa case** (`RarityFrames`, chiffres dans `RarityMotion`, testés) : teinte qui monte du peu commun au légendaire (le commun reste le puits sombre), sous l'objet ; sur l'épique un voile violet et un reflet lent, sur le légendaire un cadre doré qui tourne, des étincelles, un reflet vif et une respiration ; reflet unique au survol, éclair blanc à l'arrivée d'un objet. L'objet n'est jamais modifié. Remplace le filet lumineux sous l'objet. | Choisi par le propriétaire parmi huit variantes : lisible de loin, et le légendaire se reconnaît sans la couleur. |
| D15 | Le livre de recettes est dessiné en verre (`GlassBook` : sous-classes du livre du jeu installées dans les écrans par réflexion), aussi haut que le conteneur voisin ; le jeu garde la logique (recettes, onglets, pages, clics, recette fantôme). | Le livre vanilla a une taille fixe de 147 x 166 et ne peut pas s'aligner sur le panneau ; seul le menu des variantes de recette garde le dessin du jeu. |
| D16 | Métier à tisser, cartographie et inventaire créatif sont en verre : on vide le corps de leur texture et on dessine le panneau, les puits et les onglets ; les sprites utiles (motifs, icônes, curseur) restent ceux du jeu. | Ces écrans mêlent logique et dessin du jeu (motifs de bannière, carte, onglets) qu'on ne peut pas refaire sans les réécrire ; vider le fond suffit à les mettre en verre. |
| D17 | L'éditeur de HUD offre un aperçu de la disposition à huit tailles d'interface (320 x 240 à 1920 x 1080), avec les chevauchements et les débordements marqués. | Un joueur voit la disposition à sa taille d'écran, pas à celle de l'admin. |
| D9 | Le thème fournit les couleurs de structure ; le sémantique est fixe. | Un thème ne doit pas rendre le rouge de danger illisible. |

Dette connue (à traiter dans l'ordre) :

1. Sous-titres sonores (`SUBTITLES`) : la liste est privée à `SubtitleOverlay` ; elle reste vanilla. F3 reste vanilla (outil de diagnostic).
2. Titre à l'écran : lu par type et ordre de déclaration dans `Gui` (`TitleHud`, repli sur le titre du jeu si la disposition diffère) ; à revérifier à chaque version du jeu.
3. Options > Contrôles, Vidéo, Sons : seuls les boutons et le fond sont habillés ; les listes de réglages gardent leur forme.
4. Éditeur de HUD : une notice à la première ouverture (`firstOpenHint`) mais pas d'assistant pas à pas. La prévisualisation multi-résolutions existe depuis la 0.1.78 (`HudPreview`, bouton Aperçu).
5. Champs `EditBox` du chat et des commandes : le chat de Voidframe a le sien, la ligne de commande du jeu garde le champ vanilla.
6. Fiche d'objet : les barres de caractéristiques de la maquette ne sont pas encore dessinées (liste de chiffres alignés).
7. (Soldée en 0.1.78 : créatif, métier à tisser et cartographie sont en verre.)

## 15. Voidframe 3 : ce qu'on prépare

Voidframe 3 part de ce document : on ne jette pas Voidframe 2, on le fait évoluer par versions mineures (2.1, 2.2…) tant que le
langage visuel tient, et on ouvre la version 3 quand l'un de ces critères est atteint.

**Critères d'ouverture de Voidframe 3**

- Le jeu change de version majeure et impose un nouveau moteur d'interface (1.21 : composants et rendu des écrans réécrits).
- Une identité visuelle nouvelle (nouvelle matière que le verre, nouvelle structure de panneau) est décidée.
- Le nombre de dérogations à ce document dépasse ce que la check-list tolère.

**Points d'extension déjà prévus**

| Sujet | Idée | Pourquoi pas déjà |
|---|---|---|
| Réduire les animations | option qui met toutes les durées de la section 4 à zéro sauf la pulsation, via un seul coefficient dans `HudTokens` | pas d'option joueur demandée |
| Densité d'interface | trois densités (compacte, normale, aérée) qui changent les hauteurs de la section 3.4 | demande à reprendre chaque écran |
| Flou d'arrière-plan | verre dépoli derrière les panneaux | exige un shader et un tampon de rendu, coût de performance non mesuré |
| Fondu d'ouverture avec transparence | alpha global d'un écran | `GuiGraphics.fill` ignore `setShaderColor` ; nécessite un tampon intermédiaire |
| Police | police bitmap propre à Fordix | casserait la cohérence avec les mods ; à étudier avec un pack de ressources |
| Éditeur de thème en jeu | accent et teinte choisis dans les réglages | existe en fichier (`fordix-client.toml`) |
| Liste déroulante, curseur de réglage, sélecteur de couleur | composants manquants de la section 5 | aucun écran Fordix n'en a encore eu besoin |
| Écrans titre et mondes | si Astral-launcher disparaît | décision D7 |

**Règle de passage de V2 à V3** : toute production nouvelle d'après la date d'ouverture suit le document V3 ; les écrans V2 existants ne
sont repris qu'écran par écran, avec leur capture avant et après, jamais en bloc.

## Annexe A. Tokens numériques

| Token | Valeur | Source |
|---|---|---|
| Seuil bas / critique | 30 % / 15 % | `HudTokens.LOW` / `CRITICAL` |
| Fondu de situation | 200 ms | `HudTokens.FADE_MS` |
| Pulsation | 1 000 ms | `HudTokens` |
| Reflet de barre | cycle 3 200 ms, départ à 2 400 ms | `HudTokens.glint` |
| Notification | 200 / 2 200 / 300 ms | `HudTokens.TOAST_*` |
| Survol | 120 ms | `Glass.HOVER_MS` |
| Ouverture de conteneur | 160 ms, 8 px | `VoidframeChrome` |
| Halo de case | 150 ms | `VoidframeChrome` |
| Opacité du corps d'un écran | 238 / 255 | `Glass.panel` |
| Opacité d'un panneau de HUD | choix du joueur | `hudOpacity` |
| Bord d'un bouton | blanc 11 % (accent clair 75 % pour `PRIMARY`) | `Glass.button` |
| Hauteurs | bouton 24, principal 26 à 28, ligne de réglage 28, champ 16 à 22, pastille 11 | section 3.4 |
| Marges | 12 / 14 px (7 px infobulle) | section 3.4 |

## Annexe B. État des écrans

Validé en jeu par l'utilisateur sur la version 0.1.77 (tailles d'écran et d'interface comprises) : tous les écrans ci-dessous marqués « fait » sont conformes. Restent hors de la validation : les écrans seulement recolorés ou habillés, et la dette listée plus haut.

| Écran | État Voidframe 2 |
|---|---|
| HUD (vie, faim, jauges, barre d'objets, panneaux, cibles, effets, chat, liste Tab, notifications, scoreboard, barre d'action) | fait |
| Inventaire, coffres, shulker, entonnoir, distributeur, établi, fourneaux | fait |
| Enclume, meule, alambic, balise, enchantement, villageois, forge, cheval | fait (glass et primitives) |
| Tailleur de pierre | fait (choix en puits et lignes) |
| Métier à tisser, cartographie, créatif | fait (verre, corps de texture vidé) |
| Infobulle, rareté, durabilité | fait |
| Réglages, éditeur de HUD | fait |
| Administration, guides, wiki, fiches d'objet et de créature, livre de bienvenue, avancements, panels générés | fait |
| Roue des animaux | fait |
| Menu pause, écran de mort | fait |
| Autres écrans du jeu (options, statistiques, multijoueur, packs…) | boutons et fond habillés |
| Écran titre, liste des mondes | **hors périmètre** (Astral-launcher) |
| Titre à l'écran | fait (`TitleHud`) |
| Sous-titres sonores, F3 | vanilla (dette 1) |

## Annexe C. Glossaire

- **Verre** : la matière d'un panneau (section 3.1).
- **Liseré** : la bande d'accent sur le bord gauche d'un panneau.
- **Puits** : zone en creux (case d'objet, piste de jauge).
- **Accent** : la couleur du thème qui désigne l'actif ou le principal.
- **Situation** : l'état du jeu qui décide de la disposition du HUD (exploration, combat, minage, boss, monture).
- **Habiller** : changer l'apparence d'un écran du jeu sans le remplacer.
- **Classe exacte** : on ne remplace un écran que si sa classe est celle du jeu, pas celle d'une sous-classe d'un autre mod.
- **Pur** : code sans dépendance à Minecraft, donc testable en JUnit.
