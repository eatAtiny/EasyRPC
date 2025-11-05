package com.easy.rpc.springconsumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class SpringConsumerApplicationTests {

    @Resource
    private ExampleServiceImpl exampleServiceImpl;

    @Test
    void test() {
        exampleServiceImpl.test();
    }

}
