plugins {
    groovy
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
    signing
}

group = "io.github.mictaege"
version = "2024.1"

gradlePlugin {
    website.set("https://github.com/mictaege/spoon-gradle-plugin")
    vcsUrl.set("https://github.com/mictaege/spoon-gradle-plugin.git")
    plugins {
        create("spoonPlugin") {
            id = "io.github.mictaege.spoon-gradle-plugin"
            displayName = "Spoon Gradle Plugin"
            description = "The spoon-gradle-plugin is a gradle plugin for the Java analysis and transformation framework Spoon."
            implementationClass = "com.github.mictaege.spoon_gradle_plugin.SpoonPlugin"
            tags.set(listOf("spoon"))
        }
    }
}

tasks.wrapper {
    gradleVersion = "8.7"
    distributionType = Wrapper.DistributionType.ALL
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    //implementation("org.codehaus.groovy:groovy-all:3.0.17")
    implementation("com.google.guava:guava:33.2.0-jre")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.10")
    api("fr.inria.gforge.spoon:spoon-core:11.0.0"){
        exclude(group = "org.eclipse.jdt", module = "org.eclipse.jdt.core")
    }
}

tasks.register("generateResources") {
    val propFile = file("$buildDir/generated/spoon-gradle-plugin.properties")
    outputs.file(propFile)
    doLast {
        mkdir(propFile.parentFile)
        propFile.writeText("version=${project.version}")
    }
}

tasks.processResources {
    from(files(tasks.getByName("generateResources")))
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("spoon-gradle-plugin")
                description.set("The spoon-gradle-plugin is a gradle plugin for the Java analysis and transformation framework Spoon.")
                url.set("https://github.com/mictaege/spoon-gradle-plugin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("mictaege")
                        name.set("Michael Taege")
                        email.set("mictaege@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/mictaege/spoon-gradle-plugin.git")
                    url.set("https://github.com/mictaege/spoon-gradle-plugin")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = if (hasProperty("ossrhUsername")) property("ossrhUsername") as String else ""
                password = if (hasProperty("ossrhPassword")) property("ossrhPassword") as String else ""
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}