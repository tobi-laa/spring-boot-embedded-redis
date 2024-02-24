import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springBootVersion = "3.2.3"
val junitPlatformVersion = "1.10.2"
val jsonPathVersion = "2.9.0" // override transitive dep due to CVE violation
val embeddedRedisVersion = "1.4.1"
val mockkVersion = "1.13.9"
val archunitVersion = "1.2.1"

plugins {
    val springDependencyManagementVersion = "1.1.4"
    val kotlinVersion = "1.9.22"
    val adarshrTestLoggerVersion = "4.0.0"
    val sonarqubeVersion = "4.4.1.3373"
    val gradleReleasePluginVersion = "3.0.2"

    kotlin("jvm") version kotlinVersion
    id("io.spring.dependency-management") version springDependencyManagementVersion
    id("java-library")
    id("com.adarshr.test-logger") version adarshrTestLoggerVersion
    id("jacoco")
    id("org.sonarqube") version sonarqubeVersion
    id("maven-publish")
    id("signing")
    id("net.researchgate.release") version gradleReleasePluginVersion
}

group = "io.github.tobi-laa"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
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
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.jayway.jsonpath:json-path:$jsonPathVersion") // override transitive dep due to CVE violation
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    exclude("**/RedisValidationExtensionTest$*")
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${group}"
            artifactId = rootProject.name
            version = version
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name = "${group}:${rootProject.name}"
                description = "Integrates embedded-redis with Spring Boot"
                url = "https://github.com/tobias-laa/spring-boot-embedded-redis.git"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                        comments = "A business-friendly OSS license"
                    }
                }
                developers {
                    developer {
                        id = "tobi-laa"
                        name = "Tobias Laatsch"
                        email = "tobias.laatsch@posteo.de"
                        organizationUrl = "https://github.com/tobias-laa"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:tobias-laa/spring-boot-embedded-redis.git"
                    developerConnection = "scm:git:git@github.com:tobias-laa/spring-boot-embedded-redis.git"
                    url = "https://github.com/tobias-laa/spring-boot-embedded-redis/tree/master"
                    tag = "HEAD"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/tobias-laa/spring-boot-embedded-redis/issues"
                }
                inceptionYear = "2024"
            }
        }
    }
    repositories {
        maven {
            name = "ossrh"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_TOKEN")
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
}

release {
    tagTemplate = "v${version}"
}