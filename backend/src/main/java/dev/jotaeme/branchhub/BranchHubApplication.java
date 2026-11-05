package dev.jotaeme.branchhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BranchHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(BranchHubApplication.class, args);
	}

}
