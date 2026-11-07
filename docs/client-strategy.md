# TurboCraft Client — Strategy

> Situation: launcher (TurboCraft Launcher) ready hai. Users "client" maang rahe hain.
> Dev environment: sirf ek Android phone (Realme 13 Pro 5G) + GitHub. Koi PC nahi.

---

## 1. Users ko exactly kya chahiye jab wo "client" bolte hain

YouTube pe jo "best client for Android" videos dekhi hote hain, unme ye cheezein dikhti hain:

| Feature | Tumhare launcher me hai? | Client layer chahiye? |
|---|---|---|
| Minecraft chalna | ✅ | — |
| Higher FPS (Sodium/Lithium/Starlight) | ❌ (user manually mods daalta hai) | ✅ mod bundle |
| HUD: FPS, CPS, keystrokes, coords, ping, armor, potion timers | ❌ | ✅ **custom mod** |
| Lunar-style menu (Right Shift) | ❌ | ✅ |
| Crosshair/bosstimer/custom UI | ❌ | ✅ |
| Cape/wings cosmetics | ❌ | ✅ (backend + mod) |
| Toggle sprint, freelook, zoom | ❌ | ✅ |
| One-click "TurboCraft profile" | ✅ launcher me kar sakte ho | — |

**Nateeja:** 90% "client" = ek Fabric mod + curated mod list. Ye launcher se independently ship hota hai.

---

## 2. Teen possible raaste — aur kyun #1 chunna hai

### Option A — Fabric client mod + mod bundle ✅ (recommended)

**Kya:** `TurboCraft Client` naam ka client-side Fabric mod. HUD, settings GUI, cosmetics hooks.
Saath me ek curated mod list (Sodium, Lithium, Iris...) jo tumhara launcher auto-install kar de.

- ✅ Phone-only dev ke saath **GitHub Actions** pe build ho jaata hai (Android SDK nahi chahiye)
- ✅ Tumhare launcher me bhi chalta hai, Pojav/Zalith/FCL me bhi → **reach 10x**
- ✅ Legal clean — game files redistribute nahi, LGPL fork nahi
- ✅ 1 mod jar ~100 KB, OTA update easy (GitHub Releases se)
- ⚠️ Renderer-specific bugs (Sodium/Zink) test karne padenge asli phone pe

### Option B — Launcher ke andar hi client features daalna

Launcher me HUD inject karna, UI branding, mod auto-install.

- ✅ Ek hi APK, seamless UX
- ❌ Launcher Pojav/Amethyst fork hai → **LGPL-3.0** → tumhare changes public karne padenge
- ❌ Har feature ke liye native/Java patch, slow iteration
- ❌ Sirf tumhare launcher pe chalta hai, kisi aur pe nahi

### Option C — Lunar/Fast Client jaisa poora alag launcher

- ❌ Phone-only dev se practically impossible (Android Studio + NDK + 300MB assets + weeks)
- ❌ Fast Client/Lunar ka Android version hi nahi hai — wo PC apps hain. Jo Android pe "chalte" hain wo
  Pojav-family engine ke andar import kiye jaate hain. Tum already wahi engine rakh chuke ho.

**Faisla: Option A.** Launcher jaisa hai waisa rehne do, uske saath TurboCraft Client mod ship karo.
Launcher ka ek hi change: profile banate waqt TurboCraft jar + mods auto-download (manifest se).

---

## 3. Version strategy (tumne "multi" chuna)

Ek hi jar sab versions pe nahi chalta — Minecraft har version pe code badalta hai. Isliye:

| Phase | Target | Kyun pehle |
|---|---|---|
| v0.1 | **1.20.1 + Fabric** | Android pe sabse stable + Sodium/Iris ecosystem sabse strong, Java 17 |
| v0.2 | 1.21.x (Java 21) | Naya audience |
| v0.3 | 1.8.9 (Legacy Fabric / Forge) | Indian PvP community ka bada chunk yahi hai |

Har version ke liye alag branch/gradle config. Code shared (HUD logic), sirf rendering mappings alag.
Pehle **1.20.1 ko perfect karo** — 80% mobile Java players wahi use karte hain.

---

## 4. Roadmap

| Phase | Deliverable | Effort |
|---|---|---|
| **0** | Repo + CI green + 1 jar build | 1 din |
| **1** | HUD: FPS, CPS, keystrokes, coords, ping, armor | 2–3 din |
| **2** | Settings GUI + config file + keybinds | 1–2 din |
| **3** | Mod bundle (Sodium/Lithium/Iris) + launcher auto-install manifest | 1–2 din |
| **4** | Cosmetics (capes/wings) + Supabase backend + auth | 1–2 hafte |
| **5** | 1.21 + 1.8.9 ports, Freelook/Zoom/ToggleSprint | ongoing |

---

## 5. Legal — ye galtiyan mat karna

1. **Minecraft jar ship mat karo.** User apne Microsoft account se Mojang se download karega.
   Cracked/offline jar bundling = copyright + EULA violation, aur launcher ban ho sakta hai.
2. **Mod distribution:** Modrinth/CurseForge mods ko bundle karna unke license pe depend karta hai.
   Sodium/Lithium MIT (allowed with attribution). Iris LGPL. Har mod ka license check karo —
   behtar: **launcher runtime pe Modrinth API se download kare**, tum APK me na bharo.
3. **Naam:** "Minecraft" apne product name me mat use karo. "TurboCraft Client" theek hai,
   bas disclaimer rakho: "Not affiliated with Mojang or Microsoft".
4. **Monetization:** EULA client mods ko allow karta hai par pay-to-win/cosmetic rules hain.
   Cape/wings **cosmetic only** rakho, koi gameplay advantage nahi.
5. **LTW renderer** closed-source proprietary hai — apne APK me mat bharo.

---

## 6. Phone-only dev workflow (ye important hai)

Koi PC nahi hai, isliye loop ye hoga:

```
Arena chat me code change karvao
        ↓
main commit + push (arena/... branch)
        ↓
GitHub Actions automatically build karta hai (~3-5 min)
        ↓
Phone pe GitHub app → Actions → run → Artifacts → jar download
        ↓
TurboCraft Launcher / Pojav me mods/ folder me daalo → game test
```

CI: `.github/workflows/build.yml` — push pe jar build + artifact upload.
Error aaye to log yahan paste kar dena, main fix karta hoon.

---

## 7. Poora system kaisa dikhega

```
TurboCraft Launcher (tumhare paas ready)
├── Profile: "TurboCraft Client 1.20.1"
│     ├── MC 1.20.1 + Fabric loader        ← launcher download karta hai
│     ├── mods/turbocraft-client-0.1.0.jar ← CI se aata hai (GitHub Releases)
│     ├── mods/sodium, lithium, iris...    ← manifest se auto-download
│     └── config/turbocraft.json           ← HUD settings, user ka apna
└── (optional) "Vanilla" profile

Game ke andar:
  Right Shift → TurboCraft menu (HUD toggles)
  Top-left  → FPS / CPS / coords / ping / keystrokes
```
