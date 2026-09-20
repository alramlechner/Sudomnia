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

// The update host name is a per-deployment setting, not something that belongs in the
// repository -- it names this developer's own server. It lives in the untracked
// local.properties (see .gitignore) under "sudomnia.updateHost"; a fresh clone falls
// back to a placeholder that resolves nowhere (RFC 2606), so the selfhosted flavour
// still compiles, it just cannot reach an update server until configured.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.isFile) f.inputStream().use { load(it) }
}
val updateHost = localProps.getProperty("sudomnia.updateHost") ?: "sudomnia.invalid"

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

    // Two distribution paths, two flavours -- and the difference is not a runtime
    // switch but which files get compiled at all.
    //
    //   play        for Google Play. No update/, no client certificate, not a
    //               single permission. Play forbids apps from the store updating
    //               themselves any other way. A side effect that matters just as
    //               much: a fresh clone compiles this flavour without any secret --
    //               before, it failed on R.raw.sudomnia_client.
    //   selfhosted  as before: fetches new versions from the EnergyControl server
    //               at home (deploy.sh builds this flavour).
    //
    // Same applicationId in both: the family tablets should be able to switch
    // between the house APK and the store version without losing statistics and
    // the running game. That requires both to be signed with the same key -- see
    // RELEASING.md on Play App Signing.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") { dimension = "distribution" }
        create("selfhosted") {
            dimension = "distribution"
            // See updateHost above -- UpdateClient.BASE_URL reads this via BuildConfig.
            buildConfigField("String", "UPDATE_HOST", "\"$updateHost\"")
        }
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
        // The settings screen shows the app version, and version.properties is the
        // single source for it -- BuildConfig gets it there without a PackageManager call.
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
    // org.json only exists in android.jar as a stub that throws in unit tests. The
    // reference implementation on the test classpath is what makes ReleaseInfo.parse testable.
    testImplementation(libs.json)
}

// Slow generator tests run only on request: ./gradlew test -DsudokuDeep=1
// Without forwarding it, the test JVM would not see the Gradle process's property.
tasks.withType<Test>().configureEach {
    System.getProperty("sudokuDeep")?.let { systemProperty("sudokuDeep", it) }
}
