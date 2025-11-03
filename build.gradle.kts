plugins {
    id("java")
    id("io.freefair.lombok") version "8.14.2"
    id("com.gradleup.shadow") version "9.0.2"
}

group = "energy.lux.uplux"
version = "2"

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
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    // remove "-all" from the jar file name
    archiveClassifier.set("")

    dependencies {
        exclude(dependency("com.fasterxml.jackson.core:.*:.*"))
    }
}
