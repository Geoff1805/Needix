import java.util.Base64
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

configurations.configureEach {
  resolutionStrategy.eachDependency {
    when {
      requested.group == "io.netty" &&
        requested.name != "netty-tcnative-boringssl-static" &&
        requested.name != "netty-tcnative-classes" -> useVersion("4.1.139.Final")
      requested.group == "org.bouncycastle" &&
        requested.name in setOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on") -> useVersion("1.86")
      requested.group == "org.jdom" && requested.name == "jdom2" -> useVersion("2.0.6.1")
      requested.group == "org.apache.httpcomponents" && requested.name == "httpclient" -> useVersion("4.5.14")
      requested.group == "org.apache.commons" && requested.name == "commons-lang3" -> useVersion("3.21.0")
      requested.group == "org.apache.commons" && requested.name == "commons-compress" -> useVersion("1.28.0")
      requested.group == "commons-io" && requested.name == "commons-io" -> useVersion("2.22.0")
      requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" -> useVersion("0.9.7")
      requested.group == "com.google.guava" && requested.name == "guava" -> useVersion("33.7.2-jre")
      requested.group == "com.google.protobuf" &&
        requested.name in setOf("protobuf-java", "protobuf-java-util", "protobuf-kotlin") -> useVersion("3.25.9")
    }
  }
}

android {
  namespace = "com.example"
  compileSdk = 37

  val versionPropsFile = file("${rootDir}/version.properties")
  val versionProps = Properties()
  if (versionPropsFile.exists()) {
    versionProps.load(versionPropsFile.inputStream())
  }
  val envVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull()
  val appVersionCode = envVersionCode ?: (versionProps.getProperty("versionCode", "1").toIntOrNull() ?: 1)
  val appVersionName = versionProps.getProperty("versionName", "1.0.0")

  val keystoreFile = file("${rootDir}/debug.keystore")
  val keystoreBase64File = file("${rootDir}/debug.keystore.base64")
  if (!keystoreFile.exists() && keystoreBase64File.exists()) {
    val decodedBytes = Base64.getMimeDecoder().decode(keystoreBase64File.readText().trim())
    keystoreFile.writeBytes(decodedBytes)
  }

  val envProps = Properties()
  val envExampleFile = file("${rootDir}/.env.example")
  if (envExampleFile.exists()) {
    envProps.load(envExampleFile.inputStream())
  }
  val envFile = file("${rootDir}/.env")
  if (envFile.exists()) {
    envProps.load(envFile.inputStream())
  }
  val chompApiKey = envProps.getProperty("CHOMP_API_KEY", System.getenv("CHOMP_API_KEY") ?: "")

  defaultConfig {
    applicationId = "com.aistudio.inventory.hmtst"
    minSdk = 24
    targetSdk = 37
    versionCode = appVersionCode
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "CHOMP_API_KEY", "\"$chompApiKey\"")
  }

  signingConfigs {
    getByName("debug") {
      storeFile = keystoreFile
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    create("release") {
      val customPath = System.getenv("KEYSTORE_PATH")
      val releaseKeystore = if (!customPath.isNullOrEmpty() && file(customPath).exists()) {
        file(customPath)
      } else if (file("${rootDir}/release-keystore.jks").exists()) {
        file("${rootDir}/release-keystore.jks")
      } else if (file("${rootDir}/my-upload-key.jks").exists()) {
        file("${rootDir}/my-upload-key.jks")
      } else {
        keystoreFile
      }
      storeFile = releaseKeystore
      storePassword = System.getenv("STORE_PASSWORD")?.takeUnless { it.isBlank() } ?: "android"
      keyAlias = System.getenv("KEY_ALIAS")?.takeUnless { it.isBlank() } ?: "androiddebugkey"
      keyPassword = System.getenv("KEY_PASSWORD")?.takeUnless { it.isBlank() } ?: "android"
    }
  }

  lint {
    abortOnError = false
    checkReleaseBuilds = false
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        it.jvmArgs(
          "--add-opens=java.base/java.lang=ALL-UNNAMED",
          "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
          "--add-opens=java.base/java.io=ALL-UNNAMED"
        )
      }
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
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
