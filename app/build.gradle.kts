plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.cookmate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cookmate"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    //import của thư viện Volley để gửi và nhận dữ liệu qua API (HTTP request)
    implementation("com.android.volley:volley:1.2.1")
    //import thư viện Google để lấy thông tin email trên máy
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    //import thư viện glide Tải ảnh từ URL, URI, file local rồi lưu cache
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    // import thư viện gson để chuyển đổi dữ liệu từ Java Object → JSON và ngược lại
    implementation("com.google.code.gson:gson:2.11.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}