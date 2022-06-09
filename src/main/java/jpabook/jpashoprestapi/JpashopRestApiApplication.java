package jpabook.jpashoprestapi;

import com.p6spy.engine.spy.P6SpyOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class JpashopRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JpashopRestApiApplication.class, args);
    }

    @Configuration
    public class P6spyConfig {
        @PostConstruct
        public void setLogMessageFormat() {
            P6SpyOptions.getActiveInstance().setLogMessageFormat(P6spyPrettySqlFormatter.class.getName());
        }
    }

}
