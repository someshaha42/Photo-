# PixelForge Studio Web & Android Modular Architecture

PixelForge Studio is structured with strict file-path-based modularity. Every UI component, style sheet, script, XML descriptor, animation JSON, and PNG icon exists in its own dedicated file path with zero monolithic or hardcoded inline code blocks.

## Directory Structure & Asset Paths

- **Web Frontend (`/src/`)**:
  - `index.html`: HTML5 entry point referencing modular stylesheets.
  - `main.tsx`: TypeScript DOM renderer.
  - `App.tsx`: Root React state controller.
  - `manifest.json`: Web app metadata and asset pointers.
  - `/styles/pixelforge.css`: Centralized CSS design system and theme tokens.
  - `/components/HomeScreen.tsx`: Modular home screen component.
  - `/components/EditorScreen.tsx`: Modular editor screen component.
  - `/components/CornerPinAnd3DPanels.tsx`: Modular 3D & Corner Pin panels.
  - `/components/FilterToolPanel.tsx`: Modular filter and blend panel.
  - `/components/FloatingPanelContainer.tsx`: Draggable/resizable panel container.
  - `/assets/icons/`: 32 standalone `.png` asset icon files.
  - `/assets/animations/`: Lottie/JSON animation descriptor files.

- **Android App (`/app/`)**:
  - `AndroidManifest.xml`: Multi-device and platform capabilities manifest.
  - `/src/main/cpp/`: C++ native ARMv8/ARMv7/x86 pixel engine.
  - `/src/main/java/com/example/compat/`: Compatibility, device environment, and native bridges.
  - `/src/main/res/xml/`: XML configuration rules (network security, data extraction).
  - `/src/main/res/drawable/`: Vector and raster drawable assets.
