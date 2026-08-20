package ${package};

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AweBootApplicationTest {

    /**
     * Register handler for 'classpath' protocol of Tomcat server
     */
    @BeforeAll
    static void init() {
        org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.getInstance();
    }

    @Test
    void contextLoads() {
    }
}
