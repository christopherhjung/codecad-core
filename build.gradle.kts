import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
}

group = "me.chris"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_16

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    //implementation("org.openpnp:opencv:4.5.1-2")
    implementation("de.lighti:Clipper:6.4.2")

    implementation(files("/usr/local/Cellar/opencv/4.5.2_4/share/java/opencv4/opencv-452.jar"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "15"
    targetCompatibility = "15"
}


tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "15"
}
/*
application {
    mainClassName = "MainKt"
}
*/
