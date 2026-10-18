# Gradle (Groovy DSL) — multi-project пример

Модули:
- `geometry` — библиотека (`Shape`, `Circle`, `Rectangle`)
- `app` — приложение с `Main`, зависи от `geometry`

Изисква JDK 21. Wrapper-ът е включен, не е нужен инсталиран Gradle.

## Как се пуска
```bash
cd example-gradle-groovy
./gradlew :app:run        # стартира приложението
./gradlew build           # компилира всички модули
./gradlew clean           # трие build/ папките
./gradlew projects        # показва модулите
```
Windows: `gradlew.bat :app:run`.

## Файлове
- `settings.gradle` — `include 'geometry', 'app'`
- `build.gradle` — обща конфигурация в `subprojects { }`
- `app/build.gradle` — `implementation project(':geometry')` и `mainClass`
