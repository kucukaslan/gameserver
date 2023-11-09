package tr.com.kucukaslan.gameserver;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import tr.com.kucukaslan.gameserver.service.DBService;

@SpringBootTest
class GameServerApplicationTests {
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
