repositories {
    jcenter()
}

plugins {
    application
    kotlin("jvm") version "1.2.31"
    id ("com.github.johnrengelman.shadow") version "2.0.4"
}

tasks {
    "stage" {
        dependsOn("clean", "shadowJar")
        mustRunAfter("build")
    }
}

application {
    mainClassName = "no.sparefant.MainKt"
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))
    compile("io.javalin:javalin:1.6.1")
    compile("com.github.kittinunf.fuel:fuel:1.13.0")
    compile("com.github.kittinunf.fuel:fuel-moshi:1.13.0")
    compile ("io.github.microutils:kotlin-logging:1.5.4")
    compile ("ch.qos.logback:logback-classic:1.3.0-alpha4")
}


