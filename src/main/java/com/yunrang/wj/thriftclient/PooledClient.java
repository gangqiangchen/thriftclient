package com.yunrang.wj.thriftclilent;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;

abstract class PooledClient<T> implements Client<T> {
    private String name;
    private int maxAllowableFailures = 5;
    private long retryInterval = 10000;

    private StackObjectPool pool = new StackObjectPool(new ConnectionFactory());
    private int numActive = pool.getNumActive();
    private int numIdle = pool.getNumIdle();
    private int poolSize = numActive + numIdle;

    private volatile long wentUnhealthyAt;
    private volatile AtomicInteger numFailures = new AtomicInteger(0);

    public abstract Connection<T> createConnection();

    public boolean isHealthy() {
        long now = System.currentTimeMillis();
        if(wentUnhealthyAt < now - retryInterval) {
            markHealthy();
            return true;
        } else
            return false;
    }

    public void didSucceed() { markHealthy(); }

    public void didFail() {
        if(numFailures.incrementAndGet() > maxAllowableFailures)
            markUnhealthy();
    }

    public void markUnhealthy() {
        wentUnhealthyAt = System.currentTimeMillis();
        logEvent(new UnhealthyEvent(wentUnhealthyAt));
    }

    public void markHealthy() {
        logEvent(new HealthyEvent(System.currentTimeMillis(), wentUnhealthyAt));
        wentUnhealthyAt = -1;
        numFailures.set(0);
    }

    public Connection<T> get() throws Exception {
        if(isHealthy()) {
            try {
                return (Connection<T>)pool.borrowObject();
            } catch(java.util.NoSuchElementException e) {
                didFail();
                return null;
            }
        } else
            return null;
    }

    public void put(Connection<T> conn) throws Exception{
        if(conn.didFail || !conn.isHealthy()) {
            didFail();
        } else {
            pool.returnObject(conn);
            didSucceed();
        }
    }

    public void logEvent(ClientEvent e) {

    }


    class ConnectionFactory implements PoolableObjectFactory {
        public Object makeObject() {
            Connection<T> c = createConnection();
            c.ensureOpen();
            return (Object)c;
        }

        public boolean validateObject(Object o) {
            return ((Connection<T>)o).isHealthy();
        }

        public void destroyObject(Object o) {
            ((Connection<T>)o).teardown();
        }

        public void activateObject(Object o) {
            ((Connection<T>)o).ensureOpen();
        }

        public void passivateObject(Object o) {
            ((Connection<T>)o).flush();
        }
    }

}


abstract class ClientEvent {}

class UnhealthyEvent extends ClientEvent {
    public UnhealthyEvent(long time) {

    }
}

class HealthyEvent extends ClientEvent {
    public HealthyEvent(long time, long unhealthyTime) {

    }
}

class TimeoutEvent extends ClientEvent {
    public TimeoutEvent(long time) {

    }
}
