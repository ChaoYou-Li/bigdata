package pf.bluemoon.com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "pf.bluemoon.com")
@SpringBootApplication
public class GrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrapApplication.class, args);
    }

}
