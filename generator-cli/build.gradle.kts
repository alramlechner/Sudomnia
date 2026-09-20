// Standalone JVM tool -- deliberately not wired into android-app's Gradle build
// (own settings.gradle.kts, own gradlew) so it can't interfere with whatever is
// in progress there. Reuses android-app's already-cached Gradle/Kotlin versions.
plugins {
    kotlin("jvm") version "2.2.21"
    application
}

kotlin {
    jvmToolchain(17)
}

// rules/ exists only ONCE in the repo, namely in android-app -- the package has no
// Android imports, so this pure JVM tool can point straight at it. Up to 0.5.0 there
// were byte-identical copies here; that was only a matter of time until a change
// landed on one side only and the tool started measuring something different from
// what the app does.
sourceSets["main"].kotlin.srcDir("../android-app/app/src/main/java/name/lechners/sudomnia/rules")

application {
    mainClass.set("name.lechners.sudomnia.gen.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
