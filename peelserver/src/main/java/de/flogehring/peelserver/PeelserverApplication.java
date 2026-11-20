package de.flogehring.peelserver;

import de.flogehring.peelserver.impl.run.PeelScriptWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Arrays;

@SpringBootApplication
public class PeelserverApplication {

	public static void main(String[] args) throws IOException {
		if (args.length == 3 && args[0].equals("--peel-script-worker")) {
			PeelScriptWorker.main(Arrays.copyOfRange(args, 1, args.length));
			return;
		}
		SpringApplication.run(PeelserverApplication.class, args);
	}

}
