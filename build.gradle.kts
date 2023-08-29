import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.execution.ProgramText.Companion.from


plugins {
    java
    kotlin("jvm") version "1.7.20"
    `maven-publish`
    //id("com.palantir.docker") version "0.27.0"
    //id("com.palantir.docker-run") version "0.27.0"

    id("org.springframework.boot") version "2.5.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "com.codecad"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_16

//docker {
//    dependsOn(tasks.findByName("build"))
//    name = "${project.name}:${project.version}"
//    files( "build/libs/codecad-core-1.0-SNAPSHOT.jar")
//}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}


tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    requiresUnpack("**/kotlin-compiler-*.jar")
    requiresUnpack("**/kotlin-daemon-*.jar")
    requiresUnpack("**/kotlin-*.jar")
    requiresUnpack("**/kotlinx-*.jar")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation(kotlin("test-junit5"))
    //testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    //testImplementation("org.junit.platform:junit-platform-commons:1.10.0")
    //testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    testImplementation("junit:junit:4.13.2")

    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")

    implementation("com.codecad:codecad-common:1.0-SNAPSHOT")
    //implementation("de.lighti:Clipper:6.4.2")
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(kotlin("script-runtime"))
    implementation(kotlin("script-util"))
    implementation(kotlin("daemon"))
    implementation(kotlin("daemon-client"))
    implementation(kotlin("compiler"))
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlin("scripting-jsr223"))
    implementation(kotlin("scripting-compiler-embeddable"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.orbisgis:poly2tri:0.1.2")
    implementation("org.orbisgis:poly2tri-core:0.1.2")

    implementation("org.apache.commons:commons-lang3:3.12.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "16"
    targetCompatibility = "16"
}


tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "16"
}
