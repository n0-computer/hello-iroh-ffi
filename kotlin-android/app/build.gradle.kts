plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "computer.iroh.pong"
    compileSdk = 35

    defaultConfig {
        applicationId = "computer.iroh.pong"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
        )
    }

    // iroh-ffi's kotlin module is a JVM library, not an Android library.
    // Until it publishes an Android-friendly Maven artifact, consume the
    // generated bindings and the pre-built per-ABI .so files directly
    // from the sibling iroh-ffi checkout. Once published, delete these
    // source-set lines and add the artifact as a normal dependency.
    sourceSets {
        getByName("main") {
            java.srcDirs("../../../iroh-ffi/kotlin/lib/src/main/kotlin")
            jniLibs.srcDirs("../../../iroh-ffi/kotlin/lib/src/main/jniLibs")
        }
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    // iroh-ffi's generated bindings call java.lang.ref.Cleaner at runtime
    // behind a Class.forName guard, but Lint doesn't understand the guard
    // and flags it as a NewApi violation. Baseline captures the known
    // false-positives; any new issue would still fail the build.
    lint {
        baseline = file("lint-baseline.xml")
        // AGP serializes baseline path variables per-variant; the
        // debug-generated baseline trips lintVitalRelease with a
        // "path variable not provided" error. Lint still gates debug
        // builds, which is what we ship in this demo.
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // iroh-ffi bindings consume these transitively when published. Until
    // then we declare them explicitly because we're pulling raw .kt source.
    implementation("net.java.dev.jna:jna:5.15.0@aar")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
