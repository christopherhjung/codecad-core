import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    //java
    kotlin("jvm") version "1.5.20"
}

group = "com.codecad.core"
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
    //implementation("de.lighti:Clipper:6.4.2")

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(kotlin("script-runtime"))
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlin("script-util"))
    implementation("org.orbisgis:poly2tri:0.1.2")
    implementation("org.orbisgis:poly2tri-core:0.1.2")
    runtimeOnly(kotlin("scripting-compiler-embeddable"))
    runtimeOnly("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.5.20")





    //implementation("com.angusj.clipper:clipper-native:0.1.0-SNAPSHOT")
    //implementation("org.kabeja:kabeja:0.5.0")
    implementation(files("/usr/local/Cellar/opencv/4.5.2_4/share/java/opencv4/opencv-452.jar"))
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
