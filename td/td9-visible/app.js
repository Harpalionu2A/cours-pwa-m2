// app.js : toute l'interface est créée ici
const app = document.querySelector("#app");
app.innerHTML = `
  <h1>Notes</h1>
  <p id="offline" hidden>Hors ligne</p>
  <form id="form"><input id="text" required> <button>Ajouter</button></form>
  <p id="count"></p>
  <ul id="list"></ul>`;

const db = await idb.openDB("pwa-notes", 1, {
  upgrade(d) {
    d.createObjectStore("notes", { keyPath: "id", autoIncrement: true });
  },
});

async function render() {
  const notes = await db.getAll("notes");
  document.querySelector("#count").textContent = `${notes.length} note${notes.length > 1 ? "s" : ""}`;
  document.querySelector("#list").innerHTML = notes
    .map((n) => `<li>${n.text} <button data-id="${n.id}">Supprimer</button></li>`)
    .join("");
}

document.querySelector("#form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const input = document.querySelector("#text");
  await db.add("notes", { text: input.value, createdAt: Date.now() });
  input.value = "";
  render();
});

document.querySelector("#list").addEventListener("click", async (e) => {
  if (!e.target.dataset.id) return;
  await db.delete("notes", Number(e.target.dataset.id));
  render();
});

const offline = document.querySelector("#offline");
const showOffline = () => (offline.hidden = navigator.onLine);
window.addEventListener("online", showOffline);
window.addEventListener("offline", showOffline);
showOffline();

if ("serviceWorker" in navigator) navigator.serviceWorker.register("/sw.js");
render();
