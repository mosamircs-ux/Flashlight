import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.compose.compiler)
    base
}

base {
    archivesName.set("lumina-torch")
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

fun hasSigningVars(): Boolean {
    return providers.environmentVariable("SIGNING_KEY_ALIAS").orNull != null &&
        providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull != null &&
        providers.environmentVariable("SIGNING_STORE_FILE").orNull != null &&
        providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull != null
}

android {
    namespace = project.property("APP_ID").toString()
    compileSdk = libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        applicationId = project.property("APP_ID").toString()
        minSdk = libs.versions.app.build.minimumSDK.get().toInt()
        targetSdk = libs.versions.app.build.targetSDK.get().toInt()
        versionName = project.property("VERSION_NAME").toString()
        versionCode = project.property("VERSION_CODE").toString().toInt()

        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-4969792934066333~7355134341"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        } else if (hasSigningVars()) {
            create("release") {
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
                storeFile = file(providers.environmentVariable("SIGNING_STORE_FILE").get())
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
            }
        } else {
            logger.warn("Warning: No signing config found. Release build will be unsigned.")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Official Google AdMob Adaptive Banner test ad unit ID for debug builds
            buildConfigField(
                "String",
                "ADMOB_BANNER_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9214589741\""
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Production AdMob Banner Ad Unit ID for Lumina Torch
            buildConfigField(
                "String",
                "ADMOB_BANNER_AD_UNIT_ID",
                "\"ca-app-pub-4969792934066333/8004649848\""
            )
            if (keystorePropertiesFile.exists() || hasSigningVars()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    sourceSets {
        getByName("main").java.directories.add("src/main/kotlin")
    }

    compileOptions {
        val javaVer = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get())
        sourceCompatibility = javaVer
        targetCompatibility = javaVer
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.app.build.kotlinJVMTarget.get()))
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.lifecycle)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.google.ads)
}
