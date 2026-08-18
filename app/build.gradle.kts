plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  // google-services.json is intentionally NOT committed (it contains the API
  // key). Apply the plugin only when the file is present so CI/clean clones
  // can still compile; local builds activate it automatically.
  alias(libs.plugins.google.services) apply false
}

val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
  apply(plugin = "com.google.gms.google-services")
}

android {
  namespace = "com.kaamio.nepal"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.kaamio.nepal"
    minSdk = 24
    targetSdk = 35
    versionCode = 2
    versionName = "1.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Firebase project config is supplied via environment variables or the
    // Secrets plugin (.env). Never committed as literals in source. When absent
    // and google-services.json does not auto-initialize Firebase, KaamioApplication
    // skips manual init (fail loudly) instead of pinning a stale committed config.
    val firebaseApiKey = System.getenv("FIREBASE_API_KEY") ?: (findProperty("FIREBASE_API_KEY") as? String ?: "")
    val firebaseAppId = System.getenv("FIREBASE_APP_ID") ?: (findProperty("FIREBASE_APP_ID") as? String ?: "")
    val firebaseProjectId = System.getenv("FIREBASE_PROJECT_ID") ?: (findProperty("FIREBASE_PROJECT_ID") as? String ?: "")
    buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")
    buildConfigField("String", "FIREBASE_APP_ID", "\"$firebaseAppId\"")
    buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      // Production Khalti public key must be provided by CI via KHALTI_PUBLIC_KEY env var.
      buildConfigField("String", "KHALTI_PUBLIC_KEY", "\"${System.getenv("KHALTI_PUBLIC_KEY") ?: "test_public_key_dc74e0fd57cb46cd93932ee953a07897"}\"")
      buildConfigField("String", "KHALTI_ENV", "\"PROD\"")
      buildConfigField("boolean", "IS_RELEASE", "true")
    }
    debug {
      isMinifyEnabled = false
      // Test public key lets the sandbox flow exercise the UI/integration path.
      buildConfigField("String", "KHALTI_PUBLIC_KEY", "\"test_public_key_dc74e0fd57cb46cd93932ee953a07897\"")
      buildConfigField("String", "KHALTI_ENV", "\"TEST\"")
      buildConfigField("boolean", "IS_RELEASE", "false")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }

  bundle {
    language {
      enableSplit = false
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

roborazzi {
  outputDir.set(project.layout.projectDirectory.dir("src/test/screenshots"))
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

// Ensure the Roborazzi output directory system property reaches the test JVM
// so captured screenshots land in src/test/screenshots (the VRT baseline).
tasks.withType<Test>().configureEach {
  systemProperty(
    "roborazzi.output.dir",
    project.layout.projectDirectory.dir("src/test/screenshots").asFile.absolutePath
  )
  systemProperty("roborazzi.record.filePathStrategy", "relativePathFromRoborazziContextOutputDirectory")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.functions)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.messaging)
  implementation(libs.khalti.checkout)
  implementation(libs.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.hilt.android)
  implementation(libs.androidx.hilt.navigation.compose)
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
  "ksp"(libs.hilt.compiler)
}
