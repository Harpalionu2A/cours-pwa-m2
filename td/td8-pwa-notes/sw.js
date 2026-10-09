// sw.js — point de départ des TD du Jour 2
// Fourni : le cycle de vie tracé (install, activate, fetch, message) et un cache
//          volontairement incomplet. À écrire : tout ce qui est marqué TODO.
//
// Chaque message passe par log() : console du Service Worker (DevTools →
// Application → Service Workers → « inspect ») ET journal de la page.

const CACHE_NAME = "pwa-notes-v1"; // changer la version pour déclencher install → activate

// L'App Shell : ce qui doit s'afficher sans réseau. Volontairement incomplet.
// TODO TD 8 : compléter (app.js, offline.html, manifest.json, les icônes, la librairie idb).
const APP_SHELL = ["/", "/index.html", "/style.css"];

// ---------------------------------------------------------------------------
// log : console + toutes les pages contrôlées (elles l'affichent dans leur journal)
// ---------------------------------------------------------------------------
async function log(message) {
  console.log(`[SW ${CACHE_NAME}] ${message}`);
  const pages = await self.clients.matchAll({ includeUncontrolled: true });
  for (const page of pages) page.postMessage(message);
}

// ---------------------------------------------------------------------------
// install : une fois par version de sw.js. On remplit le cache.
// Si une URL de APP_SHELL échoue, addAll rejette et l'installation échoue :
// à observer en ajoutant une URL fausse.
// ---------------------------------------------------------------------------
self.addEventListener("install", (event) => {
  log(`install : nouvelle version, mise en cache de ${APP_SHELL.length} fichiers`);
  event.waitUntil(
    caches
      .open(CACHE_NAME)
      .then((cache) => cache.addAll(APP_SHELL))
      .then(() => log(`install : cache « ${CACHE_NAME} » rempli`))
      .catch((err) => log(`install : ÉCHEC (${err.message}) — une URL de APP_SHELL est introuvable ?`)),
  );
  // TODO TD 8 (optionnel, Jour 1 TD 5) : self.skipWaiting() pour ne pas rester en attente.
});

// ---------------------------------------------------------------------------
// activate : la version devient active. C'est ici qu'on nettoie les anciens caches.
// ---------------------------------------------------------------------------
self.addEventListener("activate", (event) => {
  log(`activate : cette version prend le contrôle`);
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      log(`activate : caches présents → ${keys.join(", ") || "aucun"}`);
      // TODO TD 8 : supprimer tous les caches dont le nom n'est pas CACHE_NAME
      //             (caches.delete), puis vérifier dans Application → Cache Storage.
      await self.clients.claim(); // contrôler tout de suite les pages déjà ouvertes
      log(`activate : clients.claim() — les pages ouvertes sont contrôlées`);
    })(),
  );
});

// ---------------------------------------------------------------------------
// fetch : chaque requête de la page passe ici. Fourni : Cache First, avec trace.
// ---------------------------------------------------------------------------
self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  const short = url.origin === self.location.origin ? url.pathname : url.href;

  event.respondWith(
    (async () => {
      const cached = await caches.match(event.request);
      if (cached) {
        log(`fetch ${short} → cache`);
        return cached;
      }
      try {
        const response = await fetch(event.request);
        log(`fetch ${short} → réseau (${response.status})`);
        // TODO TD 8 (optionnel) : mettre en cache ce qui vient d'être chargé
        //             (response.clone(), chapitre 1 « Cloner les réponses »).
        return response;
      } catch (err) {
        log(`fetch ${short} → ÉCHEC réseau, rien en cache (mode ${event.request.mode})`);
        // TODO TD 8 : si event.request.mode === "navigate", répondre caches.match("/offline.html")
        throw err;
      }
    })(),
  );
});

// ---------------------------------------------------------------------------
// message : la page peut parler au Service Worker (postMessage). Fourni.
// ---------------------------------------------------------------------------
self.addEventListener("message", (event) => {
  log(`message reçu de la page : ${JSON.stringify(event.data)}`);
  // TODO TD 8 (optionnel) : if (event.data === "skipWaiting") self.skipWaiting();
});

// TODO TD 10 (Background Sync) : self.addEventListener("sync", …) avec e.tag === "sync-outbox" ;
//            idb dans le SW : importScripts("https://cdn.jsdelivr.net/npm/idb@8/build/umd.js").
// TODO TD 11 (Capabilities) : rien côté SW ; les notifications locales via
//            self.registration.showNotification() (chapitre 5).
