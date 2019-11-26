plugins {
    id("java")
    id("io.freefair.lombok") version "4.1.1"
    id("com.github.johnrengelman.shadow") version "5.1.0"
	id("org.jetbrains.kotlin.jvm") version "1.3.50"
}

apply(plugin = "java")

group = "com.waridley.ttv"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://oss.jfrog.org/artifactory/libs-release")
    maven(url = "https://jitpack.io")
}

dependencies {
    testCompile(group = "junit", name = "junit", version = "4.12")

    implementation("com.github.Waridley:chatgame:0d9f90277d")

    //mongodb java driver
    compile("org.mongodb:mongo-java-driver:3.11.0")

    //Twitch API library
    compile(group = "com.github.twitch4j", name = "twitch4j", version = "1.0.0-alpha.17")

    //lombok
    compileOnly("org.projectlombok:lombok:1.18.10")
    annotationProcessor("org.projectlombok:lombok:1.18.10")

    //logger
    compile(group = "org.slf4j", name = "slf4j-simple", version = "1.7.2")
	
	//kotlin
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	
	//CLI parser
	implementation("com.github.ajalt:clikt:2.3.0")
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = "com.waridley.ttv.logger.LauncherKt"
    }
}
tasks.compileKotlin {
	kotlinOptions {
		jvmTarget = "1.8"
	}
}
tasks.compileTestKotlin {
	kotlinOptions {
		jvmTarget = "1.8"
	}
}