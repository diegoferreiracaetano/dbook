plugins {
    id("dbook.library-conventions")
}

dependencies {
    api(project(":booking"))
    api(project(":catalog"))
    api(project(":core"))
}
