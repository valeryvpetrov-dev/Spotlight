plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  compileSdk = 32
  defaultConfig {
    minSdk = 14
  }
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.22")
  implementation("androidx.appcompat:appcompat:1.6.1")
}

apply { from("../publish.gradle") }
