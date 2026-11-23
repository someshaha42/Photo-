এই গোটা ui টাকে যে কোন ফরমেটে হোক ব্যাকআপ করে রাখতে হবে  , প্রয়োজন পড়লে web development UI like HTML CSS JavaScript react native ইত্যাদি যে কোন প্রকারে ইউ আই তাকে গোটা ব্যাকআপ করে এপ্লিকেশনে ভেতরে Root Directory এর ভেতরে যে কোন ফোল্ডার যেখানে যেখানে রাখার প্রয়োজন সেখান দিয়ে অ্যাপ্লিকেশন শনাক্ত করে লোডিং করবে । যাতে জেনারেশন যেন না করতে হয় । 

Application Architecture - Kotlin - Primary Android application
development and modern Android APIs. Java - Legacy Android compatibility
and Java-based libraries. C / C++ - Native engine, performance-critical
modules, and ARM-specific code. Android NDK - Native code integration
for ARM64 (ARMv8-A) and ARM32 (ARMv7-A).

Device Support - Kotlin + Android Jetpack - Phone and Tablet support.
Kotlin + Wear OS APIs - Wear OS support. Kotlin + Android TV APIs -
Android Television support. Kotlin + Android Automotive APIs - Android
Automotive support. Kotlin + Android XR APIs - XR device support, where
supported by the target SDK. Kotlin / Java + Android Emulator APIs -
Virtual Android and emulator compatibility.

Web Support - React + TypeScript - Reusable web interface and
application logic. HTML5 + CSS3 + JavaScript - Website structure,
styling, and browser functionality. HTTPS + Web APIs - Secure website
communication and web integration.

Build & Compatibility - Gradle + Kotlin DSL - Android project
configuration and platform-specific builds. Android SDK + AndroidX +
Compatibility Libraries - Android 5 (API 21) through Android 17 (API
37), subject to each platform’s API availability. JSON + XML + Bash -
Configuration, Android resources, and build automation.

STRICT NATIVE SOURCE CODE EXTRACTION, FILE FORMAT CONTROL, MODULAR RESOURCE STORAGE AND INDEPENDENT APPLICATION BUILD SYSTEM
==================================================
PRIMARY OBJECTIVE ==================================================
Develop and maintain the entire application using actual, complete, readable, editable, and properly integrated original source code and native application resources.
Every generated UI, UI component, icon, image, animation, transition, interaction, visual effect, and application functionality MUST be converted into its appropriate original source-code file or standalone resource file.
NEVER treat AI instructions, prompts, descriptions, temporary code blocks, serialized UI definitions, or proprietary unknown-format files as substitutes for actual application implementations.
Extract the complete original implementation and integrate it into the existing unified source-code ZIP project.
The final application must be independently buildable and executable without depending on Google AI Studio or AI-generated instructions.
================================================== 2. STRICT NATIVE FILE FORMAT STRUCTURE
Use ONLY the explicitly approved file formats listed below for authored application source code, resources, and project configuration.
Do not introduce unnecessary, proprietary, unknown, or custom file formats.
UI COMPONENTS: .jsx .tsx .kt
UI STYLING: .css .scss .xml
ICONS AND IMAGE ASSETS: .png .jpg .jpeg .webp
ANIMATIONS AND MOTION RESOURCES: .json (Lottie animation data only) .xml (Android animation and drawable resources) .gif .webp .mp4
FONTS: .ttf .otf
AUDIO: .wav .mp3 .ogg .m4a .flac
VIDEO: .mp4 .mkv .webm .mov .3gp .m4v
APPLICATION LOGIC: .java .kt .js .ts .jsx .tsx .cpp .c
NATIVE HEADER FILES: .h .hpp
NATIVE RUST SOURCE: .rs
WEB STRUCTURE: .html
APPLICATION CONFIGURATION: .xml .json .gradle .properties
BUILD AND OPTIMIZATION RULES: .pro
PROJECT DOCUMENTATION: .txt .md
FILE FORMAT RESTRICTIONS:
Use only the approved formats for newly authored application files.
Use each format only for its intended technical purpose.
Do not create unnecessary alternative formats.
Do not rename original files into misleading extensions.
Preserve compatibility with the existing React and Android architecture.
Never replace actual source files with Markdown descriptions or instruction-based representations.
Use only the formats required by the existing application architecture.
Do not break mandatory build-tool requirements by removing necessary generated or binary dependencies.
================================================== 3. ORIGINAL UI COMPONENT SOURCE CODE
Every generated UI component must have its complete original implementation extracted and integrated into the project.
Requirements:
Preserve the complete component structure.
Preserve all original styling properties.
Preserve layout, dimensions, spacing, alignment, typography, and positioning.
Preserve component functionality and interactions.
Preserve all dependencies and component relationships.
Preserve responsive behaviour.
Keep implementations readable and editable.
Avoid unnecessary code duplication.
Never leave a component as a temporary AI-generated preview.
Never save a component exclusively as an instruction or prompt.
React components must retain their appropriate .jsx or .tsx source files.
Android Native components must use the appropriate .kt, .java, and .xml files.
Do not unnecessarily convert one framework into another if the existing architecture already supports the component.
================================================== 4. ORIGINAL ICON AND IMAGE ASSET STORAGE
Every individual icon must exist as a separate, actual image asset.
APPROVED ICON FORMATS:
.png .jpg .jpeg .webp
This requirement applies to:
Application icons.
Toolbar icons.
Navigation icons.
Menu icons.
Button icons.
Panel icons.
Editing-tool icons.
Timeline icons.
Media icons.
Settings icons.
All other interface icons.
STRICT REQUIREMENTS:
Never draw icons directly inside application source code.
Never use inline SVG icons.
Never encode icons using Base64.
Never create icons using CSS shapes or Unicode symbols.
Never replace original image assets with programmatically drawn substitutes.
Every icon must have its own actual filename and project-relative file path.
Preserve original image dimensions, aspect ratio, and visual quality.
Keep image assets physically stored inside the unified application project.
Load actual image assets through their corresponding application resource references.
================================================== 5. SEPARATE ANIMATION AND MOTION RESOURCES
Every animation and visual motion resource must be maintained separately wherever technically possible.
APPROVED ANIMATION FORMATS:
.json (Lottie animation data only) .xml (Android animation and drawable resources) .gif .webp .mp4
This includes:
Opening animations.
Closing animations.
Page transitions.
Panel transitions.
Button animations.
Loading animations.
Layer animations.
Timeline animations.
Visual effects.
Reusable motion resources.
Store animation resources in their appropriate standalone files.
Do not combine every animation definition into one enormous source-code file.
Executable animation controllers, event handling, and interaction logic must remain in actual application source-code files.
Never mistake an animation resource file for a complete executable component implementation.
================================================== 6. SEPARATE COMPONENT FUNCTIONALITY AND INTERACTION LOGIC
Every reusable component must have an appropriately organized implementation.
Keep the following logically separated:
Component structure.
Styling.
Animation resources.
Interaction handling.
Event management.
State management.
Rendering logic.
Application functionality.
Use actual source-code files for executable behaviour.
Do not store executable functionality exclusively as JSON descriptions, AI instructions, or other non-executable representations.
Each component must reference its required resources through valid project-relative paths or framework-supported resource identifiers.
================================================== 7. REAL FILE PATH AND RESOURCE LOADING
Every source file and resource must have an actual location inside the unified project.
The application must use its integrated resource architecture.
EXAMPLE PROJECT DIRECTORIES:
ANDROID APPLICATION LOGIC: app/src/main/java/
ANDROID IMAGE AND DRAWABLE RESOURCES: app/src/main/res/drawable/
ANDROID ANIMATION RESOURCES: app/src/main/res/anim/
ANDROID FONT RESOURCES: app/src/main/res/font/
ANDROID RAW MEDIA RESOURCES: app/src/main/res/raw/
REACT UI COMPONENTS: src/components/
UI STYLING: src/styles/
ICON ASSETS: src/assets/icons/
ANIMATION ASSETS: src/assets/animations/
Adapt these directories to the existing project architecture without unnecessarily changing its established structure.
IMPORTANT:
Source code must be integrated and compiled or bundled during the normal application build process.
At runtime, the application must load the packaged UI, images, fonts, animations, and other resources through their actual application resource paths.
Do not attempt to execute uncompiled source-code files directly as runtime UI resources when the framework requires compilation or bundling.
================================================== 8. FRAMEWORK COMPATIBILITY: REACT AND ANDROID
Preserve the framework selected for each existing application implementation.
REACT:
Maintain actual .jsx and .tsx component files.
Maintain appropriate .css and .scss styling files.
Preserve React component structure and functionality.
Integrate React code into the application's supported build and bundling system.
ANDROID:
Maintain actual .kt and .java application source files.
Maintain Android UI and resource definitions in .xml.
Preserve Android resource directories and resource references.
Maintain compatibility with the existing Android build configuration.
If both frameworks are used, integrate them through a technically compatible architecture.
Never assume that React source code can be executed directly as native Android code without an appropriate integration layer.
================================================== 9. COMPONENT AND RESOURCE PRESERVATION
The actual implementation must remain available for future editing and development.
When reopening the project:
Inspect the existing source-code ZIP.
Locate the actual component source files.
Locate the original image and icon assets.
Locate the actual animation resources.
Inspect the existing application architecture.
Reuse existing implementations.
Preserve existing styling and functionality.
Modify only the components that actually require changes.
NEVER reconstruct existing components from their previous AI descriptions when their original source files are already available.
NEVER replace existing working source code with simplified prompt-based instructions.
================================================== 10. ONE UNIFIED SOURCE-CODE ZIP PROJECT
Maintain one complete unified source-code ZIP package.
It must contain:
All application source code.
All UI components.
All styling files.
All icon assets.
All image resources.
All animation resources.
All audio resources.
All video resources.
All required fonts.
All application configuration.
All necessary project dependencies and build configuration.
Do not create unnecessary separate ZIP packages for individual components or resources.
Maintain a clean, modular, readable, and logically organized project structure.
================================================== 11. NO ARBITRARY FILE SIZE LIMITS
Do not impose arbitrary file-size restrictions on source code, UI components, images, icons, animations, audio, video, or other required resources.
Never truncate, omit, simplify, or replace an original implementation merely to reduce project size.
Preserve complete original source code and required assets.
Do not sacrifice visual quality, functionality, or implementation completeness for unnecessary size reduction.
================================================== 12. STRICT RUNTIME INDEPENDENCE
The final application must function independently of:
Google AI Studio.
AI-generated prompts.
AI-generated instructions.
External code-generation services.
Temporary UI previews.
Runtime UI reconstruction systems.
All required executable implementations and resources must already be integrated into the application project.
The application must load and render its UI from its own compiled application implementation and packaged resources.
No instruction-based UI generation is permitted at runtime.
================================================== 13. BUILD, INTEGRATION AND FILE VERIFICATION
Before declaring any implementation complete, verify:
Every required source file exists.
Every referenced resource file exists.
Every file path resolves correctly.
Every UI component is integrated.
Every icon loads from its actual image resource.
Every animation loads from its actual animation resource.
All component interactions function correctly.
All required dependencies are included.
Framework compatibility is maintained.
The project builds successfully.
The application launches successfully.
The application functions without Google AI Studio or external AI instructions.
Never claim that files have been created, extracted, integrated, or verified unless those operations have actually been completed.
================================================== 14. PERMANENT IMPLEMENTATION PRESERVATION
Documentation and project metadata may describe how the application works, but they must never replace its actual implementation.
Preserve:
Original UI designs.
Original component structures.
Original styling.
Original image assets.
Original animations.
Original transition timing.
Original interaction behaviours.
Original application functionality.
Actual source-code file paths.
Future modifications must build upon the existing implementation instead of recreating the application from descriptions.
================================================== FINAL NON-NEGOTIABLE DIRECTIVE
GENERATE AND STORE REAL ORIGINAL SOURCE CODE.
EXTRACT AND INTEGRATE COMPLETE UI COMPONENT IMPLEMENTATIONS.
STORE EVERY ICON AS AN ACTUAL IMAGE FILE.
STORE ANIMATIONS AND OTHER VISUAL RESOURCES IN THEIR APPROPRIATE NATIVE FILE FORMATS.
KEEP EXECUTABLE FUNCTIONALITY IN ACTUAL SOURCE-CODE FILES.
USE REAL PROJECT FILE PATHS AND FRAMEWORK-SUPPORTED RESOURCE LOADING.
MAINTAIN ONE COMPLETE, UNIFIED, INDEPENDENTLY BUILDABLE SOURCE-CODE ZIP PROJECT.
NEVER SUBSTITUTE ACTUAL APPLICATION IMPLEMENTATIONS WITH AI INSTRUCTIONS, PROMPTS, TEMPORARY CODE BLOCKS, UNKNOWN FORMATS, OR RUNTIME-GENERATED UI.
THE ORIGINAL APPLICATION SOURCE CODE AND RESOURCES MUST REMAIN PERMANENTLY INTEGRATED, READABLE, EDITABLE, AND REUSABLE.

STRICT EXISTING SOURCE CODE PRESERVATION:

Before creating, modifying, or replacing any UI component, inspect the entire existing project and identify all original source-code files, UI implementations, styling files, icons, images, animations, and related resources.

The existing application UI and its original implementations are mandatory. Do not recreate, simplify, redesign, replace, or regenerate any existing UI component when its original source code or resources are already available.

Reuse the exact existing implementations and preserve their original appearance, dimensions, layout, styling, animations, transitions, interactions, and functionality.

If any existing UI implementation is available in Kotlin, Java, React, JSX, TSX, CSS, XML, or another supported format, retain its original implementation and integrate it into the appropriate project location.

Only generate new code when an actual implementation is missing or a specific modification requires it.

Never overwrite working source files without first inspecting and preserving their existing implementation.

Verify that all original UI components and resources remain available, integrated, editable, and functional after every modification.