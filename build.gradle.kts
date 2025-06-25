plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "tf.bug"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

repositories {
    mavenCentral()
    maven {
        name = "Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.3.0-SNAPSHOT")

    implementation("org.slf4j:slf4j-simple:2.0.17")

    implementation("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")
    implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")

    implementation("net.time4j:time4j-base:5.9.4")
    implementation("net.time4j:time4j-sqlxml:5.9.4")
    implementation("net.time4j:time4j-tzdata:5.0-2025a")

    implementation("net.iakovlev:timeshape:2025b.26")

    implementation("org.apache.commons:commons-text:1.13.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            imageName.set("chalkbot")
            mainClass.set("tf.bug.chalkbot.ChalkBot")

            buildArgs.add("--initialize-at-build-time=org.slf4j.LoggerFactory,org.slf4j.helpers.SubstituteLoggerFactory,org.slf4j.helpers.SubstituteServiceProvider,org.slf4j.helpers.NOPLoggerFactory,org.slf4j.helpers.NOP_FallbackServiceProvider,org.slf4j.simple")
        }
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "tf.bug.chalkbot.ChalkBot"
    }
}

tasks.test {
    useJUnitPlatform()
}
