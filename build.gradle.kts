plugins { java }
group = "com.fizzysmp"
version = "1.0.2"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
val rulesCheck by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    doFirst { file("test-output").mkdirs() }
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.fizzysmp.hiddenmessages.RulesTest")
}
tasks.test { enabled = false }
tasks.check { dependsOn(rulesCheck) }
