package com.instamina.study.pool.demo.controller;


import com.instamina.study.pool.demo.utils.DataBasePool;

import com.instamina.study.pool.demo.utils.DefaultDataBasePool;
import org.junit.Test;

import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class poolTest {
    private int threadSize = 200;
    private AtomicInteger count = new AtomicInteger();
    @Test
    public void test(){
        CountDownLatch latch = new CountDownLatch(1);
        DataBasePool pool = new DefaultDataBasePool();
        pool.init(20,40,20,1000);
        for(int i=0;i<threadSize;i++){
            Thread thread = new Thread(()->{
                Connection conn = null;
                try {
                    conn = pool.getConnection();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                try {
                    latch.await();
                    PreparedStatement ps = conn.prepareStatement("select user_name from user limit 1");
                    ResultSet rs = ps.executeQuery();
                    while(rs.next()){
                        String userName = rs.getString(1);
                        System.out.println("我的名字是"+userName+"---"+count.incrementAndGet());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    pool.release(conn);
                }
                }
            );
            thread.start();
        }
        latch.countDown();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
