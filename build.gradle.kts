plugins {
    kotlin("jvm") version "2.0.20"
    `java-library`
    `maven-publish`
}

group = "cc.mewcraft"
version = "1.5"

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)

    sourceSets {
        val main by getting {
            dependencies {
                compileOnly(kotlin("stdlib"))
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

publishing {
    repositories {
        maven {
            name = "nyaadanbou"
            url = uri("https://repo.mewcraft.cc/releases/")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
