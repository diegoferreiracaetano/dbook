pluginManagement {
    // the convention plugins every module applies (see build-logic/)
    includeBuild("build-logic")
}

rootProject.name = "dbook"
include("core")
include("audit")
include("identity")
include("catalog")
include("booking")
include("flight")
include("accommodation")
include("pricing")
include("favorite")
include("ai")
include("payment")
include("review")
include("trips")
include("notification")
include("admin")
