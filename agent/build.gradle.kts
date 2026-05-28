plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta4"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "io.github.nouhouari"
version = "0.1.0"

val javalinVersion: String by project
val jacksonVersion: String by project
val slf4jVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.graphics", "javafx.base", "javafx.swing")
    configuration = "compileOnly"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:$javalinVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")

    testImplementation("org.openjfx:javafx-base:21:${getJavafxPlatform()}")
    testImplementation("org.openjfx:javafx-graphics:21:${getJavafxPlatform()}")
    testImplementation("org.openjfx:javafx-controls:21:${getJavafxPlatform()}")
    testImplementation("org.openjfx:javafx-swing:21:${getJavafxPlatform()}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

fun getJavafxPlatform(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        os.contains("mac") && arch.contains("aarch64") -> "mac-aarch64"
        os.contains("mac") -> "mac"
        os.contains("win") -> "win"
        os.contains("linux") && arch.contains("aarch64") -> "linux-aarch64"
        else -> "linux"
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyJavaFxLibs") {
    from(configurations.named("testRuntimeClasspath").get().filter { it.name.contains("javafx") })
    into(layout.buildDirectory.dir("javafx-libs"))
    dependsOn("compileTestJava")
}

tasks.register("prepareShowcase") {
    dependsOn("compileTestJava", "shadowJar", "copyJavaFxLibs")
    description = "Prepares everything needed to launch the ShowcaseApp"
}

tasks.shadowJar {
    mergeServiceFiles()

    archiveBaseName.set("fxagent")
    archiveVersion.set("")

    manifest {
        attributes(
            "Premain-Class" to "com.sicpa.fxagent.FxAgent",
            "Agent-Class" to "com.sicpa.fxagent.FxAgent",
            "Can-Redefine-Classes" to "false",
            "Can-Retransform-Classes" to "false",
            "Implementation-Title" to "FxAgent",
            "Implementation-Version" to project.version
        )
    }

    relocate("org.eclipse.jetty", "com.sicpa.fxagent.shaded.jetty")
    relocate("io.javalin", "com.sicpa.fxagent.shaded.javalin")
    relocate("com.fasterxml.jackson", "com.sicpa.fxagent.shaded.jackson")
    relocate("org.slf4j", "com.sicpa.fxagent.shaded.slf4j")
    relocate("jakarta.servlet", "com.sicpa.fxagent.shaded.jakarta.servlet")

    exclude("javafx/**")
    exclude("com/sun/javafx/**")

    archiveClassifier.set("")
}
