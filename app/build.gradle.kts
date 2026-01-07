plugins {
    id("com.android.application")
}

android {
    namespace = "com.gamebooster.launcher"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.gamebooster.launcher"
        minSdk = 24
        targetSdk = 33
        versionCode = 2
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
}
