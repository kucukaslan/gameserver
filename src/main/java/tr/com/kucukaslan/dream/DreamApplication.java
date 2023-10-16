package tr.com.kucukaslan.dream;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.dream.service.DBService;

@Slf4j
@SpringBootApplication
public class DreamApplication {

	public static void main(String[] args) {
		
			log.info("initializing DBService");
			try {
				DBService.getInstance().initialize();
			} catch (IOException | SQLException e) {
				log.error("Error while initializing DBService due to {}", e.getMessage());
				log.debug("Exception: {}", e);
				log.debug("Stack trace: {}",String.valueOf(e.getStackTrace()));
				DBService.close();
				System.exit(1);
			}
			log.info("DBService initialized");
				
		
		log.info("DreamApplication is starting");
		SpringApplication.run(DreamApplication.class, args);
	}

}
