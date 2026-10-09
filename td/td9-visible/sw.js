// sw.js : App Shell en cache, Cache First
const CACHE_NAME = "pwa-notes-v1";
const APP_SHELL = ["/", "/index.html", "/app.js", "/style.css",
  "/offline.html", "/manifest.json", "/icons/icon-192.png", "/icons/icon-512.png",
  "https://cdn.jsdelivr.net/npm/idb@8/build/umd.js"];

self.addEventListener("install", (e) =>
  e.waitUntil(caches.open(CACHE_NAME).then((c) => c.addAll(APP_SHELL))));

self.addEventListener("activate", (e) =>
  e.waitUntil(caches.keys().then((keys) => Promise.all(
    keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))));

self.addEventListener("fetch", (e) =>
  e.respondWith(caches.match(e.request).then((cached) =>
    cached || fetch(e.request).catch(() =>
      e.request.mode === "navigate"
        ? caches.match("/offline.html") : undefined))));
