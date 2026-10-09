// app.js — point de départ des TD du Jour 2
// Fourni : l'interface, le journal, l'état réseau, l'enregistrement du Service Worker
//          et tous ses événements de cycle de vie, tracés dans la console et dans la page.
// À écrire : tout ce qui est marqué TODO.

// ---------------------------------------------------------------------------
// Journal : console + liste dans la page. Le Service Worker y écrit aussi
// (il envoie ses messages par postMessage, voir plus bas).
// ---------------------------------------------------------------------------
const journal = document.querySelector("#journal");

function log(message, source = "page") {
  const stamp = new Date().toLocaleTimeString("fr-FR", { hour12: false });
  console.log(`[${source}] ${message}`);
  const li = document.createElement("li");
  li.className = source;
  li.textContent = `${stamp}  [${source}] ${message}`;
  journal.append(li);
  journal.scrollTop = journal.scrollHeight;
}

document.querySelector("#btn-clear").addEventListener("click", () => journal.replaceChildren());

// ---------------------------------------------------------------------------
// État réseau (chapitre 3) : fourni. `navigator.onLine` dit « une interface
// est connectée », pas « internet répond » (voir la slide « connecté, mais sans internet »).
// ---------------------------------------------------------------------------
const network = document.querySelector("#network");

function showNetwork() {
  network.textContent = navigator.onLine ? "en ligne" : "hors ligne";
  network.classList.toggle("off", !navigator.onLine);
  log(`réseau : ${navigator.onLine ? "en ligne" : "hors ligne"}`);
}
window.addEventListener("online", showNetwork);
window.addEventListener("offline", showNetwork);
showNetwork();

// ---------------------------------------------------------------------------
// Service Worker : enregistrement et cycle de vie (Jour 1, chapitre 4). Fourni.
// Tout est tracé : à lire dans le journal au premier chargement, puis après
// une modification de sw.js (CACHE_NAME en v2, par exemple).
// ---------------------------------------------------------------------------
function describe(worker) {
  // worker.state : installing → installed → activating → activated (ou redundant)
  return worker ? worker.state : "aucun";
}

if ("serviceWorker" in navigator) {
  log(`controller au chargement : ${navigator.serviceWorker.controller ? "oui" : "non (première visite ou Ctrl+Maj+R)"}`);

  navigator.serviceWorker.register("/sw.js").then((reg) => {
    log(`register() → scope ${reg.scope}`);
    log(`installing: ${describe(reg.installing)} · waiting: ${describe(reg.waiting)} · active: ${describe(reg.active)}`);

    // Un nouveau sw.js a été trouvé : on suit ses changements d'état
    reg.addEventListener("updatefound", () => {
      const worker = reg.installing;
      log(`updatefound : un nouveau Service Worker est en cours d'installation`, "warn");
      worker.addEventListener("statechange", () => {
        log(`statechange → ${worker.state}`);
        if (worker.state === "installed" && navigator.serviceWorker.controller) {
          log(`nouvelle version en attente (waiting) : fermer tous les onglets, ou skipWaiting()`, "warn");
        }
      });
    });

    document.querySelector("#btn-update").addEventListener("click", () => {
      log(`registration.update() : le navigateur recompare sw.js avec le serveur`);
      reg.update();
    });
  }).catch((err) => log(`register() a échoué : ${err.message}`, "warn"));

  // Le SW qui contrôle la page a changé (une nouvelle version vient de s'activer)
  navigator.serviceWorker.addEventListener("controllerchange", () => {
    log(`controllerchange : la page est maintenant contrôlée par ${describe(navigator.serviceWorker.controller)}`, "warn");
  });

  // Messages envoyés par le Service Worker (ses propres logs, voir sw.js)
  navigator.serviceWorker.addEventListener("message", (e) => log(e.data, "sw"));
} else {
  log("Service Worker non supporté par ce navigateur", "warn");
}

// ---------------------------------------------------------------------------
// Données : IndexedDB (chapitre 2). À écrire.
// Les trois fonctions ci-dessous sont appelées par l'interface, qui est fournie.
// Tant qu'elles ne sont pas écrites, les notes vivent dans un tableau en mémoire
// et disparaissent au rechargement : c'est le point de départ du TD 8.
// ---------------------------------------------------------------------------
let memory = []; // à supprimer une fois IndexedDB en place

// TODO TD 8 : ouvrir la base « pwa-notes », version 1, avec un store « notes »
//            (keyPath "id", autoIncrement) — voir la slide « TD 8 : aide ».
// const db = await idb.openDB("pwa-notes", 1, { upgrade(d) { … } });

async function getNotes() {
  // TODO TD 8 : return db.getAll("notes");
  return memory;
}

async function addNote(text) {
  // TODO TD 8 : await db.add("notes", { text, createdAt: Date.now() });
  memory.push({ id: Date.now(), text, createdAt: Date.now() });
  log(`note ajoutée (en mémoire seulement : TODO IndexedDB)`, "warn");
}

async function deleteNote(id) {
  // TODO TD 8 : await db.delete("notes", id);
  memory = memory.filter((n) => n.id !== id);
}

// ---------------------------------------------------------------------------
// Interface : fournie. Rendu par création de nœuds DOM (jamais innerHTML
// avec une donnée saisie : chapitre 9, XSS).
// ---------------------------------------------------------------------------
const list = document.querySelector("#list");
const count = document.querySelector("#count");
const form = document.querySelector("#form");
const input = document.querySelector("#text");

async function render() {
  const notes = await getNotes();
  count.textContent = `${notes.length} note${notes.length > 1 ? "s" : ""}`;
  list.replaceChildren(
    ...notes.map((note) => {
      const li = document.createElement("li");
      const span = document.createElement("span");
      span.textContent = note.text;
      const btn = document.createElement("button");
      btn.type = "button";
      btn.textContent = "Supprimer";
      btn.addEventListener("click", async () => {
        await deleteNote(note.id);
        log(`note ${note.id} supprimée`);
        render();
      });
      li.append(span, btn);
      return li;
    }),
  );
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  await addNote(input.value.trim());
  input.value = "";
  render();
});

render();

// ---------------------------------------------------------------------------
// Emplacements pour les TD suivants (mêmes fichiers, même origine : un port par TD)
// ---------------------------------------------------------------------------
// TODO TD 10 (Background Sync) : store « outbox », registration.sync.register("sync-outbox"),
//            repli window → "online" quand "sync" n'est pas dans la registration.
// TODO TD 11 (Capabilities) : un bouton par API (chapitre 6), permission demandée au clic,
//            jamais au chargement ; navigator.permissions.query pour l'état.
