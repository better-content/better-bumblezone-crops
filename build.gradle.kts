import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.UUID

plugins { java; id("net.minecraftforge.gradle") version "6.0.54"; id("org.spongepowered.mixin") version "0.7.38" }
group = "com.bettercontent"
version = property("mod_version") as String
base { archivesName.set(property("artifact_name") as String) }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)); withSourcesJar() }
// Native worlds are retained, never recycled or placed beneath Gradle's cleanable build tree.
val gameTestAttempts = project.file("run-gametest/attempts").toPath()
require(!Files.isSymbolicLink(gameTestAttempts) && !Files.isSymbolicLink(gameTestAttempts.parent)) { "GameTest attempt ancestry must not be a symlink" }
Files.createDirectories(gameTestAttempts)
val gameTestAttemptId = providers.gradleProperty("nativeGameTestAttemptId").orNull?.let { UUID.fromString(it).toString() }
    ?: UUID.randomUUID().toString()
val gameTestAttempt = Files.createDirectory(gameTestAttempts.resolve(gameTestAttemptId)).toFile()
logger.lifecycle("Native GameTest fresh attempt admitted: ${gameTestAttempt.absolutePath}; existing world=false")
minecraft {
    mappings("official", property("minecraft_version") as String)
    copyIdeResources = true
    runs {
        configureEach { workingDirectory(project.file("run")); property("forge.logging.console.level", "info"); property("mixin.env.remapRefMap","true"); property("mixin.env.refMapRemappingFile","${projectDir}/build/createSrgToMcp/output.srg"); mods { create(property("mod_id") as String) { source(sourceSets.main.get()) } } }
        create("client"); create("server") { arg("--nogui") }
        create("gameTestServer") { workingDirectory(gameTestAttempt); property("forge.enableGameTest","true"); property("forge.gameTestServer","true"); property("forge.enabledGameTestNamespaces","better_bumblezone_crops"); arg("--nogui") }
    }
}
repositories { maven("https://maven.minecraftforge.net"); maven("https://cursemaven.com"); mavenCentral() }
val bumblezoneDevelopment by configurations.creating
val remapBumblezoneDevelopment by tasks.registering(Exec::class) {
    dependsOn("createSrgToMcp")
    val output=layout.buildDirectory.file("development-dependencies/bumblezone-mapped.jar")
    inputs.files(bumblezoneDevelopment); inputs.file("scripts/remap-test-mixin-strings.py"); inputs.file(layout.buildDirectory.file("createSrgToMcp/output.srg")); outputs.file(output)
    doFirst { commandLine("python3","scripts/remap-test-mixin-strings.py",bumblezoneDevelopment.singleFile.absolutePath,layout.buildDirectory.file("createSrgToMcp/output.srg").get().asFile.absolutePath,output.get().asFile.absolutePath) }
}
dependencies {
    // Native wild-crop GameTests use the same Farmer's Delight file pinned by the pack.
    runtimeOnly(fg.deobf("curse.maven:farmers-delight-398521:8007609"))
    compileOnly(fg.deobf("curse.maven:the-bumblezone-362479:8548194")); add(bumblezoneDevelopment.name,fg.deobf("curse.maven:the-bumblezone-362479:8548194")); runtimeOnly(files(layout.buildDirectory.file("development-dependencies/bumblezone-mapped.jar")).builtBy(remapBumblezoneDevelopment)); annotationProcessor("org.spongepowered:mixin:0.8.5:processor"); minecraft("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}"); testImplementation("org.junit.jupiter:junit-jupiter:5.10.2") }
tasks.named<Jar>("jar") { finalizedBy("reobfJar") }
val stageRuntimeJar by tasks.registering(Copy::class) { dependsOn(tasks.named("reobfJar")); from(layout.buildDirectory.file("reobfJar/output.jar")); into(layout.buildDirectory.dir("libs")); rename { "${base.archivesName.get()}-$version.jar" } }
tasks.named("assemble") { dependsOn(stageRuntimeJar) }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
tasks.test { useJUnitPlatform() }
tasks.register("verifyFast") { dependsOn(tasks.named("check")) }
tasks.register("verifyFull") { dependsOn(tasks.named("verifyFast"),tasks.named("runGameTestServer")) }
tasks.processResources {
    val props = mapOf("minecraft_version" to project.property("minecraft_version"), "forge_version" to project.property("forge_version"), "mod_id" to project.property("mod_id"), "mod_name" to project.property("mod_name"), "mod_version" to project.property("mod_version"))
    inputs.properties(props); filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) { expand(props) }
}

mixin { add(sourceSets.main.get(), "better_bumblezone_crops.refmap.json"); config("better_bumblezone_crops.mixins.json") }

val syncGameTestStructures by tasks.registering(Copy::class) { from("src/main/resources/gameteststructures"); into(gameTestAttempt.resolve("gameteststructures")) }
tasks.matching { it.name.startsWith("prepareRunGameTestServer") }.configureEach { dependsOn(syncGameTestStructures) }
tasks.matching { it.name == "runGameTestServer" }.configureEach {
    doFirst {
        val actual = file(minecraft.runs.getByName("gameTestServer").workingDirectory)
        require(actual.canonicalFile == gameTestAttempt.canonicalFile) { "Native GameTest working-directory drift: $actual" }
        require(!Files.exists(gameTestAttempt.resolve("world").toPath(), LinkOption.NOFOLLOW_LINKS)) { "Refusing existing GameTest world: $gameTestAttempt" }
        logger.lifecycle("Native GameTest launching fresh world: ${gameTestAttempt.resolve("world").absolutePath}; structures=${gameTestAttempt.resolve("gameteststructures").absolutePath}")
    }
}
apply(from = "gradle/native-gametest-fixture.gradle")
apply(from = "gradle/native-provider-closure.gradle")
apply(from = "gradle/native-canonical-data.gradle")
