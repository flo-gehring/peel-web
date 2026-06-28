package de.flogehring.peelserver;

import de.flogehring.peelserver.projects.ProjectSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProjectSettings.class)
public class PeelserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(PeelserverApplication.class, args);
	}

}
