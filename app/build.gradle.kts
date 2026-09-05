plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "com.kotobawall.app"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.kotobawall.app"
  minSdk = 24
  targetSdk = 35
  versionCode = 2
  versionName = "1.1.0"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 buildTypes {
  release {
   isMinifyEnabled = true
   isShrinkResources = true
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  }
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 implementation("androidx.core:core-ktx:1.16.0")
 implementation("androidx.work:work-runtime-ktx:2.10.1")
 implementation("androidx.exifinterface:exifinterface:1.4.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
}
