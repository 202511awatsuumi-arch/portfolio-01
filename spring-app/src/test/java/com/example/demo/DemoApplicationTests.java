package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
		properties = {
				"spring.datasource.url=jdbc:h2:mem:demo-application-test;DB_CLOSE_DELAY=-1;MODE=LEGACY",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.jpa.hibernate.ddl-auto=validate",
				"spring.flyway.enabled=true"
		})
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
