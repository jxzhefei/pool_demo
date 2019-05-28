package com.instamina.study.pool.demo.utils;

import java.sql.Connection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultDataBasePool implements DataBasePool {
    private int initSize;
    private int maxSize;
    private int idleCount;
    private long timeout;
    private BlockingQueue<Connection> busy;
    private BlockingQueue<Connection> idle;
    private AtomicInteger activeSize = new AtomicInteger();

    @Override
    public void init(int initSize, int maxSize, int idleCount, long timeout) {
        this.initSize = initSize;
        this.maxSize = maxSize;
        this.idleCount = idleCount;
        this.timeout = timeout;
        idle = new LinkedBlockingQueue<>();
        busy = new LinkedBlockingQueue<>();
        initConnection(this.initSize);
    }

    private void initConnection(int initSize) {
        for (int i = 0; i <initSize ; i++) {
            if(activeSize.get() < maxSize){
                if(activeSize.incrementAndGet() <= maxSize){
                   Connection conn =  DBUtil.getConnection();
                   idle.offer(conn);
                }else{
                    activeSize.decrementAndGet();

                }
            }
        }
    }

    @Override
    public void destory() {

    }

    @Override
    public Connection getConnection() throws TimeoutException {
        long start = System.currentTimeMillis();
        Connection conn = idle.poll();
        if(conn != null){
            busy.offer(conn);
            System.out.println("从空闲中获取连接。。。");
            return conn;
        }
        if(activeSize.get() < maxSize){
            if(activeSize.incrementAndGet() <= maxSize){
                conn = DBUtil.getConnection();
                busy.offer(conn);
                System.out.println("新建的连接。。。");
                return conn;
            }else{
                activeSize.decrementAndGet();
            }
        }
        long waitTime = timeout - (System.currentTimeMillis()-start);
        try {
            conn = idle.poll(waitTime, TimeUnit.MILLISECONDS);
            busy.offer(conn);
            System.out.println("等待获取连接。。。");
        } catch (Exception e) {
            e.printStackTrace();
            throw new TimeoutException("获取连接超时。。。");
        }
        return conn;
    }

    @Override
    public void release(Connection conn) {
        if(conn == null)
            return;
        if(busy.remove(conn)){
            if(idle.size() > idleCount){
                DBUtil.close(conn);
                activeSize.decrementAndGet();
            }else{
                if(!idle.offer(conn)){
                    DBUtil.close(conn);
                    activeSize.decrementAndGet();
                }
            }
        }else{
            DBUtil.close(conn);
            activeSize.decrementAndGet();
        }
    }
}
