plugins {
    id("dbook.library-conventions")
}

dependencies {
    api(project(":booking"))
    api(project(":flight"))
    api(project(":catalog"))
    api(project(":core"))
}
