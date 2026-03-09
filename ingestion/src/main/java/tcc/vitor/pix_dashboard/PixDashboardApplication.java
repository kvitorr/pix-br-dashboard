package tcc.vitor.pix_dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PixDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(PixDashboardApplication.class, args);
	}

}
