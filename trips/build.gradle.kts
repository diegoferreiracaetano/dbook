plugins {
    id("dbook.library-conventions")
}

dependencies {
    api(project(":booking"))
    api(project(":flight"))
    api(project(":accommodation"))
    api(project(":review"))
    api(project(":catalog"))
    api(project(":core"))
}
