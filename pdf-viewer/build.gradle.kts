plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.hiaashuu.pdfreader"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
        }
    }
    
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Expose Pdfium so apps consuming this library can access it if needed
    api(libs.pdfium.android)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                // Dynamically fetch the Group and Version injected by JitPack's GitHub Tag
                // Fallback to defaults for local testing
                groupId = project.group.toString().ifEmpty { "com.github.hiaashuu" }
                artifactId = "pdfreader"
                version = project.version.toString().ifEmpty { "1.0.6" }
                
                from(components["release"])
            }
        }
    }
}