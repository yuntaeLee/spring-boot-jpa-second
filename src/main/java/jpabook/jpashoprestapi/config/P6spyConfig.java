package jpabook.jpashoprestapi.config;

import com.p6spy.engine.spy.P6SpyOptions;
import jpabook.jpashoprestapi.p6spy.P6spyPrettySqlFormatter;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class P6spyConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().
                setLogMessageFormat(P6spyPrettySqlFormatter.class.getName());
    }
}
