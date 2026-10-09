# TD 9 — Visible : la PWA lisible par Google et par les agents IA

## L'idée en une phrase

**Un robot (Googlebot, GPTBot, ClaudeBot) lit le HTML que le serveur envoie. Il n'exécute pas `app.js`.** Tout ce que le JavaScript construit après le chargement est invisible pour lui.

## Lancer

```bash
cd td9-visible
npx serve -l 3009          # port 3009 : une origine distincte du TD 8
```

Ouvrir http://localhost:3009 : l'app fonctionne. Puis, dans un autre terminal :

```bash
curl -s http://localhost:3009/ | grep -c "<li[ >]"
```

Résultat : **0**. `curl` reçoit le HTML brut, exactement comme un robot : `index.html` ne contient qu'un `<div id="app"></div>`, tout le reste est créé par `app.js`. Pour un humain la page est pleine ; pour un robot elle est vide. C'est le point de départ.

Objectif du TD : faire passer cette commande de **0 à 3**, puis ajouter ce que les robots et les réseaux sociaux lisent.

## Étape 1 — Le contenu dans le HTML (l'essentiel)

Dans `index.html`, remplacer `<div id="app"></div>` par l'interface écrite en dur :

```html
<main>
  <h1>PWA Notes</h1>
  <p>Carnet de notes installable, utilisable hors ligne.</p>
  <form id="form">
    <input id="text" aria-label="Nouvelle note" required />
    <button>Ajouter</button>
  </form>
  <p id="count"></p>
  <ul id="list">
    <li data-seed>Première note : ce carnet s'ouvre en mode avion</li>
    <li data-seed>Deuxième note : les notes restent sur cet appareil</li>
    <li data-seed>Troisième note : installable depuis le navigateur</li>
  </ul>
</main>
```

Dans `app.js`, supprimer le bloc `app.innerHTML = …` : l'interface existe déjà, le script ne fait plus que la brancher. Relancer `curl` : **3**.

**Les trois notes d'exemple** sont là pour que la page ait un contenu lisible. Comme l'app affiche sa liste depuis IndexedDB, il faut les y importer **une seule fois** au premier lancement (sinon elles réapparaissent à chaque rechargement : 3, 6, 9…) :

```javascript
const seeds = [...document.querySelectorAll("#list [data-seed]")].map((li) => li.textContent.trim());
if ((await db.count("notes")) === 0)
  for (const text of seeds) await db.add("notes", { text, createdAt: Date.now() });
```

C'est ce qu'on appelle **hydrater** : le HTML porte le contenu, le script le reprend. Variante sans hydratation : garder dans le HTML uniquement du contenu statique (titre, description, mode d'emploi) et laisser la liste vide — `curl` compte alors les `<p>` plutôt que les `<li>`.

## Étape 2 — Les métadonnées du `<head>`

Ce que chaque lecteur regarde :

| Lecteur | Balise |
|---|---|
| Google, l'onglet, les favoris | `<title>`, `<meta name="description">` |
| Le lien partagé (WhatsApp, LinkedIn, Slack) | `<meta property="og:title">`, `og:description`, `og:image` (URL absolue), `og:url` |
| Les résultats enrichis | `<script type="application/ld+json">` (`@type: WebApplication`) |
| Une seule URL publique | `<link rel="canonical">` |
| Les agents IA | `<link rel="alternate" type="text/markdown" href="/notes.md">` |

Le `<head>` complet est sur la slide « TD 9 : aide 1/2 » et dans l'antisèche § 13.

## Étape 3 — Les fichiers de découverte, à la racine

| Fichier | Rôle |
|---|---|
| `robots.txt` | Qui a le droit de lire ; une ligne `Sitemap:` ; **une décision par crawler d'IA, avec sa raison en commentaire** (GPTBot, ClaudeBot, CCBot, Google-Extended…) |
| `sitemap.xml` | La liste des pages, avec `lastmod` |
| `llms.txt` | La carte du site pour les agents IA : titre, résumé, liens |
| `notes.md` | La jumelle Markdown de la page d'accueil |

Modèles sur la slide « TD 9 : aide 2/2 » et dans l'antisèche § 13.

## Étape 4 — Rien ne régresse

`index.html` a changé, mais le Service Worker du TD 8 le sert **depuis le cache** : le navigateur montre l'ancienne page alors que `curl` voit la nouvelle. Passer `CACHE_NAME` en `v2`, ajouter `notes.md` à l'App Shell, recharger, fermer tous les onglets de `localhost:3009`, rouvrir. Puis vérifier que l'app marche toujours en mode avion et que les exemples ne sont pas en double.

## Validation

```bash
curl -s http://localhost:3009/ | grep -c "<li[ >]"      # 3
curl -s http://localhost:3009/notes.md                   # le Markdown
curl -sI http://localhost:3009/robots.txt | head -1      # 200 ; idem sitemap.xml, llms.txt
```

- DevTools → Lighthouse → SEO : 90 au départ, **100** à la fin
- validator.schema.org : 0 erreur sur le JSON-LD
- Network → Offline → recharger : l'app et les notes s'affichent, sans doublon

## Piège classique

`grep -c "<li"` compte aussi `<link` : utiliser `"<li[ >]"`. Le `-A "GPTBot"` des slides change seulement le user-agent affiché au serveur ; un serveur statique répond la même chose à tout le monde.

Corrigé complet : `demos/demo-pwa-visible/`.
