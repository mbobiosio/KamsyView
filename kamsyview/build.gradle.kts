plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.dokka) // Add Dokka for documentation
    `maven-publish`
}

android {
    namespace = "com.github.kamsyview"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.addAll(
                listOf(
                    "-Xjsr305=strict",
                    "-opt-in=kotlin.RequiresOptIn"
                )
            )
        }
    }

    lint {
        targetSdk = 36
    }

    testOptions {
        targetSdk = 36
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module"
            )
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            // Remove this line to avoid duplicate javadoc JARs
            // withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    compileOnly(libs.bundles.image.loading)
    compileOnly(libs.bundles.image.loading.optional)
    ksp(libs.glide.compiler)

    implementation(libs.bundles.coroutines)
    implementation(libs.timber)

    compileOnly(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.bundles.testing)
    androidTestImplementation(libs.bundles.testing.android)
    kspAndroidTest(libs.hilt.android.compiler)
}

// Minimal Dokka V2 configuration
dokka {
    moduleName.set("KamsyView")
    moduleVersion.set("1.0.0")
}

// Create documentation JAR tasks using correct Dokka V2 task names
val javadocJar by tasks.registering(Jar::class) {
    dependsOn("dokkaGeneratePublicationHtml") // Use HTML as Javadoc equivalent
    from(layout.buildDirectory.dir("dokka/html"))
    archiveClassifier.set("javadoc")
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    dependsOn("dokkaGeneratePublicationHtml")
    from(layout.buildDirectory.dir("dokka/html"))
    archiveClassifier.set("dokka")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.mbobiosio"
                artifactId = "kamsyview"
                version = "1.0.0-SNAPSHOT"

                artifact(javadocJar)
                artifact(dokkaHtmlJar)

                pom {
                    name.set("KamsyView")
                    description.set("Advanced avatar view library for Android with intelligent fallback handling")
                    url.set("https://github.com/mbobiosio/KamsyView")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("mbobiosio")
                            name.set("Mbuodile Obiosio")
                            email.set("cazewonder@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/mbobiosio/KamsyView.git")
                        developerConnection.set("scm:git:ssh://github.com:mbobiosio/KamsyView.git")
                        url.set("https://github.com/mbobiosio/KamsyView/tree/main")
                    }
                }
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mbobiosio/KamsyView")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}
