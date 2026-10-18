// обща конфигурация за всички модули
subprojects {
    apply(plugin = "java")

    group = "bg.tues"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}
