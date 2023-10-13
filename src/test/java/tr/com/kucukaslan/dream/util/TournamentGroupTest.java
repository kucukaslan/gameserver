package tr.com.kucukaslan.dream.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TournamentGroupTest {
    String[] countries = new String[] { "TR", "US", "DE", "FR", "GB" };
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    void TestJoin() {

        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for (Long i = 0L; i < 5; i++) {
            Long j = i;
            executorService.submit(() -> {
                log.debug("{}: joining country: {} ", j.intValue(), countries[j.intValue()]);
                TournamentGroup group = TournamentGroup.join(countries[j.intValue()], j);
                log.debug("{} joined country {} to group: {} ", j.intValue(), countries[j.intValue()], group);
                try {
                    group.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                log.debug("{}: woke up", j);

            });
        }

        assertEquals("Hello Baeldung Readers!!\n", outputStreamCaptor.toString());

        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    void TestPut() {

    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }
}
