plugins {
    id("rpgroll.plugin-conventions")
}

dependencies {
    // RPGContent vive en RPGRoll-Lib, que en runtime es un plugin aparte: no se
    // arrastra dentro del jar de quien empaquete :api (el core).
    compileOnly(project(":common"))
    testImplementation(project(":common"))
}
