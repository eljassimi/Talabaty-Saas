package ma.talabaty.talabaty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TalabatyApplication {

	public static void main(String[] args) {
		SpringApplication.run(TalabatyApplication.class, args);
	}

}
