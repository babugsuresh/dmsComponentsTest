package hmrc.cds.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DmsComponentsTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(DmsComponentsTestApplication.class, args);
	}

}
