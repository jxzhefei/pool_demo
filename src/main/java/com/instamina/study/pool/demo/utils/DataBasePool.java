package com.instamina.study.pool.demo.utils;

import java.sql.Connection;
import java.util.concurrent.TimeoutException;

public interface DataBasePool {
    void init(int initSize,int maxSize,int idleCount,long timeout);
    void destory();
    Connection getConnection() throws TimeoutException;
    void release(Connection conn);
}
