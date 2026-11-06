package com.seinfo.seinfomationA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SeInformationAApplication {

	public static void main(String[] args) {

		// [치트키] application.properties를 완전히 무시하고 자바 코드로 직접 주입
        System.setProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        System.setProperty("spring.datasource.url", "jdbc:mysql://b4ey5e476x38w3w45zjl-mysql.services.clever-cloud.com:3306/b4ey5e476x38w3w45zjl?serverTimezone=Asia/Seoul&characterEncoding=UTF-8");
        System.setProperty("spring.datasource.username", "ux60upem7b2vzj51");
        System.setProperty("spring.datasource.password", "T80dnemJc3hOVlZbbo0T");

        // JPA/하이버네이트 구동 차단 및 강제 종류 고정
        System.setProperty("spring.jpa.database", "mysql");
        System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
        System.setProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        System.setProperty("spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration");


		SpringApplication.run(SeInformationAApplication.class, args);
	}

}
