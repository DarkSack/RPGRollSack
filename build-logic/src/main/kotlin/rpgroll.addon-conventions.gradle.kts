plugins {
    id("rpgroll.plugin-conventions")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":common"))
}