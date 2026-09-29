# Mise à jour automatique de Fordix sur le VPS

Même principe que le mod Astral Nexus (`Astral-Nexus-MC/Astral-launcher`) :

1. un tag `vX.Y.Z` sur ce dépôt lance `.github/workflows/release.yml`, qui publie une release
   GitHub avec `fordix-X.Y.Z.jar` et son `.sha256` ;
2. sur le VPS, `fordix-mod-sync.service` (toutes les 5 minutes) installe cette release dans
   `modpack/mods/` et retire l'ancien `fordix-*.jar`. Il réutilise le script
   `sync-astralnexus-mod.py` d'[Omneria/ALlauncher](https://github.com/Omneria/ALlauncher)
   (`scripts/vps/`), avec les paramètres de Fordix ;
3. `modpack-manifest.path` régénère le manifest : les joueurs reçoivent la nouvelle version à
   leur prochain clic sur JOUER ;
4. `sync-server-mods.py` recopie le jar dans le dossier `mods/` du serveur Minecraft.

Fordix doit être **des deux côtés** : il ouvre un canal réseau (panneaux, notifications) et
Forge refuse la connexion si le serveur et le joueur n'ont pas la même version. Il ne faut donc
pas l'ajouter à `clientOnlyMods`. Le serveur ne charge le nouveau jar qu'à son **prochain
redémarrage** : entre la mise à jour du modpack et ce redémarrage, les joueurs qui ont déjà
la nouvelle version ne peuvent pas se connecter. Redémarrer le serveur juste après la
publication (ou publier avant un redémarrage prévu).

## Installation, une seule fois

Prérequis : les scripts du mod Astral Nexus sont déjà installés (`/opt/modpack-tools/`,
`modpack-manifest.path`, et `SERVER_MODS_DIR` renseigné dans `modpack-manifest.service` pour
que `sync-server-mods.py` alimente le serveur). Voir `scripts/vps/README.md` d'ALlauncher.

Le dépôt Fordix est privé : le token GitHub du VPS doit y avoir accès. Deux possibilités :

- **modifier le token existant** : github.com > Settings > Developer settings > Fine-grained
  tokens > le token du VPS > ajouter le dépôt `Astral-Nexus-MC/Fordix` (permission
  "Contents: Read-only") ;
- **ou créer un token séparé** pour Fordix, l'enregistrer dans
  `/etc/modpack-tools/github-token-fordix` (`chmod 600`) et changer `GITHUB_TOKEN_FILE` dans
  `fordix-mod-sync.service`.

```bash
cp fordix-mod-sync.service fordix-mod-sync.timer /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now fordix-mod-sync.timer

# Premier passage tout de suite, puis vérification
systemctl start fordix-mod-sync.service
journalctl -u fordix-mod-sync.service -n 20 --no-pager
```

Tant qu'aucune release n'existe, le journal affiche "Aucune release trouvée" : c'est normal.
Même message avec une release publiée : le token n'a pas accès au dépôt Fordix.
Un jar dont l'empreinte ne correspond pas au `.sha256` de la release n'est jamais installé.
