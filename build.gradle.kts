import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springBootVersion = "3.2.2"
val embeddedRedisVersion = "1.4.1"
val mockkVersion = "1.13.9"

plugins {
    val springDependencyManagementVersion = "1.1.4"
    val kotlinVersion = "1.9.22"
    val adarshrTestLoggerVersion = "4.0.0"
    val sonarqubeVersion = "4.4.1.3373"

    id("io.spring.dependency-management") version springDependencyManagementVersion
    kotlin("jvm") version kotlinVersion
    id("com.adarshr.test-logger") version adarshrTestLoggerVersion
    id("jacoco")
    id("org.sonarqube") version sonarqubeVersion
}

group = "io.github.tobi.laa"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

sonar {
    properties {
        property("sonar.projectKey", "tobias-laa_spring-boot-embedded-redis")
        property("sonar.organization", "tobias-laa")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    api("com.github.codemonstur:embedded-redis:$embeddedRedisVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-test")
    implementation("org.springframework:spring-test")
    implementation("org.slf4j:slf4j-api")
    implementation("redis.clients:jedis")
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}