plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.takusemba.spotlight"

  compileSdk = 32
  defaultConfig {
    minSdk = 14
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = "1.8"
  }
}



dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.22")
  implementation("androidx.appcompat:appcompat:1.6.1")
}

apply { from("../publish.gradle") }
