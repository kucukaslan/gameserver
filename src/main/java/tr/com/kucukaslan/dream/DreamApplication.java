package tr.com.kucukaslan.dream;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.dream.service.DBService;

@Slf4j
@SpringBootApplication
public class DreamApplication {
	public static void main(String[] args) {


		log.info("DreamApplication is starting");
		SpringApplication.run(DreamApplication.class, args);
	}

}
