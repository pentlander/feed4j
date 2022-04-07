plugins {
    java
    application
}

group = "com.pentlander"
version = "1.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.pentlander.feed4j.Main")
}

dependencies {
    implementation("gg.jte:jte:2.0.1")
    implementation("com.rometools:rome:1.18.0")

    runtimeOnly("org.slf4j:slf4j-nop:1.7.36")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}