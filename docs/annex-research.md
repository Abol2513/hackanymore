# HEXFORGE Research & Reference Annex (Part 23)

This annex documents the open-source and behavioral reference pillars studied during HEXFORGE architecture design, along with the licensing boundary and integration decision for each component.

| # | Project / Ecosystem | Mechanism Studied | License & Integration Decision |
|---|---|---|---|
| 1 | RikkaApps/Shizuku (+ Sui) | Binder service over ADB/root (`IShizukuBinderService`), permission model, Android 11+ wireless debugging pairing | License: Apache-2.0 — Decision: vendor API dependency (`rikka.shizuku`) + clean-room ADB-Direct T2 client |
| 2 | LSPosed/LSPatch | Embedded hook loader & manifest proxy class inside repacked APKs | License: GPL-3.0 — Decision: clean-room design reference / optional user-installed plugin |
| 3 | LSPosed/LSPlant | ART `ArtMethod` hooking and trampoline backup mechanism | License: LGPL-3.0 — Decision: optional runtime plugin / isolated native dependency |
| 4 | canyie/Pine | Broad Android 4.4–15 ART hook compatibility matrix & Thumb-2 notes | License: Apache-2.0 — Decision: clean-room compatibility matrix & selectable backend |
| 5 | VirtualApp lineage | Process hosting, `IApplicationThread` / Binder proxy stubs | License: Mixed / Commercial restrictions — Decision: clean-room 2026-first HexSpace architecture |
| 6 | Perfare/Il2CppDumper | ELF + `global-metadata.dat` header layouts (v24–v31), class/field/method dump | License: MIT — Decision: clean-room C++/Kotlin implementation in `:core:hexelf` |
| 7 | vfsfitvnm/frida-il2cpp-bridge | Live in-process IL2CPP reflection & runtime metadata discovery | License: MIT — Decision: clean-room runtime bridge model |
| 8 | BepInEx Il2CppInterop | Managed-to-IL2CPP interop assembly invocation patterns | License: MIT — Decision: design reference for M0/M3 loader |
| 9 | Frida Gadget | Optional external instrumentation gadget tier | License: wxWindows / LGPL — Decision: plugin-only (user-supplied) |
| 10 | iBotPeaches/Apktool | Binary AXML/ARSC resource codec behavior tables | License: Apache-2.0 — Decision: clean-room Kotlin resource codec |
| 11 | JesusFreke/smali + dexlib2 | Programmatic DEX AST & smali manipulation | License: BSD-3 / Apache-2.0 — Decision: vendor `dexlib2` in PatchForge (M5) |
| 12 | skylot/jadx | Read-only Java decompilation view for DeepProbe dossiers | License: Apache-2.0 — Decision: vendor read-only view library or clean-room bridge |
| 13 | ReVanced/revanced-patcher | Named, option-schema, applicability-predicated patch catalog | License: GPL-3.0 — Decision: clean-room `HexPatch` API design |
| 14 | rednaga/APKiD | Packer, obfuscator, and protector fingerprinting rules | License: GPL-3.0 — Decision: clean-room heuristic signatures in `:core:hexelf` |
| 15 | Cheat Engine | Fuzzy unknown-initial scan semantics, pointer chains, cheat tables | License: Custom/Source-available — Decision: clean-room algorithm design |
| 16 | scanmem / GameConqueror | Incremental page-streamed scan-loop refinement | License: GPL-3.0 — Decision: clean-room Kotlin/C++ implementation |
| 17 | KittyMemory | Mobile `/proc/<pid>/maps` + AOB signature scan patterns | License: MIT — Decision: vendor/attribute in native `:core:hexcore` |
| 18 | Dobby / And64InlineHook | ARM/ARM64 inline hook trampoline construction | License: MIT / Apache-2.0 — Decision: clean-room / attributed MIT in `:core:hexhook` |
| 19 | xDL | Linker-namespace `dlopen`/`dlsym` resolution | License: MIT — Decision: vendor with attribution in `:core:hexhook` |
| 20 | Xposed / LSPosed Module API | Sandboxed capability-declared extension API | License: Apache-2.0 / GPL-3.0 — Decision: clean-room `M14` Plugin SDK |
| 21 | Termux Terminal View | Terminal console UX patterns for Chronicle | License: GPL-3.0 — Decision: clean-room Jetpack Compose monospace console |
| 22 | Cafe Bazaar & Myket IAB SDK Docs | Official AIDL interfaces (`IInAppBillingService`), response codes, purchase JSON | License: Official SDK Docs — Decision: clean-room `IabProviderAdapter` emulation |
| 23 | Lucky Patcher behavioral taxonomy | License/IAB/Ad/Permission patch menu taxonomy | License: Closed — Decision: clean-room behavioral parity only |
| 24 | GameGuardian | Fuzzy/group/encrypted scan, speedhack, Lua scripting UX | License: Closed — Decision: clean-room rootless implementation |
| 25 | MT Manager | Dual-pane commander, hex/DEX/ARSC/AXML co-editing UX | License: Closed — Decision: clean-room Kotlin implementation |
