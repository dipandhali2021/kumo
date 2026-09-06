import java.net.URL
import java.security.MessageDigest

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
  versionCode = 6
  versionName = "1.5.0"
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
 sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/japaneseAssets"))
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.compose.ui:ui")
 implementation("io.coil-kt:coil-compose:2.7.0")
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
 testImplementation("org.json:json:20240303")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
}

val prepareJapaneseFonts by tasks.registering {
 val output=layout.buildDirectory.dir("generated/japaneseAssets/fonts")
 val revision="5e35378e6bda803962ee6fd257e444a7d459660d"
 val files=listOf(
  Triple("zenkakugothicnew/ZenKakuGothicNew-Regular.ttf","gothic_regular.ttf","9940304caecd27df011ef6d15af9e187713967be"),
  Triple("zenkakugothicnew/ZenKakuGothicNew-Bold.ttf","gothic_bold.ttf","e8143e657afdb0a74da509cb07e007592105d271"),
  Triple("zenoldmincho/ZenOldMincho-Regular.ttf","mincho_regular.ttf","0086880c10a9925338a87b1b1e199e1ef6e9ca39"),
  Triple("zenoldmincho/ZenOldMincho-Bold.ttf","mincho_bold.ttf","7eaaeee9edb9f2bbbfd64568f6fdb2691885e71e"),
  Triple("zenkakugothicnew/OFL.txt","gothic_OFL.txt","c05130c2195586600c9bc245c88a67d1154369a8"),
  Triple("zenoldmincho/OFL.txt","mincho_OFL.txt","665345ba7787f0bfb9c5942e2a02a28b1082aa68")
 )
 inputs.property("revision",revision)
 inputs.property("files",files.joinToString())
 outputs.dir(output)
 doLast {
  val directory=output.get().asFile.apply {mkdirs()}
  fun hash(data: ByteArray): String {
   val digest=MessageDigest.getInstance("SHA-1")
   digest.update("blob ${data.size}\u0000".toByteArray(Charsets.UTF_8))
   return digest.digest(data).joinToString("") {"%02x".format(it.toInt() and 255)}
  }
  files.forEach {(path,name,expected) ->
   val destination=directory.resolve(name)
   if(!destination.isFile || hash(destination.readBytes())!=expected) {
    val connection=URL("https://raw.githubusercontent.com/google/fonts/$revision/ofl/$path").openConnection()
    connection.connectTimeout=20_000;connection.readTimeout=40_000
    val data=connection.getInputStream().use {it.readNBytes(8*1024*1024+1)}
    check(data.size<=8*1024*1024 && hash(data)==expected) {"Font integrity check failed: $name"}
    destination.writeBytes(data)
   }
  }
 }
}
tasks.named("preBuild").configure {dependsOn(prepareJapaneseFonts)}
tasks.configureEach {
 if(name.startsWith("merge") && name.endsWith("Assets")) dependsOn(prepareJapaneseFonts)
}
