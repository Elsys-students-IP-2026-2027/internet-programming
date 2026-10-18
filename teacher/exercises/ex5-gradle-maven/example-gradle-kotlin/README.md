# Gradle (Kotlin DSL) — multi-project пример

Модули:
- `geometry` — библиотека (`Shape`, `Circle`, `Rectangle`)
- `app` — приложение с `Main`, зависи от `geometry`

Изисква JDK 21. Wrapper-ът е включен, не е нужен инсталиран Gradle.

## Как се пуска
```bash
cd example-gradle-kotlin
./gradlew :app:run        # стартира приложението
./gradlew build           # компилира всички модули
./gradlew clean           # трие build/ папките
./gradlew projects        # показва модулите
```
Windows: `gradlew.bat :app:run`.

## Файлове
- `settings.gradle.kts` — `include("geometry", "app")`
- `build.gradle.kts` — обща конфигурация в `subprojects { }`
- `app/build.gradle.kts` — `implementation(project(":geometry"))` и `mainClass`
