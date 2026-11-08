# TurboCraft Client (Fabric mod)

Client-side Fabric mod for **Minecraft 1.20.1**: HUD (FPS / CPS / coords / ping / keystrokes) and an
in-game menu (Right Shift). Pure client side — safe on vanilla servers.

> Not affiliated with Mojang or Microsoft. Contains no Minecraft game files.

---

## Kya karta hai (v0.1.0)

| Feature | Status |
|---|---|
| FPS counter | ✅ |
| CPS counter (left \| right) | ✅ mouse mixin |
| Coordinates | ✅ |
| Ping | ✅ multiplayer only |
| Keystrokes (WASD + space) | ✅ |
| Right Shift menu with toggles | ✅ |
| Config persistence (`config/turbocraft.json`) | ✅ |
| Cosmetics / capes | ⏳ phase 4 |
| Freelook, zoom, toggle sprint | ⏳ phase 5 |

---

## Phone-only workflow (no PC) — README ke liye

1. Code isi repo me hai (`client/`).
2. `git push` karte hi **GitHub Actions** build chal jata hai.
3. Phone pe GitHub app kholo → **Actions** tab → latest "Build TurboCraft Client" run →
   **Artifacts** → `turbocraft-client-jar` download.
   (Ya **Releases** tab se `latest-build` ka `turbocraft-client-0.1.0.jar` seedha le lo.)
4. Jar ko launcher ke game folder me `mods/` me daalo:
   - TurboCraft Launcher → Files/Game directory → `mods/`
   - Pojav / Amethyst / Zalith → bhi same `mods/` folder
5. Require: **Fabric Loader** + **Fabric API**. Profile me Fabric API mod bhi hona chahiye.

### Zaroori settings phone pe
- Java runtime: **17** (1.20.1 ke liye)
- RAM: 2–2.5 GB (zyada dene se Android process kill karta hai)
- Renderer: **MobileGlues** ya **Zink** (Snapdragon), **GL4ES** (Mali/purane phones)
- Render distance 4–8 chunks

---

## Local build (agar PC mile to)

```bash
cd client
./gradlew build
# output: client/build/libs/turbocraft-client-0.1.0.jar
```

---

## Code structure

```
client/src/main/java/com/turbocraft/client/
├── TurboCraftClient.java        # entrypoint, keybind, tick hook
├── config/TurboCraftConfig.java # config/turbocraft.json (GSON, no deps)
├── event/InputTracker.java      # CPS ring buffers, ping lookup
├── gui/ClientMenuScreen.java    # Right Shift menu
├── hud/HudRenderer.java         # HUD drawing (vanilla DrawContext only)
└── mixin/MouseMixin.java        # click tap for CPS
```

Design rules for this mod (Android-first):
- **Zero extra dependencies** beyond Fabric API. Every dependency is another thing that can fail
  inside the Pojav/Amethyst JVM.
- No custom textures/shaders → low VRAM pressure.
- `require = 0` on mixins → a mapping change degrades one feature instead of crashing the game.

---

## Version support

| Version | Status |
|---|---|
| 1.20.1 (Fabric) | 🚧 in progress (this branch) |
| 1.21.x (Java 21) | ⏳ next |
| 1.8.9 (Legacy Fabric) | ⏳ later — biggest PvP audience in India |

Minecraft's code moves between versions, so each target gets its own branch with the same HUD logic.
