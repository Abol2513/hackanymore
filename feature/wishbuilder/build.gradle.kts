plugins { alias(libs.plugins.android.library) }

android {
  namespace = "io.hexforge.feature.wishbuilder"
  compileSdk { version = release(36) { minorApiLevel = 1 } }
  defaultConfig { minSdk = 24 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:gatekeeper"))
  implementation(project(":core:chronicle"))
  implementation(project(":core:vault"))
}
