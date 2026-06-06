// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.0.0" apply false
    id("com.android.library") version "8.0.0" apply false
    kotlin("android") version "1.9.0" apply false
}

subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("com.android.library") || plugins.hasPlugin("com.android.application")) {
            android {
                compileSdk = 34
                
                defaultConfig {
                    minSdk = 21
                    targetSdk = 34
                }
            }
        }
    }
}
