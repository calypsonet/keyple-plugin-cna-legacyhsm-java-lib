///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    java
    id("com.diffplug.spotless") version "5.10.2"
}
buildscript {
    repositories {
        mavenLocal()
        maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
        mavenCentral()
    }
    dependencies {
        classpath("org.eclipse.keyple:keyple-gradle:0.2.+") { isChanging = true }
    }
}
apply(plugin = "org.eclipse.keyple")

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
repositories {
    mavenLocal()
    maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
}
dependencies {
    // Add compiled HSM plugin.
    implementation(fileTree(mapOf("dir" to "../build/libs", "include" to listOf("*.jar"))))

    implementation("org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.2.0")
    implementation("org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.8.0")
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.0")
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.3.0")
    implementation("org.eclipse.keyple:keyple-service-java-lib:2.2.1")
    implementation("org.eclipse.keyple:keyple-service-resource-java-lib:2.1.1")
    implementation("org.eclipse.keyple:keyple-card-calypso-java-lib:2.3.5")
    implementation("org.eclipse.keyple:keyple-plugin-pcsc-java-lib:2.1.2")
    implementation ("org.slf4j:slf4j-simple:1.7.32")
}

val javaSourceLevel: String by project
val javaTargetLevel: String by project
java {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
    println("Compiling Java $sourceCompatibility to Java $targetCompatibility.")
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
tasks {
    spotless {
        java {
            target("src/**/*.java")
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
            importOrder("java", "javax", "org", "com", "")
            removeUnusedImports()
            googleJavaFormat()
        }
    }
}
