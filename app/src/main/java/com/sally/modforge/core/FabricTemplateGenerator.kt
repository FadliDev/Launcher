package com.sally.modforge.core

import com.sally.modforge.data.ModProject
import com.sally.modforge.data.VersionMatrix
import java.io.File

object FabricTemplateGenerator {
    fun generate(project: ModProject, root: File) {
        require(project.minecraftVersion != "26.3" || project.template == "Empty") {
            "Untuk Minecraft 26.3, template yang tersedia saat ini hanya Empty karena perubahan mapping/API masih perlu divalidasi."
        }
        val profile = VersionMatrix.profile(project.minecraftVersion)
        val packagePath = project.packageName.replace('.', '/')
        val baseClassName = com.sally.modforge.data.javaClassName(project.name)
        val className = if (baseClassName.endsWith("Mod")) baseClassName else "${baseClassName}Mod"

        val files = linkedMapOf<String, String>()
        files["settings.gradle"] = """
            pluginManagement {
                repositories {
                    maven { url = 'https://maven.fabricmc.net/' }
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            rootProject.name = '${project.modId}'
        """.trimIndent() + "\n"

        files["build.gradle"] = """
            plugins {
                id 'fabric-loom' version '${profile.loom}'
                id 'maven-publish'
            }

            version = project.mod_version
            group = project.maven_group
            base { archivesName = project.archives_base_name }

            repositories { mavenCentral() }

            dependencies {
                minecraft "com.mojang:minecraft:${'$'}{project.minecraft_version}"
                if (project.mappings_version == 'official') {
                    mappings loom.officialMojangMappings()
                } else {
                    mappings "net.fabricmc:yarn:${'$'}{project.mappings_version}:v2"
                }
                modImplementation "net.fabricmc:fabric-loader:${'$'}{project.loader_version}"
                modImplementation "net.fabricmc.fabric-api:fabric-api:${'$'}{project.fabric_api_version}"
            }

            processResources {
                inputs.property 'version', project.version
                filesMatching('fabric.mod.json') { expand 'version': project.version }
            }

            tasks.withType(JavaCompile).configureEach {
                options.release = Integer.parseInt(project.java_version)
            }

            java {
                toolchain { languageVersion = JavaLanguageVersion.of(Integer.parseInt(project.java_version)) }
                withSourcesJar()
            }
        """.trimIndent() + "\n"

        files["gradle.properties"] = """
            org.gradle.jvmargs=-Xmx2G
            org.gradle.parallel=true
            org.gradle.caching=true
            minecraft_version=${project.minecraftVersion}
            mappings_version=${profile.yarn}
            loader_version=${profile.loader}
            fabric_api_version=${profile.fabricApi}
            mod_version=1.0.0
            maven_group=${project.packageName.substringBeforeLast('.', "com.sally")}
            archives_base_name=${project.modId}
            java_version=${project.javaVersion}
        """.trimIndent() + "\n"

        files[".gitignore"] = """
            .gradle/
            build/
            run/
            .idea/
            *.iml
            .modforge/
        """.trimIndent() + "\n"

        files[".github/workflows/build.yml"] = """
            name: Build Fabric Mod
            on:
              push:
                branches: [main]
              workflow_dispatch:
            permissions:
              contents: read
            concurrency:
              group: mod-build-${'$'}{{ github.ref }}
              cancel-in-progress: true
            jobs:
              build:
                runs-on: ubuntu-latest
                steps:
                  - uses: actions/checkout@v6
                  - uses: actions/setup-java@v5
                    with:
                      distribution: temurin
                      java-version: '${project.javaVersion}'
                  - uses: gradle/actions/setup-gradle@v6
                    with:
                      gradle-version: '${project.gradleVersion}'
                      cache-provider: basic
                  - name: Compile mod
                    run: gradle build --no-daemon --stacktrace
                  - name: Upload mod JAR
                    uses: actions/upload-artifact@v4
                    with:
                      name: ${project.modId}-jar
                      path: |
                        build/libs/*.jar
                        !build/libs/*-sources.jar
                        !build/libs/*-dev.jar
                      if-no-files-found: error
                      retention-days: 7
        """.trimIndent() + "\n"

        files["src/main/resources/fabric.mod.json"] = """
            {
              "schemaVersion": 1,
              "id": "${project.modId}",
              "version": "${'$'}{version}",
              "name": "${jsonEscape(project.name)}",
              "description": "${jsonEscape(project.description)}",
              "authors": ["${jsonEscape(project.author)}"],
              "license": "MIT",
              "environment": "*",
              "entrypoints": {
                "main": ["${project.packageName}.${className}"]
              },
              "depends": {
                "fabricloader": ">=${profile.loader}",
                "minecraft": "${if (project.minecraftVersion == "26.3") project.minecraftVersion else "~${project.minecraftVersion}"}",
                "java": ">=${project.javaVersion}",
                "fabric-api": "*"
              }
            }
        """.trimIndent() + "\n"

        if (project.template == "Item" || project.template == "Item + Block") {
            files["src/main/resources/assets/${project.modId}/models/item/ruby.json"] = """
                {"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/emerald"}}
            """.trimIndent() + "\n"
        }
        if (project.template == "Block" || project.template == "Item + Block") {
            files["src/main/resources/assets/${project.modId}/blockstates/ruby_block.json"] = """
                {"variants":{"":{"model":"${project.modId}:block/ruby_block"}}}
            """.trimIndent() + "\n"
            files["src/main/resources/assets/${project.modId}/models/block/ruby_block.json"] = """
                {"parent":"minecraft:block/cube_all","textures":{"all":"minecraft:block/iron_block"}}
            """.trimIndent() + "\n"
            files["src/main/resources/assets/${project.modId}/models/item/ruby_block.json"] = """
                {"parent":"${project.modId}:block/ruby_block"}
            """.trimIndent() + "\n"
        }
        if (project.template == "Item" || project.template == "Block" || project.template == "Item + Block") {
            val translations = linkedMapOf<String, String>()
            if (project.template == "Item" || project.template == "Item + Block") translations["item.${project.modId}.ruby"] = "Ruby"
            if (project.template == "Block" || project.template == "Item + Block") {
                translations["block.${project.modId}.ruby_block"] = "Ruby Block"
                translations["item.${project.modId}.ruby_block"] = "Ruby Block"
            }
            files["src/main/resources/assets/${project.modId}/lang/en_us.json"] =
                "{\n" + translations.entries.joinToString(",\n") { (key, value) -> "  \"$key\": \"$value\"" } + "\n}\n"
        }

        files["src/main/java/$packagePath/$className.java"] = buildMainClass(project, className)
        files["README.md"] = """
            # ${project.name}

            A Fabric mod for Minecraft ${project.minecraftVersion}, generated with ModForge.

            ## Build

            Push this repository to GitHub. The **Build Fabric Mod** workflow compiles the project and uploads the release JAR as an Actions artifact.

            - Minecraft: ${project.minecraftVersion}
            - Java: ${project.javaVersion}
            - Gradle: ${project.gradleVersion}
            - Template: ${project.template}

            Generated files can be edited in ModForge or in another IDE. Do not commit access tokens or secrets.
        """.trimIndent() + "\n"

        files.forEach { (path, text) ->
            val target = File(root, path)
            target.parentFile?.mkdirs()
            target.writeText(text)
        }
        File(root, ".modforge").mkdirs()
        File(root, ".modforge/generated-template.txt").writeText("template=${project.template}\nminecraft=${project.minecraftVersion}\nexperimental=${profile.experimental}\n")
    }

    private fun buildMainClass(project: ModProject, className: String): String {
        if (project.minecraftVersion == "26.3") {
            return """package ${project.packageName};

                import net.fabricmc.api.ModInitializer;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;

                public class $className implements ModInitializer {
                    public static final String MOD_ID = "${project.modId}";
                    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

                    @Override
                    public void onInitialize() {
                        LOGGER.info("Loaded {} for Minecraft ${project.minecraftVersion}", MOD_ID);
                    }
                }
            """.trimIndent() + "\n"
        }
        val olderRegistry = project.minecraftVersion == "1.16.5" || project.minecraftVersion == "1.18.2"
        val registryClass = if (olderRegistry) "net.minecraft.util.registry.Registry" else "net.minecraft.registry.Registry"
        val itemRegistry = if (olderRegistry) "$registryClass.ITEM" else "net.minecraft.registry.Registries.ITEM"
        val blockRegistry = if (olderRegistry) "$registryClass.BLOCK" else "net.minecraft.registry.Registries.BLOCK"
        val oldItemSettings = if (olderRegistry) "new net.minecraft.item.Item.Settings().group(net.minecraft.item.ItemGroup.MISC)" else "new net.minecraft.item.Item.Settings()"
        val oldBlockItemSettings = if (olderRegistry) "new net.minecraft.item.Item.Settings().group(net.minecraft.item.ItemGroup.BUILDING_BLOCKS)" else "new net.minecraft.item.Item.Settings()"
        val itemCode = when (project.template) {
            "Item", "Item + Block" -> """
                public static final net.minecraft.item.Item RUBY = $registryClass.register(
                    $itemRegistry, identifier("ruby"), new net.minecraft.item.Item($oldItemSettings));
            """.trimIndent()
            else -> ""
        }
        val blockCode = when (project.template) {
            "Block", "Item + Block" -> """
                public static final net.minecraft.block.Block RUBY_BLOCK = $registryClass.register(
                    $blockRegistry, identifier("ruby_block"),
                    new net.minecraft.block.Block(net.minecraft.block.AbstractBlock.Settings.copy(net.minecraft.block.Blocks.IRON_BLOCK)));
                public static final net.minecraft.item.BlockItem RUBY_BLOCK_ITEM = $registryClass.register(
                    $itemRegistry, identifier("ruby_block"), new net.minecraft.item.BlockItem(RUBY_BLOCK, $oldBlockItemSettings));
            """.trimIndent()
            else -> ""
        }
        val groupEvent = if (!olderRegistry && (project.template == "Item" || project.template == "Block" || project.template == "Item + Block")) {
            val addRuby = if (project.template == "Item" || project.template == "Item + Block") "entries.add(RUBY);" else ""
            val addBlock = if (project.template == "Block" || project.template == "Item + Block") "entries.add(RUBY_BLOCK_ITEM);" else ""
            """
                net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.item.ItemGroups.INGREDIENTS)
                    .register(entries -> { $addRuby });
                net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.item.ItemGroups.BUILDING_BLOCKS)
                    .register(entries -> { $addBlock });
            """.trimIndent()
        } else ""
        val eventCode = if (project.template == "Event") """
            net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) ->
                net.minecraft.util.TypedActionResult.pass(player.getStackInHand(hand)));
        """.trimIndent() else ""
        val identifierExpression = if (project.minecraftVersion in listOf("1.16.5", "1.18.2", "1.20.1")) {
            "new Identifier(MOD_ID, path)"
        } else {
            "Identifier.of(MOD_ID, path)"
        }
        val imports = if (olderRegistry) {
            "import net.minecraft.util.registry.Registry;"
        } else {
            "import net.minecraft.registry.Registry;\nimport net.minecraft.registry.Registries;"
        }
        return """package ${project.packageName};

            import net.fabricmc.api.ModInitializer;
            import net.minecraft.util.Identifier;
            $imports
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;

            public class $className implements ModInitializer {
                public static final String MOD_ID = "${project.modId}";
                private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

            ${itemCode.prependIndent("    ")}
            ${blockCode.prependIndent("    ")}
                @Override
                public void onInitialize() {
                    LOGGER.info("Loaded {} for Minecraft ${project.minecraftVersion}", MOD_ID);
            ${groupEvent.prependIndent("        ")}
            ${eventCode.prependIndent("        ")}
                }

                private static Identifier identifier(String path) {
                    return $identifierExpression;
                }
            }
        """.trimIndent().replace("\n\n\n", "\n\n") + "\n"
    }

    private fun jsonEscape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
}
