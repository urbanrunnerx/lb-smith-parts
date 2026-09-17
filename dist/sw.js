const CACHE='lbsmith-parts-1.0.0';
const FILES=['./','./index.html','./style.css','./app.js','./core.js','./vin.js','./vin-ui.js','./catalog.json','./manifest.webmanifest','./assets/fonts.css','./assets/dealer-logo.jpg','./assets/icon.svg','./install.html'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(FILES)).then(()=>self.skipWaiting())));
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('lbsmith-parts-')&&k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{const url=new URL(event.request.url);if(event.request.method!=='GET'||url.origin!==self.location.origin||!url.pathname.startsWith(new URL(self.registration.scope).pathname))return;event.respondWith(caches.match(event.request).then(cached=>cached||fetch(event.request)));});
