// Root build file. Individual module build files apply the plugins they need.
// (No root-level `clean` task here — each subproject already gets one from
// the Android/Kotlin plugins; a custom one using the now-deprecated
// `rootProject.buildDir` API isn't needed.)
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
