package az.fitnest.iamservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableFeignClients
@EnableMethodSecurity
@EnableCaching
@EnableJpaAuditing
public class IamServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IamServiceApplication.class, args);
	}

}
