package com.sally.modforge.data

import java.io.File

object ModTemplateGenerator {
    fun generate(root: File, project: ModProject) {
        root.mkdirs()
        write(root, "README.md", readme(project))
        write(root, ".gitignore", ".gradle/\nbuild/\nrun/\n.idea/\n*.iml\nlocal.properties\n")
        when (project.loader) {
            ModLoader.FABRIC -> generateFabric(root, project)
            ModLoader.FORGE -> generateForge(root, project)
            ModLoader.NEOFORGE -> generateNeoForge(root, project)
        }
        write(root, "LICENSE.txt", "${project.license} license selected for ${project.name}. Replace this placeholder with the full license text before publishing.\n")
    }

    private fun generateFabric(root: File, p: ModProject) {
        val java = javaFolder(root, p.packageName)
        val className = mainClass(p.modId)
        val isExampleVersion = p.minecraftVersion == "1.21.1"
        write(root, "settings.gradle", """
            pluginManagement {
                repositories {
                    maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            rootProject.name = '${escapeGradle(p.modId)}'
        """.trimIndent() + "\n")
        write(root, "gradle.properties", """
            org.gradle.jvmargs=-Xmx2G
            org.gradle.parallel=true
            minecraft_version=${p.minecraftVersion}
            yarn_mappings=${if (isExampleVersion) "1.21.1+build.3" else "REPLACE_WITH_VALID_MAPPINGS"}
            loader_version=0.16.10
            fabric_api_version=${if (isExampleVersion) "0.116.1+1.21.1" else "REPLACE_WITH_FABRIC_API_FOR_VERSION"}
            mod_version=${p.modVersion}
            maven_group=${p.packageName.substringBeforeLast('.', "com.example")}
            archives_base_name=${p.modId}
        """.trimIndent() + "\n")
        write(root, "build.gradle", """
            plugins {
                id 'net.fabricmc.fabric-loom-remap' version '1.11-SNAPSHOT'
                id 'maven-publish'
            }

            version = project.mod_version
            group = project.maven_group
            base { archivesName = project.archives_base_name }

            repositories { mavenCentral() }

            dependencies {
                minecraft "com.mojang:minecraft:${'$'}{project.minecraft_version}"
                mappings "net.fabricmc:yarn:${'$'}{project.yarn_mappings}:v2"
                modImplementation "net.fabricmc:fabric-loader:${'$'}{project.loader_version}"
                modImplementation "net.fabricmc.fabric-api:fabric-api:${'$'}{project.fabric_api_version}"
            }

            processResources {
                inputs.property 'version', project.version
                filesMatching('fabric.mod.json') { expand 'version': project.version }
            }

            java { toolchain { languageVersion = JavaLanguageVersion.of(${javaVersion(p.minecraftVersion)}) } }
            tasks.withType(JavaCompile).configureEach { options.release = ${javaVersion(p.minecraftVersion)} }
        """.trimIndent() + "\n")
        write(root, "src/main/resources/fabric.mod.json", """
            {
              "schemaVersion": 1,
              "id": "${p.modId}",
              "version": "${'$'}{version}",
              "name": "${jsonEscape(p.name)}",
              "description": "${jsonEscape(p.description)}",
              "authors": ["${jsonEscape(p.author.ifBlank { "ModForge User" })}"],
              "license": "${jsonEscape(p.license)}",
              "environment": "*",
              "entrypoints": { "main": ["${p.packageName}.$className"] },
              "depends": {
                "fabricloader": ">=0.16.0",
                "minecraft": "${p.minecraftVersion}",
                "java": ">=${javaVersion(p.minecraftVersion)}"
              }
            }
        """.trimIndent() + "\n")
        val mainSource = when (p.template) {
            ModTemplate.ITEM -> fabricItemSource(p, className)
            ModTemplate.BLOCK -> fabricBlockSource(p, className)
            ModTemplate.COMMAND -> fabricCommandSource(p, className)
            ModTemplate.EMPTY -> fabricEmptySource(p, className)
        }
        write(java, "$className.java", mainSource)
        when (p.template) {
            ModTemplate.ITEM -> generateFabricItemAssets(root, p)
            ModTemplate.BLOCK -> generateFabricBlockAssets(root, p)
            else -> write(root, "src/main/resources/assets/${p.modId}/lang/en_us.json", "{}\n")
        }
        val workflowJava = javaVersion(p.minecraftVersion)
        val gradleVersion = if (workflowJava <= 17) "8.12" else "8.14.3"
        write(root, ".github/workflows/build.yml", cloudBuildWorkflow(workflowJava, gradleVersion, "fabric"))
        if (!isExampleVersion) {
            write(root, "VERSION_PROFILE_WARNING.md", """
                # Version profile needs review

                ModForge generated the project metadata for Minecraft ${p.minecraftVersion}, but the built-in sample dependency versions are only pinned for Fabric 1.21.1. Replace `yarn_mappings` and `fabric_api_version` in `gradle.properties` with versions listed by the official Fabric develop page before building.

                The project stays editable; ModForge does not silently guess version-specific coordinates.
            """.trimIndent() + "\n")
        }
    }

    private fun generateForge(root: File, p: ModProject) {
        val java = javaFolder(root, p.packageName)
        val className = mainClass(p.modId)
        val javaVersion = javaVersion(p.minecraftVersion)
        write(root, "settings.gradle", """
            pluginManagement { repositories { maven { url = 'https://maven.minecraftforge.net/' }; gradlePluginPortal(); mavenCentral() } }
            rootProject.name = '${escapeGradle(p.modId)}'
        """.trimIndent() + "\n")
        write(root, "gradle.properties", """
            org.gradle.jvmargs=-Xmx3G
            org.gradle.daemon=false
            minecraft_version=${p.minecraftVersion}
            forge_version=${if (p.minecraftVersion == "1.20.1") "47.3.0" else "REPLACE_WITH_FORGE_VERSION_FOR_MINECRAFT"}
            mod_id=${p.modId}
            mod_name=${p.name.replace("\n", " ")}
            mod_version=${p.modVersion}
            mod_group_id=${p.packageName.substringBeforeLast('.', "com.example")}
            mod_authors=${p.author.ifBlank { "ModForge User" }}
            mod_description=${p.description.replace("\n", " ")}
        """.trimIndent() + "\n")
        write(root, "build.gradle", """
            plugins { id 'net.minecraftforge.gradle' version '[6.0,6.2)' }
            group = mod_group_id
            version = mod_version
            base { archivesName = mod_id }

            java.toolchain.languageVersion = JavaLanguageVersion.of($javaVersion)

            minecraft {
                mappings channel: 'official', version: minecraft_version
                copyIdeResources = true
                runs {
                    configureEach { workingDirectory project.file('run'); property 'forge.logging.console.level', 'info'; mods { "${'$'}{mod_id}" { source sourceSets.main } } }
                    client {}
                    server { args '--nogui' }
            }
            }

            dependencies { minecraft "net.minecraftforge:forge:${'$'}{minecraft_version}-${'$'}{forge_version}" }
            tasks.named('processResources', ProcessResources).configure {
                var replaceProperties = [minecraft_version: minecraft_version, forge_version: forge_version, mod_id: mod_id, mod_name: mod_name, mod_version: mod_version, mod_authors: mod_authors, mod_description: mod_description]
                inputs.properties replaceProperties
                filesMatching(['META-INF/mods.toml']) { expand replaceProperties }
            }
            tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
        """.trimIndent() + "\n")
        write(root, "src/main/resources/META-INF/mods.toml", """
            modLoader="javafml"
            loaderVersion="[${if (p.minecraftVersion == "1.20.1") "47," else "1,"})"
            license="${p.license}"
            [[mods]]
            modId="${'$'}{mod_id}"
            version="${'$'}{mod_version}"
            displayName="${'$'}{mod_name}"
            authors="${'$'}{mod_authors}"
            description='''${'$'}{mod_description}'''
            [[dependencies.${p.modId}]]
            modId="forge"
            mandatory=true
            versionRange="[${'$'}{forge_version},)"
            ordering="NONE"
            side="BOTH"
            [[dependencies.${p.modId}]]
            modId="minecraft"
            mandatory=true
            versionRange="[${'$'}{minecraft_version},${minecraftUpperBound(p.minecraftVersion)})"
            ordering="NONE"
            side="BOTH"
        """.trimIndent() + "\n")
        write(java, "$className.java", """
            package ${p.packageName};

            import net.minecraftforge.fml.common.Mod;

            @Mod(${className}.MOD_ID)
            public final class $className {
                public static final String MOD_ID = "${p.modId}";

                public $className() {
                    // TODO: register items, blocks, events and commands here.
                }
            }
        """.trimIndent() + "\n")
        val gradleVersion = if (javaVersion <= 17) "8.8" else "8.10.2"
        write(root, ".github/workflows/build.yml", cloudBuildWorkflow(javaVersion, gradleVersion, "forge"))
        if (p.minecraftVersion != "1.20.1") write(root, "VERSION_PROFILE_WARNING.md", "Built-in Forge profile is pinned for 1.20.1 / Forge 47.3.0. Update `forge_version` to a matching Forge build for ${p.minecraftVersion} before building.\n")
    }

    private fun generateNeoForge(root: File, p: ModProject) {
        val java = javaFolder(root, p.packageName)
        val className = mainClass(p.modId)
        val javaVersion = javaVersion(p.minecraftVersion)
        val neoForgeVersion = if (p.minecraftVersion == "1.21.1") "21.1.219" else "REPLACE_WITH_NEOFORGE_VERSION_FOR_MINECRAFT"
        write(root, "settings.gradle", """
            pluginManagement { repositories { maven { url = 'https://maven.neoforged.net/releases' }; gradlePluginPortal(); mavenCentral() } }
            rootProject.name = '${escapeGradle(p.modId)}'
        """.trimIndent() + "\n")
        write(root, "gradle.properties", """
            org.gradle.jvmargs=-Xmx3G
            org.gradle.daemon=false
            minecraft_version=${p.minecraftVersion}
            neoforge_version=$neoForgeVersion
            mod_id=${p.modId}
            mod_name=${p.name.replace("\n", " ")}
            mod_version=${p.modVersion}
            mod_group_id=${p.packageName.substringBeforeLast('.', "com.example")}
            mod_authors=${p.author.ifBlank { "ModForge User" }}
            mod_description=${p.description.replace("\n", " ")}
        """.trimIndent() + "\n")
        write(root, "build.gradle", """
            plugins { id 'net.neoforged.moddev' version '2.0.147' }
            version = mod_version
            group = mod_group_id
            base { archivesName = mod_id }
            java.toolchain.languageVersion = JavaLanguageVersion.of($javaVersion)
            neoForge {
                version = neoforge_version
                runs { client { client() }; server { server(); programArgument '--nogui' } }
                mods { "${'$'}{mod_id}" { sourceSet sourceSets.main } }
            }
            tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
        """.trimIndent() + "\n")
        write(java, "$className.java", """
            package ${p.packageName};

            import net.neoforged.fml.common.Mod;

            @Mod(${className}.MOD_ID)
            public final class $className {
                public static final String MOD_ID = "${p.modId}";

                public $className() {
                    // TODO: register content and subscribe to events here.
                }
            }
        """.trimIndent() + "\n")
        write(root, "src/main/resources/META-INF/neoforge.mods.toml", """
            modLoader="javafml"
            loaderVersion="[4,)"
            license="${p.license}"
            [[mods]]
            modId="${p.modId}"
            version="${p.modVersion}"
            displayName="${p.name}"
            authors="${p.author.ifBlank { "ModForge User" }}"
            description='''${p.description}'''
        """.trimIndent() + "\n")
        write(root, ".github/workflows/build.yml", cloudBuildWorkflow(javaVersion, "8.12", "neoforge"))
        if (p.minecraftVersion != "1.21.1") write(root, "VERSION_PROFILE_WARNING.md", "Built-in NeoForge profile is pinned for Minecraft 1.21.1. Update `neoforge_version` to a matching NeoForge release for ${p.minecraftVersion} before building.\n")
    }

    private fun fabricEmptySource(p: ModProject, className: String) = """
        package ${p.packageName};

        import net.fabricmc.api.ModInitializer;

        public final class $className implements ModInitializer {
            public static final String MOD_ID = "${p.modId}";

            @Override
            public void onInitialize() {
                System.out.println("[" + MOD_ID + "] ${javaEscape(p.name)} initialized");
            }
        }
    """.trimIndent() + "\n"

    private fun fabricItemSource(p: ModProject, className: String) = """
        package ${p.packageName};

        import net.fabricmc.api.ModInitializer;
        import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
        import net.minecraft.item.Item;
        import net.minecraft.item.ItemGroups;
        import net.minecraft.registry.Registries;
        import net.minecraft.registry.Registry;
        import net.minecraft.util.Identifier;

        public final class $className implements ModInitializer {
            public static final String MOD_ID = "${p.modId}";
            public static final Item RUBY = new Item(new Item.Settings());

            @Override
            public void onInitialize() {
                Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "ruby"), RUBY);
                ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(RUBY));
                System.out.println("[" + MOD_ID + "] Registered item ruby");
            }
        }
    """.trimIndent() + "\n"

    private fun fabricBlockSource(p: ModProject, className: String) = """
        package ${p.packageName};

        import net.fabricmc.api.ModInitializer;
        import net.minecraft.block.AbstractBlock;
        import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
        import net.minecraft.block.Block;
        import net.minecraft.item.BlockItem;
        import net.minecraft.item.Item;
        import net.minecraft.item.ItemGroups;
        import net.minecraft.registry.Registries;
        import net.minecraft.registry.Registry;
        import net.minecraft.util.Identifier;

        public final class $className implements ModInitializer {
            public static final String MOD_ID = "${p.modId}";
            public static final Block RUBY_BLOCK = new Block(AbstractBlock.Settings.create().strength(4.0f));

            @Override
            public void onInitialize() {
                Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "ruby_block"), RUBY_BLOCK);
                Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "ruby_block"), new BlockItem(RUBY_BLOCK, new Item.Settings()));
                ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(RUBY_BLOCK));
                System.out.println("[" + MOD_ID + "] Registered block ruby_block");
            }
        }
    """.trimIndent() + "\n"

    private fun fabricCommandSource(p: ModProject, className: String) = """
        package ${p.packageName};

        import net.fabricmc.api.ModInitializer;
        import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
        import net.minecraft.text.Text;
        import static net.minecraft.server.command.CommandManager.literal;

        public final class $className implements ModInitializer {
            public static final String MOD_ID = "${p.modId}";

            @Override
            public void onInitialize() {
                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                    dispatcher.register(literal("hello").executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Hello from ${p.modId}!"), false);
                        return 1;
                    }))
                );
            }
        }
    """.trimIndent() + "\n"

    private fun generateFabricItemAssets(root: File, p: ModProject) {
        write(root, "src/main/resources/assets/${p.modId}/lang/en_us.json", "{\n  \"item.${p.modId}.ruby\": \"Ruby\"\n}\n")
        write(root, "src/main/resources/assets/${p.modId}/models/item/ruby.json", """
            {
              "parent": "minecraft:item/generated",
              "textures": { "layer0": "${p.modId}:item/ruby" }
            }
        """.trimIndent() + "\n")
        writeBytes(root, "src/main/resources/assets/${p.modId}/textures/item/ruby.png", rubyTextureBytes())
    }

    private fun generateFabricBlockAssets(root: File, p: ModProject) {
        write(root, "src/main/resources/assets/${p.modId}/lang/en_us.json", "{\n  \"block.${p.modId}.ruby_block\": \"Ruby Block\"\n}\n")
        write(root, "src/main/resources/assets/${p.modId}/blockstates/ruby_block.json", """
            {
              "variants": { "": { "model": "${p.modId}:block/ruby_block" } }
            }
        """.trimIndent() + "\n")
        write(root, "src/main/resources/assets/${p.modId}/models/block/ruby_block.json", """
            {
              "parent": "minecraft:block/cube_all",
              "textures": { "all": "${p.modId}:block/ruby_block" }
            }
        """.trimIndent() + "\n")
        write(root, "src/main/resources/assets/${p.modId}/models/item/ruby_block.json", """
            { "parent": "${p.modId}:block/ruby_block" }
        """.trimIndent() + "\n")
        writeBytes(root, "src/main/resources/assets/${p.modId}/textures/block/ruby_block.png", rubyTextureBytes())
    }

    private fun rubyTextureBytes(): ByteArray = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAyklEQVR4nGNgoDX4X9HzH588IykaGTtKMNQzoQt4c4j/x2Xr/4qe/94c4ihyjOgKGBgYGBhSAiACczbgciDcNSzIgs93HWFgYGBgkIQZgAVsXbgQ7tKtP14ywg14ZhQAd9rzsBIGSTcbnJqxegHZAGTQceMygxs/F1bXTH//CuIFXJphYNfHb1jFlZl4ILEgdW4DzujEByZ9u8sIj0ZSDZn07S4jAwNaOiDWEJhmDAOIMQRZM1YD8BmCrhmnAdgMwaaZKJDHpYw3igELnkv/zTO0+wAAAABJRU5ErkJggg=="
    )

    private fun cloudBuildWorkflow(javaVersion: Int, gradleVersion: String, loader: String) = """
        name: Build Minecraft Mod
        on:
          push:
            branches: [main, master]
          workflow_dispatch:
        permissions:
          contents: read
        jobs:
          build:
            runs-on: ubuntu-latest
            steps:
              - name: Checkout source
                uses: actions/checkout@v4
              - name: Set up Java
                uses: actions/setup-java@v4
                with:
                  distribution: temurin
                  java-version: '$javaVersion'
              - name: Set up Gradle
                uses: gradle/actions/setup-gradle@v4
                with:
                  gradle-version: '$gradleVersion'
              - name: Build mod
                run: gradle build --no-daemon
              - name: Upload mod JAR
                uses: actions/upload-artifact@v4
                with:
                  name: $loader-mod-jar
                  path: build/libs/*.jar
                  if-no-files-found: error
                  retention-days: 14
    """.trimIndent() + "\n"

    private fun javaFolder(root: File, packageName: String): File {
        val file = File(root, "src/main/java/${packageName.replace('.', '/')}")
        file.mkdirs()
        return file
    }

    private fun write(root: File, path: String, content: String) {
        val file = File(root, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    private fun writeBytes(root: File, path: String, content: ByteArray) {
        val file = File(root, path)
        file.parentFile?.mkdirs()
        file.writeBytes(content)
    }

    private fun mainClass(modId: String): String {
        val normalized = modId.removeSuffix("_mod").removeSuffix("-mod")
        val base = normalized.split('_', '-', '.').filter { it.isNotBlank() }
            .joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
        return (base.ifBlank { "Example" }) + "Mod"
    }

    private fun javaVersion(version: String): Int = when {
        version.startsWith("1.16") -> 8
        version.startsWith("1.17") -> 16
        version.startsWith("1.18") || version.startsWith("1.19") || version.startsWith("1.20.1") || version.startsWith("1.20.2") || version.startsWith("1.20.3") || version.startsWith("1.20.4") -> 17
        else -> 21
    }

    private fun minecraftUpperBound(version: String): String {
        val pieces = version.split('.')
        return if (pieces.size >= 2 && pieces[0] == "1") "1.${(pieces[1].toIntOrNull() ?: 20) + 1}"
        else version
    }

    private fun readme(p: ModProject): String = """
        # ${p.name}

        ${p.description}

        - **Mod ID:** `${p.modId}`
        - **Minecraft:** `${p.minecraftVersion}`
        - **Loader:** ${p.loader.label}
        - **Version:** `${p.modVersion}`
        - **Author:** ${p.author.ifBlank { "Not set" }}
        - **Template:** ${p.template.label}

        ## Build

        The `.github/workflows/build.yml` workflow builds the mod in GitHub Actions and uploads the JAR as an artifact. Push this project to a GitHub repository, then open **Actions** to download the artifact.

        ## Important

        Generated templates are starting points, not a guarantee for every loader/version combination. The built-in sample profile is pinned to Fabric 1.21.1, Forge 1.20.1, and NeoForge 1.21.1. Review `VERSION_PROFILE_WARNING.md` if it exists, and confirm the current official loader dependency versions before publishing.
    """.trimIndent() + "\n"

    private fun escapeGradle(value: String) = value.replace("'", "\\'").replace("\n", "")
    private fun jsonEscape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    private fun javaEscape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
}
