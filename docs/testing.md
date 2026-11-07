# Testing

## 1. Automatic (CI) — already green

| Workflow | Kya check karta hai | Kahan dekho |
|---|---|---|
| **Build TurboCraft Client** | compile + jar + resources (fabric.mod.json, mixins, icon, lang) | Actions tab |
| **Smoke test (headless Minecraft)** | MC 1.20.1 actually boot hota hai mod ke saath, mixin apply hota hai, 0 crash | Actions tab |

Smoke test Ubuntu runner pe **Xvfb + Mesa llvmpipe** se game boot karta hai (8 min cap) aur
`[TurboCraft] Client loaded` marker dhoondhta hai. Ye asli phone test ka replacement nahi hai —
GPU path (GL4ES/MobileGlues/Zink) wahan cover nahi hota — par **startup/mixin crash 100% pakad leta hai**.

Latest verified run: `client/build/libs/turbocraft-client-0.1.0.jar` — 34 KB, saare entries present.

## 2. Phone test (tumhare paas jo hai)

1. **Jar lo:** GitHub app → repo → **Releases** → `latest-build` → `turbocraft-client-0.1.0.jar`
   (ya Actions → run → Artifacts → `turbocraft-client-jar`, zip unzip karke).
2. **Profile banao:** TurboCraft Launcher me MC **1.20.1 + Fabric**.
3. **mods/ folder me daalo:**
   - `turbocraft-client-0.1.0.jar` ← required
   - `fabric-api` (0.92.x for 1.20.1) ← required, iske bina mod load nahi hoga
   - `sodium` + `lithium` ← recommended (FPS)
4. **Launcher settings:** Java **17**, RAM **2–2.5 GB**, renderer:
   - Snapdragon/Adreno → **MobileGlues** (ya Zink agar shaders chahiye)
   - Mali/Exynos/purana phone → **GL4ES**
5. **Game kholo** → world/server join karo → **Right Shift** → TurboCraft menu.

### Kya check karna hai (checklist)

- [ ] Game normally load hua (crash nahi)
- [ ] HUD top-left dikh raha hai: `FPS: xx`, `CPS: 0 | 0`, `XYZ: ...`
- [ ] Mouse click karne pe CPS badhta hai (left click → pehla number)
- [ ] WASD dabane pe keystrokes highlight hote hain
- [ ] Server pe `Ping: xxms` dikhta hai
- [ ] Right Shift se menu khulta hai, toggle karne pe HUD turant badalta hai
- [ ] Game band karke `config/turbocraft.json` me settings save hui
- [ ] **30+ FPS** with Sodium, render distance 6–8

## 3. Agar crash ho

Launcher ka crash log lo (`latest.log` ya Crash Report), usme ye lines dhoondo:

```
Mixin apply failed / MixinApplyError      → MouseMixin ka mapping issue (mod tab bhi load hoga, CPS 0)
NoSuchMethodError / NoClassDefFoundError  → Fabric API missing ya galat version
Unsupported class file major version 61   → Java runtime 17 nahi hai
```

Log yahan chat me paste kar dena — main fix kar dunga. Isliye mixin `require = 0` hai: agar future
1.20.x patch me `Mouse#onMouseButton` ka naam badal jaye, to **sirf CPS band hoga, game crash nahi**.

## 4. Known limits (v0.1.0)

- Sirf **1.20.1 + Fabric**. 1.21.x (Java 21) aur 1.8.9 (Legacy Fabric) alag ports hain.
- HUD position config file se badalni padegi (`hudX`, `hudY`) — drag-to-move v0.2 me.
- Cosmetics/capes nahi (phase 4).
- Shaders sirf Zink/MobileGlues pe; GL4ES pe Iris crash kar sakta hai.
