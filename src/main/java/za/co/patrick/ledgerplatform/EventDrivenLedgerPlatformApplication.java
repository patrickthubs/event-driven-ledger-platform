package za.co.patrick.ledgerplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventDrivenLedgerPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventDrivenLedgerPlatformApplication.class, args);
    }
}
