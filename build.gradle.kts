plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "io.moderne"
description = "Moderne SAP workshop"

tasks.getByName<JavaCompile>("compileJava") {
    // minimum required for misk use in refaster-style templates
    options.release.set(17)
}

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:$rewriteVersion"))
    implementation("org.openrewrite.recipe:rewrite-java-dependencies:$rewriteVersion")

    annotationProcessor("org.openrewrite:rewrite-templating:latest.integration")
    implementation("org.openrewrite:rewrite-templating:latest.integration")
    compileOnly("com.google.errorprone:error_prone_core:2.19.1:with-dependencies") {
        exclude("com.google.auto.service", "auto-service-annotations")
    }

    runtimeOnly("org.openrewrite:rewrite-java-17")
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-gradle")
    implementation("org.openrewrite:rewrite-maven")
    implementation("org.openrewrite:rewrite-xml")
    implementation("org.openrewrite:rewrite-properties")
    implementation("org.openrewrite:rewrite-yaml")

    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:latest.release")
}
