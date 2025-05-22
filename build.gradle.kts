import org.jreleaser.model.Active
import org.jreleaser.model.Signing

plugins {
    groovy
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
    signing
    id("org.jreleaser") version "1.18.0"
}

group = "io.github.mictaege"
version = "2025.3-rc1"

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
    implementation("com.google.guava:guava:33.4.8-jre")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.4")
    api("fr.inria.gforge.spoon:spoon-core:11.2.0"){
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
            url = uri(layout.buildDirectory.dir("staging-deploy").get().asFile.toURI())
        }
    }
}

jreleaser {
    project {
        copyright.set("Michael Taege")
        description.set("The spoon-gradle-plugin is a gradle plugin for the Java analysis and transformation framework Spoon.")
    }
    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
        checksums.set(true)
        mode.set(Signing.Mode.FILE)
        passphrase.set(if (hasProperty("centralPortalKeyPwd")) property("centralPortalKeyPwd") as String else "")
        publicKey.set(if (hasProperty("centralPortalPublicKey")) property("centralPortalPublicKey") as String else "")
        secretKey.set(if (hasProperty("centralPortalSecretKey")) property("centralPortalSecretKey") as String else "")
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(Active.ALWAYS)
                    url = "https://central.sonatype.com/api/v1/publisher"
                    username.set(if (hasProperty("centralPortalUsr")) property("centralPortalUsr") as String else "")
                    password.set(if (hasProperty("centralPortalPwd")) property("centralPortalPwd") as String else "")
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.path)
                }
            }
        }
    }
    release {
        github {
            enabled.set(false)
        }
    }
}