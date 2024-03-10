plugins {
    kotlin("jvm") version "1.9.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

dependencies {
    implementation("org.springframework.security:spring-security-crypto:5.5.3")
    implementation("org.postgresql:postgresql:42.2.19")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}