# TD 8 — PWA Notes : le point de départ

Point de départ des TD du Jour 2 (TD 8, 10 et 11 ; le TD 9 « Visible » part de `TD/td9-visible/`).

## Lancer

```bash
cd TD/td8-pwa-notes
npx serve -l 3008        # un TD = un dossier = un port (une origine = une registration)
```

Ouvrir http://localhost:3008 et lire le **journal** en bas de page (les mêmes lignes sont dans la console, et celles du Service Worker dans DevTools → Application → Service Workers → inspect).

## Ce qui est fourni

| Fichier | État |
|---|---|
| `index.html`, `style.css` | complets : formulaire, liste, badge réseau, journal |
| `app.js` | interface, journal, état réseau, **enregistrement du Service Worker et tous ses événements tracés** (`register`, `updatefound`, `statechange`, `controllerchange`, `message`) ; les notes sont en mémoire → **TODO IndexedDB** |
| `sw.js` | `install`, `activate`, `fetch`, `message` tracés ; Cache First ; **App Shell incomplet, pas de nettoyage des anciens caches, pas de page hors ligne → TODO** |
| `manifest.json` | nom et une icône seulement → **TODO** (`display`, couleurs, icône 512) |
| `offline.html`, `icons/`, `serve.json` | complets |

Chaque `TODO TD 8` dans `app.js`, `sw.js` et `manifest.json` est une fonctionnalité attendue de la slide « TD 8 : PWA Notes ». Les `TODO TD 10` et `TODO TD 11` indiquent où brancher les TD suivants.

## À observer avant d'écrire quoi que ce soit

1. Premier chargement : `controller : non` → `register()` → `install` → `activate` → `controllerchange`.
2. Recharger : `controller : oui`, les `fetch … → cache` pour `/`, `/index.html`, `/style.css` ; `app.js` passe par le réseau (il n'est pas dans l'App Shell).
3. Network → Offline, cocher « Disable cache », recharger : `/` et `/style.css` viennent du cache, `app.js` et `idb` échouent (`ÉCHEC réseau`) : l'app est cassée. Ouvrir `/autre` hors ligne : erreur brute du navigateur au lieu de `offline.html`. Ce sont les premiers TODO.
4. Passer `CACHE_NAME` en `v2`, recharger : `updatefound`, `statechange → installed`, « en attente » tant qu'un onglet est ouvert (Jour 1, TD 5).

## `serve.json`

`"cleanUrls": false` est nécessaire : sans lui, `serve` redirige `/offline.html` vers `/offline`, le cache stocke la redirection et le navigateur la refuse pour une navigation.
