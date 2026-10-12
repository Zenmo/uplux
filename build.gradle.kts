import java.net.URI

plugins {
    id("java")
    id("io.freefair.lombok") version "8.14.2"
    `maven-publish`
}

group = "energy.lux"
version = System.getenv("VERSION_TAG") ?: "dev"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.minio:minio:8.5.17")

    // For verifying JWTs
    implementation("com.nimbusds:nimbus-jose-jwt:10.5")
    implementation("com.google.crypto.tink:tink:1.19.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "energy.lux"
            artifactId = "uplux"
            version = System.getenv("VERSION_TAG") ?: "dev"

            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/zenmo/uplux")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
