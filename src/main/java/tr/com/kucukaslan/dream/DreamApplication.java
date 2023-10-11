package tr.com.kucukaslan.dream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ch.qos.logback.classic.Logger;
import lombok.extern.java.Log;

@SpringBootApplication
public class DreamApplication {

	public static void main(String[] args) {
		// TODO enable logging
		// Disable logging for now
		// to disable logging for all projects, add this to the pom.xml:

		// TODO DB connections etc.
		SpringApplication.run(DreamApplication.class, args);
	}

}
