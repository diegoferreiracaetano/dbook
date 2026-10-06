plugins {
    id("dbook.library-conventions")
}

dependencies {
    api(project(":booking"))
    api(project(":flight"))
    api(project(":payment"))
    api(project(":pricing"))
    api(project(":identity"))
    api(project(":core"))
}
