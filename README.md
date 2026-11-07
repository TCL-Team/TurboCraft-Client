# TurboCraft-Client

Minecraft: Java Edition ke liye **TurboCraft Client** — Android-first client mod (Fabric),
jo TurboCraft Launcher ke saath aur Pojav / Amethyst / Zalith jaise launchers me bhi chalta hai.

> Not affiliated with Mojang or Microsoft. Is repo me Minecraft ke game files nahi hain.

## Repo me kya hai

```
docs/
├── android-java-edition-engine-notes.md   # Android pe Java Edition kaise chalta hai (Pojav/Amethyst source study)
└── client-strategy.md                     # launcher vs client, roadmap, licensing, phone-only dev workflow

client/                                    # Fabric mod (Minecraft 1.20.1)
├── src/main/java/com/turbocraft/client/
├── src/main/resources/fabric.mod.json
├── mods-manifest.json                     # launcher auto-install ke liye recommended mods
└── build.gradle

.github/workflows/build.yml                # push pe jar build + artifact + rolling release
```

## Feature status (v0.1.0)

- FPS / CPS / coordinates / ping HUD
- Keystrokes (WASD + space)
- Right Shift in-game menu, toggles with auto-save config
- Client-side only — vanilla servers (Hypixel, etc.) safe

## Build

Har push pe GitHub Actions jar build karta hai:
**Actions → Build TurboCraft Client → Artifacts → `turbocraft-client-jar`**,
ya **Releases → latest-build**.

Local:
```bash
cd client && ./gradlew build
```

## Quick start (phone)

1. Launcher me **Minecraft 1.20.1 + Fabric** profile banao.
2. `mods/` me daalo: `turbocraft-client-0.1.0.jar` + **Fabric API** (+ Sodium recommended).
3. Java **17**, RAM ~2–2.5 GB, renderer MobileGlues/Zink (Snapdragon) ya GL4ES (Mali).
4. Game me **Right Shift** → TurboCraft menu.

Details: [`docs/client-strategy.md`](docs/client-strategy.md),
[`client/README.md`](client/README.md).

## License

Mod code: MIT. (PojavLauncher / Amethyst LGPL-3.0 code is repo me include nahi hai — unhe fork karne
ka plan ho to `docs/client-strategy.md` ka licensing section padho.)
