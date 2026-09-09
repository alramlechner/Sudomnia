import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.play.publisher)
}

// The version lives in version.properties at the repository root, so that a
// release only ever has to touch one file.
val versionProps = Properties().apply {
    file("../../version.properties").inputStream().use { load(it) }
}

android {
    namespace = "name.lechners.sudomnia"
    compileSdk = 36

    defaultConfig {
        applicationId = "name.lechners.sudomnia"
        // Nothing here needs Android 15. 30 is the comfortable floor, same as
        // Chessomnia; the hard floor would be 26.
        minSdk = 30
        targetSdk = 36
        versionCode = versionProps.getProperty("VERSION_CODE").toInt()
        versionName = versionProps.getProperty("VERSION_NAME")
    }

    // Zwei Auslieferungswege, zwei Varianten -- und der Unterschied ist nicht ein
    // Schalter zur Laufzeit, sondern welche Dateien ueberhaupt uebersetzt werden.
    //
    //   play        fuer Google Play. Ohne update/, ohne Client-Zertifikat, ohne eine
    //               einzige Berechtigung. Play verbietet Apps aus dem Store, sich auf
    //               einem anderen Weg selbst zu aktualisieren. Nebeneffekt, der genauso
    //               wichtig ist: ein frischer Clone uebersetzt diese Variante ohne
    //               jedes Geheimnis -- vorher scheiterte er an R.raw.sudomnia_client.
    //   selfhosted  wie bisher: holt sich neue Versionen vom EnergyControl-Server im
    //               Haus (deploy.sh baut diese Variante).
    //
    // Gleiche applicationId in beiden: die Familientablets sollen zwischen Haus-APK und
    // Store-Version wechseln koennen, ohne Statistik und laufendes Spiel zu verlieren.
    // Das setzt voraus, dass beide mit demselben Schluessel signiert sind -- siehe
    // RELEASING.md zu Play App Signing.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") { dimension = "distribution" }
        create("selfhosted") { dimension = "distribution" }
    }

    // Belt and braces next to the play { } block below: the Play tasks of the
    // selfhosted flavour are switched off outright, so a stray `publish` cannot
    // upload a build carrying INTERNET and REQUEST_INSTALL_PACKAGES. That is the one
    // mistake in this setup that could not be taken back quietly.
    playConfigs {
        register("selfhosted") { enabled.set(false) }
    }

    // Release signing is driven by an untracked keystore.properties (see
    // keystore.properties.example). Without it the release build stays unsigned,
    // which is what a fork or a CI check wants -- and it keeps the key and the
    // passwords out of the repository.
    signingConfigs {
        val keystoreProperties = Properties().apply {
            val f = rootProject.file("keystore.properties")
            if (f.isFile) f.inputStream().use { load(it) }
        }
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        // Die Einstellungen zeigen die App-Version an, und version.properties ist die
        // einzige Quelle dafuer -- ueber BuildConfig kommt sie ohne PackageManager an.
        buildConfig = true
    }

    lint {
        // Lint belongs in the development loop, not in the release path.
        // Run it deliberately: ./gradlew lintRelease
        checkReleaseBuilds = false
        textReport = true
    }
}

// Publishing to Google Play. The service account key is untracked -- see
// play-service-account.json.example. Without it every ordinary task still works;
// only the publish* tasks fail, and they fail with a clear message rather than
// silently doing nothing.
//
// The defaults here are deliberately harmless. `publishPlayBundle` with no further
// arguments goes to the INTERNAL track, which is a named list of at most 100
// testers -- not the store. Shipping to the public is an explicit act:
//
//     ./gradlew publishPlayBundle --track production
//
// Only the play flavour is publishable at all: the selfhosted one carries the
// self-update that Google's policy forbids, and uploading it would be the one
// mistake in this whole setup that cannot be taken back quietly.
//
// The API cannot create the very first release of an app. Google requires one
// bundle to be uploaded through the Play Console by hand before the Developer API
// will accept anything for that package.
play {
    val credentials = rootProject.file("play-service-account.json")
    if (credentials.isFile) serviceAccountCredentials.set(credentials)
    defaultToAppBundles.set(true)
    track.set("internal")
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    // org.json steckt in der android.jar nur als Stub, der in Unit-Tests wirft. Die
    // Referenz-Implementierung auf dem Test-Classpath macht ReleaseInfo.parse testbar.
    testImplementation(libs.json)
}

// Slow generator tests run only on request: ./gradlew test -DsudokuDeep=1
// Without forwarding it, the test JVM would not see the Gradle process's property.
tasks.withType<Test>().configureEach {
    System.getProperty("sudokuDeep")?.let { systemProperty("sudokuDeep", it) }
}
