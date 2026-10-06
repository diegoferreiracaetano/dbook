plugins {
    id("dbook.library-conventions")
}

dependencies {
    api(project(":booking"))
    api(project(":accommodation"))
    api(project(":flight"))
    api(project(":core"))
}
