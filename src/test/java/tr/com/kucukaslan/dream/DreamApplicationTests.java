package tr.com.kucukaslan.dream;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tr.com.kucukaslan.dream.service.DBService;

@SpringBootTest
class DreamApplicationTests {

	@Autowired
    private static DBService dbService;
	// create a test database
	// create a test tables
	// create a test data
	@Test
	void testDBInitialization() {
		try {
			dbService.initialize();
		} catch (IOException | SQLException e) {
			throw new AssertionError("Error while initializing DBService due to {}" + e.getMessage());
		}
	}
}
