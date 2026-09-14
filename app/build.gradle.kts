plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sandbox.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sandbox.app"
        minSdk = 26
        // Fixado em 28 de propósito, mais baixo que o compileSdk.
        // A partir de targetSdk 29, o SELinux do Android bloqueia
        // execve()/dlopen() em qualquer arquivo que o próprio app tenha
        // gravado depois de instalado (política W^X) — e é exatamente
        // isso que o rootfs extraído em filesDir precisa fazer o tempo
        // todo (bash, git, python3, node, gcc...). Apps com targetSdk <=28
        // ficam num domínio SELinux de compatibilidade
        // (untrusted_app_27) que ainda permite isso; é a mesma saída que
        // o Termux usou por anos (ver docs/proot-embedding.md).
        // Trade-off consciente: não instala/atualiza via Google Play (que
        // hoje exige targetSdk bem mais alto) — mas para instalação
        // manual/sideload, que é como este app é distribuído, funciona
        // normalmente. Ver alternativa "termux-exec" nesse mesmo doc caso
        // no futuro seja necessário publicar na Play mantendo um
        // targetSdk moderno.
        targetSdk = 28
        versionCode = 6
        versionName = "0.5.0" // Semanas 2 e 3: catálogo e gerenciamento de plugins
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "**/libproot.so"
            keepDebugSymbols += "**/libapp_proot_loader.so"
            keepDebugSymbols += "**/libandroid-shmem.so"
            keepDebugSymbols += "**/libtalloc.so"
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // targetSdk 28 is intentional: the proot runtime executes binaries
        // extracted into filesDir and currently relies on Android's legacy
        // SELinux compatibility domain (see the architecture notes).
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {
    implementation(project(":android-module"))
    implementation(project(":brain"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
