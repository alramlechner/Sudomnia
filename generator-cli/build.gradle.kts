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

// rules/ liegt nur EINMAL im Repo, naemlich in android-app -- das Paket hat keine
// Android-Importe, also kann dieses reine JVM-Werkzeug direkt darauf zeigen. Bis
// 0.5.0 lagen hier byte-gleiche Kopien; die waren eine Frage der Zeit, bis eine
// Aenderung nur auf einer Seite ankommt und das Werkzeug etwas anderes misst als
// die App tut.
sourceSets["main"].kotlin.srcDir("../android-app/app/src/main/java/name/lechners/sudomnia/rules")

application {
    mainClass.set("name.lechners.sudomnia.gen.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
