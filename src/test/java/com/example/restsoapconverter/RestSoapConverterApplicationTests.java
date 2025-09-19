package com.example.restsoapconverter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class RestSoapConverterApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
    }

}
