plugins {
    id("rpgroll.addon-conventions")
}

group = "com.sack"
version = "1.0.0"

base.archivesName.set("RPGRoll-Recipes")

dependencies {
    // Solo RPGRoll-Lib (rpgroll.addon-conventions ya agrega :api y :common como compileOnly).
    // Las recetas de los demás módulos (Crafting, Furniture...) llegan por RecipeSource en el
    // ServicesManager, sin compilar contra ellos: este módulo se vende suelto.
}
