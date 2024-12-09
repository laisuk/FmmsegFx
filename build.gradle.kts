plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.0.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

tasks.wrapper {
    // You can either download the binary-only version of Gradle (BIN) or
    // the full version (with sources and documentation) of Gradle (ALL)
    gradleVersion = "8.11.1"
    distributionType = Wrapper.DistributionType.ALL
}

repositories {
    mavenCentral()
}

val junitVersion = "5.10.3"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec> {
    args("-Dfile.encoding=UTF-8")
}

application {
    mainModule.set("org.example.demofx")
    mainClass.set("org.example.demofx.FmmsegFxApplication")
}

javafx {
    version = "23.0.1"
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
//    implementation(files("lib/OpenCCJava.jar"))
//    implementation(fileTree("lib") {include("*.jar")})
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.example.demofx.FmmsegFxApplication"
    }
}

tasks.test {
    systemProperty("file.encoding", "utf-8")
    useJUnitPlatform()
}

jlink {
    imageZip = project.file("${layout.buildDirectory}/distributions/app-${javafx.platform.classifier}.zip")
    options = listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    launcher {
        name = "app"
    }
}

tasks.named("jlinkZip") {
    group = "distribution"
}

tasks.register<Copy>("copyNativeLibs") {
    val sourceDir = file("src/main/java/opencc") // Source directory containing your DLLs
    from(sourceDir) // Source directory containing your DLLs
    include("*.dll") // Include all DLL files
    into(layout.buildDirectory.dir("libs")) // Copy them to the libs directory

    /// Use this to enable "copy if newer" behavior
    outputs.upToDateWhen {
        val outputFiles = fileTree(destinationDir).files.filter { it.extension == "dll" }
        outputFiles.all { outputFile ->
            val inputFile = file("$sourceDir/${outputFile.name}")
            val isUpToDate = inputFile.exists() && outputFile.lastModified() >= inputFile.lastModified()
            println("Checking $inputFile against $outputFile: $isUpToDate")
            isUpToDate
        }
    }
}

tasks.named("build") { finalizedBy("copyNativeLibs") }

distributions {
    main {
        contents {
            into ("bin") {
                from(layout.buildDirectory.dir("libs")) {
                    include("OpenccWrapper.dll", "opencc_fmmseg_capi.dll") // Include the DLL in the distribution package
                }
            }
        }
    }
}