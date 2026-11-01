plugins {
    application
}

dependencies {
    implementation(project(":geometry"))
}

application {
    mainClass.set("bg.tues.app.Main")
}
