// Top-level build file where you can add configuration options common to all sub-modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

buildscript {
    dependencies {
        classpath(libs.gradle)
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}