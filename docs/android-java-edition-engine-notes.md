# Minecraft: Java Edition on Android — How It Actually Works

> Research notes for TurboCraft Client.
> Sources: source-code study of **PojavLauncher** (`PojavLauncherTeam/PojavLauncher`, LGPLv3, discontinued Sep 2025)
> and its successor **Amethyst** (`AngelAuraMC/Amethyst-Android`, LGPLv3), plus Fold Craft Launcher (FCL-Team, GPLv3)
> and Zalith Launcher 2 (Jetpack Compose UI on the same Pojav engine).

---

## 0. TL;DR — the one-line answer

Android pe Minecraft **Java Edition run hota hai — "emulate" nahi hota**.
Java bytecode architecture-agnostic hai, so aarch64 JVM bytecode ko **natively JIT** kar deta hai
(x86 → ARM emulation ki zaroorat hi nahi, jaise Windows pe hota hai).
Problem sirf **3 missing desktop cheezon** ki hai:

| Missing on Android | Desktop equivalent | Solution used by launchers |
|---|---|---|
| JVM | Oracle/OpenJDK for Windows/Linux | Custom **OpenJDK 8/17/21 built for Android (bionic)** |
| OpenGL 2.1–4.6 driver | GPU vendor GL driver | **GL→GLES/Vulkan translation layer** (GL4ES, MobileGlues, Zink, LTW, ANGLE) |
| Window + mouse/keyboard + AWT | GLFW / X11 / Win32 | **LWJGL3 ka custom Android build** (GLFW stub) + touch control layer |

Baaki sab — game jar, assets, Fabric/Forge, servers, mods — **bilkul wahi PC wala stack** hai.

---

## 1. Layered architecture (bottom → top)

```
┌──────────────────────────────────────────────────────────────────────┐
│ Minecraft (unmodified jar) + Fabric / Forge / NeoForge / Sodium ...   │
├──────────────────────────────────────────────────────────────────────┤
│ LWJGL 3 (patched for Android)                                        │
│   org.lwjgl.glfw.*  → Java stub, real work JNI me                     │
│   org.lwjgl.opengl  → GL calls jo bridge layer pe jaate hain          │
├──────────────────────────────────────────────────────────────────────┤
│ Native bridge (libpojavexec.so)                                       │
│   jre_launcher.c  → JLI_Launch() (JVM embed, koi `java` binary nahi)  │
│   egl_bridge.c    → ANativeWindow ↔ EGLSurface                        │
│   input_bridge_v3.c → JNI: touch/key/mouse → GLFW callbacks           │
│   jvm_hooks/      → dlopen hooks, JAVA_HOME/LD_LIBRARY_PATH fixes     │
├──────────────────────────────────────────────────────────────────────┤
│ Renderer .so (choose one per profile)                                 │
│   libng_gl4es.so | libmobileglues.so | libOSMesa.so (Zink)            │
│   | libltw.so | libEGL_angle.so | (legacy) virglrenderer              │
│        ↓                                                             │
│   Android EGL → GLES 2/3 or Vulkan (Adreno: bundled Turnip driver)    │
├──────────────────────────────────────────────────────────────────────┤
│ Android shell (Java/Kotlin): launcher UI, auth, downloader,           │
│ control editor, SurfaceView, Activity lifecycle, storage (SAF)        │
└──────────────────────────────────────────────────────────────────────┘
```

Key insight: **Minecraft ko chheda nahi jaata**. Game ko lagta hai wo normal desktop GLFW/OpenGL
use kar raha hai — uske neeche sab kuch swap kar diya jaata hai.

---

## 2. Component-by-component

### 2.1 Java runtime (JRE)

- Android ka built-in runtime **Dalvik/ART** hai — wo `jar` file nahi chala sakta, aur uska `libc`
  **bionic** hai (glibc nahi). Isliye **alag OpenJDK build** ship hota hai.
- Launcher apne APK ke andar (ya first-run pe download) ye rakhta hai:
  `jre8`, `jre17`, `jre21` — ARM64 / ARM32 / x86 / x86_64 sab ke liye.
- Version mapping (must-know):

  | Minecraft version | Java |
  |---|---|
  | ≤ 1.16.5 | Java 8 |
  | 1.17 – 1.20.4 | Java 17 |
  | 1.20.5+ (aur 1.21.x) | Java 21 |

- Build source: `AngelAuraMC/openjdk-build-multiarch` (patched OpenJDK — bionic, no X11,
  headless AWT, ARM opto/JIT).
- **JVM ko process ki tarah start nahi karte.** Native code `dlopen("libjli.so")` karke
  `JLI_Launch(argc, argv, ...)` call karta hai — i.e. JVM **usi Android process ke andar** embed hota hai.
  Isi liye `JNI_CreateJavaVM` ke bajaye `JLI_Launch` use hota hai (`jni/jre_launcher.c`).
- `JREUtils.initJavaRuntime()` me dhyan do: `libjvm.so`, `libjava.so`, `libawt.so`,
  `libfreetype.so`, aur `libopenal.so` sab manually `dlopen` kiye jaate hain — kyunki Android linker
  standard JVM library resolution nahi karta.

### 2.2 LWJGL 3 (patched)

Minecraft ka saara platform I/O LWJGL se hota hai. Android build me:

- `org.lwjgl.glfw.GLFW` ka **Java source stub** ship hota hai (real `liblwjgl.so` nahi) —
  `glfwCreateWindow`, `glfwPollEvents`, `glfwSetKeyCallback` etc. sab **JNI se Android shell** ko jaate hain.
- `CallbackBridge.java` = Java↔C bridge: `nativeSendKey`, `nativeSendCursorPos`, `nativeSendScroll`,
  `nativeSendChar`, `nativeSendScreenSize`, gamepad buffers.
  (`@CriticalNative` annotations — fast JNI path, per-input-call overhead kam.)
- `NativeWindow`/`SurfaceView` ka handle `ANativeWindow_fromSurface()` se EGL surface banta hai.
- Natives: `liblwjgl.so`, `liblwjgl_stb.so`, `liblwjgl_tinyfd.so`, `liblwjgl_nanovg.so` (menu/ImGui ke liye),
  `libfreetype.so` (font), `libopenal.so` (audio).
- **Windows-specific natives skip** ho jaate hain, aur version JSON ka `libraries` list
  transform hota hai: Windows/Linux native jars hata ke Android-patched jars daal diye jaate hain.

### 2.3 Graphics translation — sabse critical part

Minecraft ko **desktop OpenGL** chahiye. Phone pe sirf **GLES** ya **Vulkan** hai. 3 tareeke:

| Renderer | Kya karta hai | Output | Notes |
|---|---|---|---|
| **GL4ES / NG-GL4ES (Krypton)** | GL 1.x/2.1 calls → GLES 1.1/2.0 | GLES 2 | Purane versions (1.8.9, 1.12.2) ke liye best; GL 3.x features nahi |
| **MobileGlues** | GL → GLES 3.x (fork of Krypton + per-GPU fixes) | GLES 3 | 1.17+ pe tez, aaj default-ish; Adreno/Mali dono |
| **Zink (Mesa)** | Real OpenGL 4.6 **on top of Vulkan** (Mesa ka Gallium Zink driver) | Vulkan | `libOSMesa.so` + `libEGL_mesa.so`; Adreno pe bundled **Turnip/Freedreno** driver (`libvulkan_freedreno.so`) ke saath best. Shaders/Iris isi pe chalte hain |
| **LTW** | Proprietary GLES 3 layer (vendor binary, `libltw.so`) | GLES 3 | Optional, closed-source — redistribute nahi kar sakte |
| **ANGLE** | GLES ↔ D3D/Vulkan/Metal | GLES on Vulkan | Kuch devices pe stability ke liye |
| **virglrenderer** | GL → GLES via virtio | — | Legacy, hata diya gaya |

Code me: `JREUtils.loadGraphicsLibrary()` renderer ID → `.so` map karta hai
(`opengles2/3 → libng_gl4es.so`, `opengles_mobileglues → libmobileglues.so`,
`vulkan_zink → libOSMesa.so`, `opengles3_desktopgl_zink_kopper → libglxshim.so`, `opengles3_ltw → libltw.so`).
Environment variables se tune hota hai: `LIBGL_ES`, `LIBGL_EGL`, `LIBGL_GLES`,
`MESA_GL_VERSION_OVERRIDE=4.6COMPAT`, `FD_DEV_FEATURES` (Turnip UBWC fix), `POJAV_LOAD_TURNIP=1`.

**Device reality check**

- Snapdragon / Adreno → Zink+Turnip ya MobileGlues (best FPS, shaders possible)
- Mali / Exynos / PowerVR → GL4ES ya MobileGlues (Zink pe aksar crash/black screen)
- `MESA_GLSL_VERSION_OVERRIDE`/`MESA_GL_VERSION_OVERRIDE` spoofing common hai — Minecraft version check pass karane ke liye

### 2.4 AWT / Swing (sirf tab jab koi mod maange)

Minecraft khud AWT pe depend nahi karta (1.13+), par bahut se **mods aur Forge/Fabric installer**
Swing use karte hain. Solution: **Caciocavallo** (OpenJDK ka headless AWT backend) —
`-Dawt.toolkit=...CTCToolkit`, `-Dcacio.managed.screensize=WxH`, `libawt_xawt.so` ka fake build.
Agar tumhara client sirf game chalayega (installer PC pe), ye pura layer skip ho sakta hai — kaafi complexity bachegi.

### 2.5 Input

- Android touch → Java `KeyEvent`/`MotionEvent` → `CallbackBridge` → C input queue → GLFW callbacks
  → game ko lagta hai keyboard/mouse hai.
- **Virtual mouse** (raw cursor + sensitivity), **custom on-screen layout** (drag/resize buttons,
  JSON me save, import/export — Zalith/FCL layouts interconvertible), **gamepad** (XInput-style mapping),
  **gyro**, **notch/cutout** handling, **screen size change** (fold/rotate) par EGL surface re-create.
- Duplicate `*A`/`*` JNI functions (`nglfwSetKeyCallbackA` vs `nglfwSetKeyCallback`) — `CriticalNative`
  variants Android ke fast JNI ke liye.

### 2.6 Game files, accounts, mod loaders

- **Version JSON + assets Mojang se download** (`piston-meta`), exactly PC jaisa; `.minecraft`
  folder structure emulate hota hai (`mods/`, `saves/`, `resourcepacks/`, `shaderpacks/`, `config/`).
- **Auth**: Microsoft OAuth (device code flow) + Xbox Live → Minecraft services → `accessToken`.
  Local/offline accounts bhi supported (sirf cracked servers ke liye, EULA risk).
- **Mod loaders**: Forge ka **installer jar JVM ke andar hi chala diya jaata hai** (`forge_installer` module),
  Fabric/Quilt ke liye vanilla launcher API. Amethyst me Forge/Fabric/Quilt/NeoForge auto-install hai.
- **Sodium/Lithium/Iris** Android pe chalte hain, but Sodium ke mixin checks ko patch karna padta hai
  (`-Dsodium.checks.issue2561=false`, mixin `buffer_builder.intrinsics=false`) — kyunki mobile GL
  drivers strict nahi hote.

---

## 3. "Lunar Client / Fast Client Android pe kaise chal rahe hain?"

Ye samajhna zaroori hai, kyunki 90% YouTube videos isi confusion pe based hain:

### 3.1 Lunar Client
- Lunar Client ek **desktop Java application** hai (Electron launcher + game ke andar injection).
  **Android/iOS ke liye native Java-edition Lunar exist nahi karta.**
- Jo "Lunar Client" Android pe milta hai wo **Bedrock (MCPE) proxy/pack** hai — Lunar Proxy
  (Bedrock client jo PC/Android/iOS/console pe chalta hai). Uska **Java Edition se koi lena-dena nahi**.
- Log Java Edition pe "Lunar jaise features" khelna chahte hain to wo **Pojav/Amethyst/Zalith me
  Lunar jaisa HUD/modpack** chalate hain — actual Lunar client nahi.

### 3.2 Fast Client (aur baaki "fast/FPS" clients)
- Fast Client = **PC launcher** (Windows/macOS/Linux) jo Fabric + Sodium/Lithium/Starlight bundle karta hai
  aur mods Modrinth se one-click install karta hai.
- Android pe log ise **MojoLauncher / Zalith Launcher** ke andar **import karke** chalate hain:
  ya to unka `.jar`/instance import kar dete hain, ya wahi modpack apne launcher me daal dete hain.
  Engine wahi Pojav wala hai — Fast Client khud kuch "Android magic" nahi karta.
- Matlab: **"Android pe client chalana" = Pojav-family engine + us client ke mods/HUD.**

### 3.3 Iska seedha matlab tumhare project ke liye
Tumhe "Minecraft ko Android pe port" nahi karna. Tumhe **2 cheezein** banani hain:

1. **Engine** — Pojav/Amethyst ka fork (JVM + GL bridge + input + downloader) ← 90% kaam already open-source hai
2. **Client layer** — apna HUD, cosmetics, mod bundle, settings UI ← yahi tumhara "TurboCraft" hoga
   (Fabric mod / mixin ke through, game ke andar)

---

## 4. Legal / licensing (ignore mat karo, ban risk hai)

| Item | Rule |
|---|---|
| Minecraft game files | **Redistribute nahi kar sakte.** Har user apne Microsoft account se Mojang se download kare. Cracked jars ship karna = EULA + copyright violation |
| Minecraft EULA | Client modifications allowed, par **monetization restricted** (cosmetics/pay-to-win pe rules hain). Server-side unfair advantage nahi |
| Mojang brand | "Minecraft" naam/logo apne product ke naam me use na karo (TurboCraft theek hai) |
| PojavLauncher / Amethyst | **LGPL-3.0** (Java part) → fork karoge to apne changes bhi LGPL me release karne padenge (static-link rule nahi, par modified library ke sources dene padenge) |
| FCL | **GPL-3.0** — zyada strict, poora derivate GPL |
| LTW renderer | **Closed-source proprietary** binary — ship nahi kar sakte, optional load karna padta hai |
| Turnip / Mesa / GL4ES / MobileGlues | MIT-ish / permissive — ship kar sakte ho (attribution ke saath) |

Practical safest route: **Amethyst fork (LGPL) + apne mods (apna license) + user ke apne Mojang login**.

---

## 5. Build & ship reality

- Android Studio + **NDK** (C code), Gradle. Submodules: `SDL`, `MobileGlues`, `androidnsbypass`, `MioLibPatcher`.
- APK size typically **150–350 MB** (3 JREs + renderers + LWJGL natives). ABI split karo
  (`arm64-v8a` sirf = ~100 MB), Play Store pe App Bundle.
- Fresh clone se first debug build ~20–40 min (JRE artifacts CI se aate hain; khud build karoge to ghante).
- **Performance truth**: 4 GB phone pe 1.8.9/1.12.2 smooth, 1.20+ sirf 6–8 GB pe theek, 1.21+ Sodium ke saath.
  Render distance 4–8 chunks, resolution scale 70–80%, RAM allocation ~2.5 GB max (zyada dene se Android
  process kill kar deta hai).

---

## 6. Suggested roadmap for TurboCraft Client

| Phase | Kaam | Output |
|---|---|---|
| 0 | Engine fork + build chalana (Amethyst ya Pojav), ek device pe 1.20.1 launch | working APK |
| 1 | Apna branding, profile UI, version/mod loader manager, control layout editor | usable client |
| 2 | Client layer: Fabric mod me HUD (FPS, CPS, keystrokes, armor status, potion timers) + settings/config | "client" feel |
| 3 | Cosmetics + auth backend (Supabase), skin/cape system, friends/party | product |
| 4 | Per-GPU renderer auto-pick, crash reporter, OTA updates | polish |

---

## 7. Code map (agar fork karna ho)

```
app_pojavlauncher/src/main/
├── java/net/kdt/pojavlaunch/
│   ├── Tools.java              # JVM args build, renderer list, version/classpath
│   ├── MainActivity.java       # renderer select, launch flow
│   ├── MinecraftGLSurface.java # SurfaceView/EGL surface
│   ├── utils/JREUtils.java     # renderer .so choose, env vars, dlopen JRE libs
│   ├── prefs/                  # settings screens
│   └── tasks/                  # download/install tasks (version, assets, mods)
├── java/org/lwjgl/glfw/        # GLFW stub + CallbackBridge (input bridge)
├── jni/
│   ├── jre_launcher.c          # JLI_Launch (embed JVM)
│   ├── egl_bridge.c            # ANativeWindow → EGL
│   ├── input_bridge_v3.c       # touch/key → GLFW callbacks
│   ├── ctxbridges/gl_bridge.c  # GL context bundle per window
│   ├── jvm_hooks/              # dlopen/env hooks for bionic
│   └── driver_helper/          # Turnip/adreno driver loading, glxshim
├── assets/components/lwjgl3/   # lwjgl jars (merged modules, freetype, openal, nanovg...)
└── jniLibs/arm64-v8a/          # libvulkan_freedreno.so (Turnip), etc.
```
