plugins {
    id("dbook.library-conventions")
}

dependencies {
    api(project(":identity"))
    api(project(":audit"))
    api(project(":favorite"))
    api(project(":core"))
}
