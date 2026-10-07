plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.dovecoteescapee.byedpi"

    // Compose 1.13 alphas (pulled in by Material3 1.5.0-alpha29) need compileSdk 37.1.
    // If your toolchain/SDK cannot do 37.1 yet, set `byedpi.compileSdkMinor=0` in gradle.properties
    // (and see README_M3E.md for the fallback ladder).
    compileSdk {
        version = release(37) {
            minorApiLevel = (providers.gradleProperty("byedpi.compileSdkMinor").orNull ?: "1").toInt()
        }
    }

    defaultConfig {
        applicationId = "io.github.dovecoteescapee.byedpi"
        minSdk = 28
        targetSdk = 34
        versionCode = 12
        versionName = "1.3.0-m3e"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Optional release signing. CI passes the keystore through environment variables;
    // without them the release build simply stays unsigned (the debug build is always installable).
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("SIGNING_KEYSTORE_FILE")
            if (!keystorePath.isNullOrEmpty() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}\"")

            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }

            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}-debug\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // https://android.izzysoft.de/articles/named/iod-scan-apkchecks?lang=en#blobs
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

// ---- native build (hev-socks5-tunnel via ndk-build) ----
// AGP 9 no longer exposes `android.ndkDirectory`; resolve the NDK through the components API instead.
val ndkDirProvider = androidComponents.sdkComponents.ndkDirectory

tasks.register<Exec>("runNdkBuild") {
    group = "build"

    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    val ndkBuildArgs = listOf(
        "NDK_PROJECT_PATH=build/intermediates/ndkBuild",
        "NDK_LIBS_OUT=src/main/jniLibs",
        "APP_BUILD_SCRIPT=src/main/jni/Android.mk",
        "NDK_APPLICATION_MK=src/main/jni/Application.mk"
    )

    doFirst {
        val ndkDir = ndkDirProvider.get().asFile
        val ndkBuild = File(ndkDir, if (isWindows) "ndk-build.cmd" else "ndk-build")
        commandLine(listOf(ndkBuild.absolutePath) + ndkBuildArgs)
        println("Command: $commandLine")
    }
}

tasks.preBuild {
    dependsOn("runNdkBuild")
}
