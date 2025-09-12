import org.jreleaser.model.Active

val springBootVersion = "3.5.6"
val junitPlatformVersion = "1.10.2"
val embeddedRedisVersion = "1.4.3"
val commonsValidatorVersion = "1.10.0"
val mockkVersion = "1.14.6"
val archunitVersion = "1.4.1"
val logunitVersion = "2.0.0"
val xmlunitVersion = "2.10.4"

plugins {
    val springDependencyManagementVersion = "1.1.7"
    val kotlinVersion = "1.9.25"
    val adarshrTestLoggerVersion = "4.0.0"
    val sonarqubeVersion = "7.0.1.6134"
    val gradleReleasePluginVersion = "3.1.0"
    val jreleaserVersion = "1.21.0"

    kotlin("jvm") version kotlinVersion
    id("io.spring.dependency-management") version springDependencyManagementVersion
    id("java-library")
    id("com.adarshr.test-logger") version adarshrTestLoggerVersion
    id("jacoco")
    id("org.sonarqube") version sonarqubeVersion
    id("maven-publish")
    id("signing")
    id("net.researchgate.release") version gradleReleasePluginVersion
    id("org.jreleaser") version jreleaserVersion
}

group = "io.github.tobi-laa"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withJavadocJar()
    withSourcesJar()
}

sonar {
    properties {
        property("sonar.projectKey", "tobi-laa_spring-boot-embedded-redis")
        property("sonar.organization", "tobi-laa")
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
    implementation("commons-validator:commons-validator:$commonsValidatorVersion")
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("io.github.netmikey.logunit:logunit-logback:$logunitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.xmlunit:xmlunit-core:$xmlunitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    testImplementation("io.github.netmikey.logunit:logunit-core:$logunitVersion")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    exclude("**/RedisValidationExtensionTest$*")
    exclude("**/RedisParameterResolverTest$*")
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
                url = "https://github.com/tobi-laa/spring-boot-embedded-redis.git"
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
                        organizationUrl = "https://github.com/tobi-laa"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:tobi-laa/spring-boot-embedded-redis.git"
                    developerConnection = "scm:git:git@github.com:tobi-laa/spring-boot-embedded-redis.git"
                    url = "https://github.com/tobi-laa/spring-boot-embedded-redis/tree/master"
                    tag = "HEAD"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/tobi-laa/spring-boot-embedded-redis/issues"
                }
                inceptionYear = "2024"
            }
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy").get().asFile.toURI())
        }
    }
}

jreleaser {
    signing {
        active = Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
    release {
        github {
            skipTag = true
            skipRelease = true
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
    preTagCommitMessage = ":rocket: pre tag commit:"
    tagCommitMessage = ":rocket: creating tag:"
    newVersionCommitMessage = ":rocket: new version commit:"
}
