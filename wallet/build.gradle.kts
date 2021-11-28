import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.spring") version "1.5.31"
    kotlin("kapt") version "1.5.30"
}

group = "it.polito.wa2"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2020.0.4"

dependencies {
    implementation(project(":api"))
    implementation(project(":util"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Eureka client
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Mongo DB to store user information
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    // To support checking field-level constraints
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("javax.validation:validation-api")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-starter-stream-kafka")
    implementation("org.springdoc:springdoc-openapi-webflux-ui:1.5.9")

    implementation("org.mapstruct:mapstruct-jdk8:1.3.1.Final")
    kapt("org.mapstruct:mapstruct-processor:1.3.1.Final")


    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.5.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.4")
    implementation("org.springframework.kafka:spring-kafka:2.7.6")
    implementation("org.postgresql:postgresql:42.2.23")
    implementation("com.vladmihalcea:hibernate-types-52:2.12.1")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth:3.0.3")


    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
