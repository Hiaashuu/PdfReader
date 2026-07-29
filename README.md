<div align="center">

# 📄 PdfReader

### A fast, lightweight Android PDF viewer library — built on PdfiumAndroid, wired natively for Jetpack Compose.

[![JitPack](https://jitpack.io/v/hiaashuu/PdfReader.svg)](https://jitpack.io/#hiaashuu/PdfReader)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-21%2B-brightgreen)
![License](https://img.shields.io/badge/license-MIT--style-blue)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Ready-4285F4?logo=jetpackcompose&logoColor=white)

</div>

---

## ✨ Features

| Category | Support |
|---|---|
| 🔍 Zoom & Pan | Pinch-to-zoom, drag/pan, double-tap zoom |
| 🔄 Scroll direction | Vertical **and** Horizontal swipe |
| 📐 Fit policies | `WIDTH`, `HEIGHT`, `BOTH`, per-page fit |
| 📥 PDF sources | Asset, File, Uri, ByteArray, InputStream, custom `DocumentSource` |
| 🔒 Password-protected PDFs | ✅ Built-in |
| 🖱️ Scroll handle (thumb) | ✅ `DefaultScrollHandle` |
| 🌙 Night mode | ✅ Built-in inverted rendering |
| 🔗 Link handling | In-document jumps + external URI links |
| 🖊️ Annotation rendering | Togglable |
| 🖼️ Bitmap caching | Active/passive eviction + thumbnail cache |
| ⚡ Async rendering | Dedicated background handler thread |
| 🧩 UI compatibility | Classic XML View **and** Jetpack Compose (`AndroidView`) |
| 📑 Page subset | Load only specific pages via `pages(...)` |
| 🎯 Listeners | Load, error, page error, page change, scroll, render, tap, long-press |

---

## 📦 Installation

**Step 1 — Add JitPack** to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Step 2 — Add the dependency** to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.hiaashuu:PdfReader:1.0.6")
}
```

---

## 🚀 Quick Start

### 🟣 Option A — Jetpack Compose (recommended)

```kotlin
@Composable
fun PdfViewerScreen(uri: Uri) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
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
        }
    )
}
```

### 🔵 Option B — Classic XML Layout

**1. Add the view to your layout XML:**

```xml
<com.hiaashuu.pdfreader.PDFView
    android:id="@+id/pdfView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

**2. Load it in your Activity/Fragment:**

```kotlin
val pdfView = findViewById<PDFView>(R.id.pdfView)

pdfView.fromUri(uri)
    .defaultPage(0)
    .enableAnnotationRendering(true)
    .scrollHandle(DefaultScrollHandle(this))
    .spacing(10)
    .pageFitPolicy(FitPolicy.BOTH)
    .load()
```

---

## 📥 Loading PDFs — every supported source

PdfReader can load a PDF from **five different source types** out of the box. Pick whichever fits your use case:

### 1️⃣ From a `Uri` (user-picked file, content provider, SAF)

```kotlin
pdfView.fromUri(uri).load()
```
> Use this for files picked via `ActivityResultContracts.OpenDocument()` or shared from other apps.

### 2️⃣ From assets (`app/src/main/assets/`)

```kotlin
pdfView.fromAsset("sample.pdf").load()
```
> Place your PDF inside the `assets/` folder of your app module. Great for bundled manuals, docs, or demo content.

### 3️⃣ From a `File` (local storage / downloaded file)

```kotlin
val file = File(context.filesDir, "document.pdf")
pdfView.fromFile(file).load()
```

### 4️⃣ From a `ByteArray` (in-memory / downloaded over network)

```kotlin
val bytes: ByteArray = downloadPdfBytes() // e.g. from Retrofit/OkHttp
pdfView.fromBytes(bytes).load()
```
> Ideal for **online PDF reading** — fetch bytes from a URL with your networking library of choice, then feed them directly here without writing to disk first.

### 5️⃣ From an `InputStream`

```kotlin
val stream: InputStream = context.contentResolver.openInputStream(uri)!!
pdfView.fromStream(stream).load()
```
> Useful when you already have a stream open (e.g., from a `ContentResolver`, ZIP entry, or custom source).

### 6️⃣ Custom source (advanced)

```kotlin
pdfView.fromSource(myCustomDocumentSource).load()
```
> Implement `DocumentSource` yourself if none of the above fit your storage layer.

**📡 Reading a PDF straight from the internet — full example:**

```kotlin
lifecycleScope.launch(Dispatchers.IO) {
    val bytes = URL("https://example.com/sample.pdf").readBytes()
    withContext(Dispatchers.Main) {
        pdfView.fromBytes(bytes)
            .defaultPage(0)
            .pageFitPolicy(FitPolicy.BOTH)
            .load()
    }
}
```
> For production apps, prefer OkHttp/Retrofit over raw `URL.readBytes()` for timeout handling, caching, and error control.

---

## ⚙️ Full Configuration Reference

Every `Configurator` option available after `.fromX(...)`:

```kotlin
pdfView.fromUri(uri)
    .pages(0, 2, 4)                          // load only specific pages (optional)
    .defaultPage(0)                          // page to open on
    .enableSwipe(true)                       // enable swipe navigation
    .swipeHorizontal(false)                  // false = vertical, true = horizontal
    .enableDoubletap(true)                   // double-tap to zoom
    .enableAnnotationRendering(true)         // render PDF annotations
    .password(null)                          // pass a String if PDF is encrypted
    .scrollHandle(DefaultScrollHandle(context)) // draggable scroll thumb
    .spacing(10)                             // dp spacing between pages
    .autoSpacing(false)                      // auto-fit spacing to screen
    .pageFitPolicy(FitPolicy.BOTH)           // WIDTH, HEIGHT, or BOTH
    .fitEachPage(false)                      // fit each page individually
    .nightMode(false)                        // inverted colors for dark reading
    .disableLongpress()                      // disable long-press gesture
    .linkHandler(DefaultLinkHandler(pdfView)) // handle in-doc/external links
    .onLoad(OnLoadCompleteListener { pages -> })
    .onError(OnErrorListener { throwable -> })
    .onPageError(OnPageErrorListener { page, throwable -> })
    .onPageChange(OnPageChangeListener { page, pageCount -> })
    .onPageScroll(OnPageScrollListener { page, positionOffset -> })
    .onRender(OnRenderListener { pagesCount -> })
    .onTap(OnTapListener { motionEvent -> false })
    .onLongPress(OnLongPressListener { motionEvent -> })
    .load()
```

---

## 🔁 Reloading / swapping a PDF at runtime

Whether in Compose or XML, always `recycle()` before loading a new document into the same `PDFView` instance:

```kotlin
pdfView.recycle()
pdfView.fromUri(newUri)
    .defaultPage(0)
    .load()
```

In Compose, do this inside the `update` block of `AndroidView`:

```kotlin
AndroidView(
    factory = { context -> PDFView(context, null).apply { fromUri(uri).load() } },
    update = { view ->
        view.recycle()
        view.fromUri(uri).load()
    }
)
```

---

## 🏗️ Project Structure

```
PdfReader/
├── app/            🧪 Demo app showcasing library usage (not published)
└── pdf-viewer/     📚 The published library module (JitPack artifact)
```

> Only `pdf-viewer` is published to JitPack. `app` is a sample module for local testing inside AndroidIDE — never shipped in the release artifact.

---

## 📋 Requirements

| Tool | Minimum |
|---|---|
| `minSdk` | 21+ |
| Kotlin | 2.1.0+ |
| AGP | 8.13.0+ |

---

## 🏷️ Version

**Current stable: `1.0.4`**

### 📝 Changelog

**1.0.4**
- 🐛 Fixed nullable `Bitmap?` compiler errors across `CacheManager`, `PDFView`, and `RenderingHandler` (safe-call recycle, early-return null guard in `drawPart`)
- ✅ Verified clean `publishToMavenLocal` build on Gradle 9.0.0 / Kotlin 2.2.0 toolchain

**1.0.3**
- 🚀 Initial stable JitPack release

---

## 📄 License

This project builds on the architecture of `android-pdf-viewer` (Barteksc), adapted and maintained under the `com.hiaashuu` namespace.

## 👤 Author

Built and maintained by **[Hiaashuu](https://github.com/hiaashuu)** · published via CodXFuse.
