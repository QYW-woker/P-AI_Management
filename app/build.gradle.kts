// App模块 build.gradle.kts
// AI智能生活管理APP - 应用级构建配置

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.lifemanager.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lifemanager.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Room数据库schema导出位置
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        // 启用Java 8 Time API desugaring，确保在所有Android版本上兼容
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ==================== Core Library Desugaring ====================
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ==================== Compose BOM ====================
    // 使用与 Kotlin 1.9.20 和 Compose Compiler 1.5.5 兼容的 BOM 版本
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI核心
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Material 3设计
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")

    // 显式指定 animation-core 版本以避免版本冲突
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")

    // Material Icons扩展
    implementation("androidx.compose.material:material-icons-extended")

    // ==================== Compose Navigation ====================
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ==================== Lifecycle ====================
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ==================== Room Database ====================
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ==================== DataStore ====================
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ==================== Hilt依赖注入 ====================
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // ==================== Network (AI API) ====================
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ==================== Kotlin序列化 ====================
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // ==================== 图表库 (Vico) ====================
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")

    // ==================== 日期时间 ====================
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // ==================== 协程 ====================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ==================== WorkManager (提醒功能) ====================
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ==================== Activity Compose ====================
    implementation("androidx.activity:activity-compose:1.8.2")

    // ==================== Core KTX ====================
    implementation("androidx.core:core-ktx:1.12.0")

    // ==================== ML Kit OCR (图片文字识别) ====================
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")

    // ==================== 图片加载 (Coil) ====================
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-video:2.5.0")

    // ==================== 权限处理 (Accompanist) ====================
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // ==================== 测试依赖 ====================
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
