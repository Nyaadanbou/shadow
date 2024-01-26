plugins {
    kotlin("jvm") version "1.9.22"
    `java-library`
    `maven-publish`
}

group = "cc.mewcraft"
version = "1.4"

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)

    sourceSets {
        val main by getting {
            dependencies {
                compileOnly(kotlin("stdlib"))
                compileOnly("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
            }
        }

        val test by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    test {
        useJUnitPlatform()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.checkerframework", "checker-qual", "3.42.0")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.9.1")
    testImplementation("org.junit.jupiter", "junit-jupiter-engine", "5.9.1")
    testImplementation("org.checkerframework", "checker-qual", "3.42.0")
}

val userHome: String = when {
    System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> System.getenv("USERPROFILE")
    else -> System.getenv("HOME")
}

publishing {
    repositories {
        maven {
            url = uri("$userHome/MewcraftRepository")
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
