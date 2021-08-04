import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.execution.ProgramText.Companion.from


plugins {
    java
    kotlin("jvm") version "1.5.21"
    `maven-publish`
    id("com.palantir.docker") version "0.27.0"
    //id("com.palantir.docker-run") version "0.27.0"

    id("org.springframework.boot") version "2.5.3"
}

group = "com.codecad.core"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_16

docker {
    dependsOn(tasks.findByName("build"))
    name = "${project.name}:${project.version}"
    files( "build/libs/mil-1.0-SNAPSHOT.jar")
}


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
    requiresUnpack("**/clipper-native-*.jar")
    //requiresUnpack("**/kotlin-*.jar")
    //requiresUnpack("**/kotlinx-*.jar")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    //implementation("org.openpnp:opencv:4.5.1-2")
    //implementation("de.lighti:Clipper:6.4.2")


    implementation("de.lighti:Clipper:6.4.2")
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(kotlin("script-runtime"))
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlin("script-util"))
    implementation(kotlin("compiler"))
    implementation(kotlin("daemon"))
    implementation(kotlin("daemon-client"))
    implementation(kotlin("scripting-jsr223"))
    implementation(kotlin("scripting-compiler-embeddable"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.4")
    implementation("org.orbisgis:poly2tri:0.1.2")
    implementation("org.orbisgis:poly2tri-core:0.1.2")

    //implementation("de.lighti:Clipper:6.4.2")

    //implementation("com.angusj.clipper:clipper-native:0.1.0-SNAPSHOT")
    //implementation("org.kabeja:kabeja:0.5.0")
    //implementation(files("/usr/local/Cellar/opencv/4.5.2_4/share/java/opencv4/opencv-452.jar"))
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



/*
application {
    mainClassName = "MainKt"
}
*/
