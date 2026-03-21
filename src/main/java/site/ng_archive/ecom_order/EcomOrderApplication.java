package site.ng_archive.ecom_order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import site.ng_archive.ecom_common.config.EnableEcomCommon;

@EnableEcomCommon
@SpringBootApplication
public class EcomOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcomOrderApplication.class, args);
	}

}
