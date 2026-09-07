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

application {
    mainClass.set("name.lechners.sudomnia.gen.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
