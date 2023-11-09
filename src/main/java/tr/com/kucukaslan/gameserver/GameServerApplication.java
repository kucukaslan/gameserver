package tr.com.kucukaslan.gameserver;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.gameserver.service.DBService;

@Slf4j
@SpringBootApplication
public class GameServerApplication {

	public static void main(String[] args) {

		log.info("initializing DBService");
		try {
			DBService.getInstance().initialize();
		} catch (IOException | SQLException e) {
			log.error("Error while initializing DBService due to {}", e.getMessage());
			log.debug("Exception: {}", e, e.getMessage());
			log.debug("Stack trace: {}", String.valueOf(e.getStackTrace()));
			DBService.close();
			System.exit(1);
		}
		log.info("DBService initialized");

		log.info("GameServerApplication is starting");
		SpringApplication.run(GameServerApplication.class, args);
	}

}
