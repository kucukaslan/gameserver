package tr.com.kucukaslan.dream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ch.qos.logback.classic.Logger;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.dream.service.DBService;

@Slf4j
@SpringBootApplication
public class DreamApplication {

	public static void main(String[] args) throws FileNotFoundException, IOException, SQLException {
		log.info("initializing DBService");
		DBService.getInstance().initialize();
		log.info("DBService initialized");
		
		
		log.info("DreamApplication is starting");
		// TODO DB connections etc.
		SpringApplication.run(DreamApplication.class, args);
	}

}
