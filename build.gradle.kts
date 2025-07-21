// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt.android) apply false
}

tasks.register("publishToMavenLocal") {
    dependsOn(":kamsyview:publishToMavenLocal")
}

// Add documentation tasks using correct Dokka V2 task names
tasks.register("dokkaHtml") {
    dependsOn(":kamsyview:dokkaGeneratePublicationHtml")
    description = "Generate HTML documentation for KamsyView library"
    group = "documentation"
}

tasks.register("dokkaGenerate") {
    dependsOn(":kamsyview:dokkaGenerate")
    description = "Generate all Dokka documentation for KamsyView library"
    group = "documentation"
}

tasks.register("generateDocs") {
    dependsOn("dokkaHtml")
    description = "Generate all documentation for KamsyView library"
    group = "documentation"
}

tasks.register("publishWithDocs") {
    dependsOn("generateDocs", "publishToMavenLocal")
    description = "Generate documentation and publish to Maven Local"
    group = "publishing"
}
