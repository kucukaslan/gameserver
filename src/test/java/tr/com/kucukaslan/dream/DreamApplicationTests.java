package tr.com.kucukaslan.dream;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import tr.com.kucukaslan.dream.service.DBService;

@SpringBootTest
class DreamApplicationTests {
	// create a test database
	// create a test tables
	// create a test data
	@Test
	void testDBInitialization() {
		try {
			DBService.getInstance().initialize();
		} catch (IOException | SQLException e) {
			throw new AssertionError("Error while initializing DBService due to {}" + e.getMessage());
		}
	}
}
