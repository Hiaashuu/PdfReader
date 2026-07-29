# PdfReader

A lightweight, dependency-minimal Android PDF viewer library built on top of PdfiumAndroid, with native Jetpack Compose interop support via `AndroidView`.

[![](https://jitpack.io/v/hiaashuu/PdfReader.svg)](https://jitpack.io/#hiaashuu/PdfReader)

## Features

- Smooth pinch-to-zoom and drag/pan page navigation
- Vertical and horizontal swipe modes
- Fit policies: WIDTH, HEIGHT, BOTH
- Asset, File, Uri, ByteArray, and InputStream PDF sources
- Page snapping and scroll handle (thumb) support
- Annotation rendering toggle
- Double-tap zoom and animation manager for smooth transitions
- Link tap handling (in-document navigation and external URI links)
- Bitmap caching with active/passive cache eviction and thumbnail cache
- Async page decoding and rendering via dedicated handler thread
- Compatible with legacy View-based UI and Jetpack Compose (via `AndroidView`)

## Installation

Add JitPack to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.hiaashuu:PdfReader:1.0.4")
}
```

## Usage

### Jetpack Compose

```kotlin
@Composable
fun PdfViewerComposable(uri: Uri) {
    AndroidView(
        factory = { context ->
            PDFView(context, null).apply {
                fromUri(uri)
                    .defaultPage(0)
                    .enableAnnotationRendering(true)
                    .scrollHandle(DefaultScrollHandle(context))
                    .spacing(10)
                    .pageFitPolicy(FitPolicy.BOTH)
                    .load()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

### Classic View / XML

```kotlin
pdfView.fromUri(uri)
    .defaultPage(0)
    .enableAnnotationRendering(true)
    .scrollHandle(DefaultScrollHandle(context))
    .spacing(10)
    .pageFitPolicy(FitPolicy.BOTH)
    .load()
```

### Supported sources

```kotlin
pdfView.fromFile(file)
pdfView.fromUri(uri)
pdfView.fromAsset("sample.pdf")
pdfView.fromBytes(byteArray)
pdfView.fromStream(inputStream)
```

## Project Structure

```
PdfReader/
├── app/            Demo app showcasing library usage (not published)
└── pdf-viewer/     The published library module (JitPack artifact)
```

Only the `pdf-viewer` module is published to JitPack. The `app` module exists purely as a sample/demo for local testing inside AndroidIDE and is never included in the release artifact.

## Requirements

- `minSdk` 21+
- Kotlin 2.1.0+
- AGP 8.13.0+

## Version

**Current stable: 1.0.4**

### Changelog

**1.0.4**
- Fixed nullable `Bitmap?` compiler errors across `CacheManager`, `PDFView`, and `RenderingHandler` (safe-call recycle, early-return null guard in `drawPart`)
- Verified clean `publishToMavenLocal` build on Gradle 9.0.0 / Kotlin 2.2.0 toolchain

**1.0.3**
- Initial stable JitPack release

## License

This project builds on the architecture of `android-pdf-viewer` (Barteksc), adapted and maintained under the `com.hiaashuu` namespace.

## Author

Built and maintained by [Hiaashuu](https://github.com/hiaashuu), published via CodXFuse.
