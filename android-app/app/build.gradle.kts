import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
