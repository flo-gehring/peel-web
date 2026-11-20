rootProject.name = "peelserver"

if (!providers.gradleProperty("peelVersion").isPresent) {
    includeBuild("../../peel")
}
