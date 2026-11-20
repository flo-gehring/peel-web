plugins {
	java
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "de.flogehring"
version = providers.gradleProperty("releaseVersion").getOrElse("0.0.1-SNAPSHOT")
val peelVersion = providers.gradleProperty("peelVersion").orNull

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	providers.gradleProperty("peelRepositoryUrl").orNull?.let { repositoryUrl ->
		exclusiveContent {
			forRepository {
				maven {
					name = "peelCore"
					url = uri(repositoryUrl)
					if (url.scheme == "https" && url.host.equals("maven.pkg.github.com", ignoreCase = true)) {
						credentials(org.gradle.api.credentials.PasswordCredentials::class)
						authentication {
							create<org.gradle.authentication.http.BasicAuthentication>("basic")
						}
					}
				}
			}
			filter { includeModule("de.flo-gehring", "peel") }
		}
	}
	mavenCentral()
}

springBoot {
	buildInfo {
		excludes.add("time")
		properties {
			additional.set(mapOf("peel-core-version" to (peelVersion ?: "local-composite")))
		}
	}
}

dependencies {
	implementation(if (peelVersion == null) "de.flo-gehring:peel" else "de.flo-gehring:peel:$peelVersion")
	implementation("io.pebbletemplates:pebble:3.2.4")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.jspecify:jspecify:1.0.1")
	implementation( "org.springframework.boot:spring-boot-starter-security")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("io.minio:minio:8.6.0")
	implementation("com.github.librepdf:openpdf:3.0.5")
	implementation("com.github.librepdf:openpdf-html:3.0.5")
	implementation("org.apache.commons:commons-csv:1.11.0")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-test")
	testImplementation("org.springframework.boot:spring-boot-starter-mongodb-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:testcontainers-mongodb:2.0.1")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("peel.worker.classpath", sourceSets.main.get().runtimeClasspath.asPath)
}
