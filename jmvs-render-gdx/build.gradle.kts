plugins {
    id("jmvs.java-conventions")
}

description = "libGDX renderer, camera and picking"

dependencies {
    api(project(":jmvs-scene"))
    api(libs.gdx)
}
