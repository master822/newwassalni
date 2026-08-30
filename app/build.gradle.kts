import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties
import java.util.Base64
import java.io.FileInputStream
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.wassalni.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    if (keystorePropsFile.exists()) {
      keystoreProps.load(FileInputStream(keystorePropsFile))
    }

    val storeFilePath = keystoreProps.getProperty("storeFile") 
      ?: System.getenv("KEYSTORE_PATH") 
      ?: "${rootDir}/my-upload-key.jks"
    val storePasswordVal = keystoreProps.getProperty("storePassword") ?: System.getenv("STORE_PASSWORD")
    val keyAliasVal = keystoreProps.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS") ?: "upload"
    val keyPasswordVal = keystoreProps.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")

    val releaseKeystoreFile = file(storeFilePath)
    if (releaseKeystoreFile.exists() && !storePasswordVal.isNullOrEmpty() && !keyPasswordVal.isNullOrEmpty()) {
      create("release") {
        storeFile = releaseKeystoreFile
        storePassword = storePasswordVal
        keyAlias = keyAliasVal
        keyPassword = keyPasswordVal
      }
    }
    val debugKeystoreFile = file("${rootDir}/debug.keystore")
    val debugKeystoreBase64File = file("${rootDir}/debug.keystore.base64")
    if (!debugKeystoreFile.exists() && debugKeystoreBase64File.exists()) {
      try {
        val decoded = Base64.getDecoder().decode(debugKeystoreBase64File.readText().trim())
        debugKeystoreFile.writeBytes(decoded)
      } catch (_: Exception) {
      }
    }
    if (debugKeystoreFile.exists()) {
      create("debugConfig") {
        storeFile = debugKeystoreFile
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      buildConfigField("String", "BASE_URL", "\"https://newwassalni-hm49.onrender.com/\"")
      val relConfig = signingConfigs.findByName("release")
        ?: signingConfigs.findByName("debugConfig")
        ?: signingConfigs.findByName("debug")
      if (relConfig != null) {
        signingConfig = relConfig
      }
    }
    debug {
      buildConfigField("String", "BASE_URL", "\"https://newwassalni-hm49.onrender.com/\"")
      val dbgConfig = signingConfigs.findByName("debugConfig") ?: signingConfigs.findByName("debug")
      if (dbgConfig != null) {
        signingConfig = dbgConfig
      }
    }
  }
  lint {
    abortOnError = false
    checkReleaseBuilds = false
    disable.add("InvalidFragmentVersionForActivityResult")
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.fragment.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.messaging)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.matching { it.name.startsWith("assemble") }.configureEach {
  doLast {
    val buildOutputDir = layout.buildDirectory.dir("outputs/apk").get().asFile
    val searchDirs = listOf(
      File(buildOutputDir, "release"),
      File(buildOutputDir, "debug"),
      buildOutputDir
    )
    var chosenApk: File? = null
    for (dir in searchDirs) {
      if (dir.exists()) {
        val apks = dir.listFiles { f -> f.isFile && f.extension == "apk" && f.name != "wassalni.apk" }
        if (!apks.isNullOrEmpty()) {
          chosenApk = apks.firstOrNull { it.name.contains("release") } ?: apks.first()
          break
        }
      }
    }
    chosenApk?.let { apk ->
      val targetApk = File(buildOutputDir, "wassalni.apk")
      apk.copyTo(targetApk, overwrite = true)
      val variantTarget = File(apk.parentFile, "wassalni.apk")
      apk.copyTo(variantTarget, overwrite = true)
      apk.copyTo(File(rootDir, "wassalni.apk"), overwrite = true)
      println("SUCCESS: wassalni.apk ready at ${targetApk.absolutePath}")
    }
  }
}
