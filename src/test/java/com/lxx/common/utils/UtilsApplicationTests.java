package com.lxx.common.utils;

import com.lxx.common.utils.redis.RedisCheckInUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UtilsApplicationTests {

    @Autowired
    RedisCheckInUtil redisCheckInUtil;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testBitMap() {

        System.out.println("开始");

        for (int i = 0; i < 10000000; i++) {
            String replace = UUID.randomUUID().toString().replace("-", "");
            long abs = Math.abs(replace.hashCode());
            redisCheckInUtil.checkIn(abs);
        }

        System.out.println("结束");

    }

    @Test
    public void testImportExcel(){

    }

}
