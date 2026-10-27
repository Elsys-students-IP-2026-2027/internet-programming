# Maven — multi-module пример

Модули:
- `geometry` — библиотека (`Shape`, `Circle`, `Rectangle`)
- `app` — приложение с `Main`, зависи от `geometry`

Изисква JDK 21 и Maven 3.9+.

## Как се пуска
```bash
cd example-maven
mvn package               # компилира и пакетира двата модула
java -cp app/target/classes:geometry/target/classes bg.tues.app.Main
mvn clean                 # трие target/ папките
```
Windows: в `-cp` разделителят е `;` вместо `:`.

## Файлове
- `pom.xml` — parent (`packaging pom`), `<modules>` и `dependencyManagement`
- `geometry/pom.xml` — библиотека
- `app/pom.xml` — зависимост към `geometry`
