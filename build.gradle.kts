plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.1"
}

group = "io.github.fun.stuff.reflection"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")

}
