package com.yunrang.wj.thriftclilent;

public class ThriftClient<I, C extends I> extends PooledClient<C> {

    private boolean framed = false;
    private String host = null;
    private int port = -1;
    private long soTimeout;
    private String name;

    private int maxIdleConnections = 100;

    public ThriftClient(String host, int port, boolean framed, long soTimeout, String name) {
        this.host = host;
        this.port = port;
        this.framed = framed;
        this.soTimeout = soTimeout;
        this.name = name;
    }

    public ThriftClient(String host, int port, boolean framed) {
        this(host, port, framed, 5000, C.class.getClassName());
    }

    public ThriftConnection<C> createConnection() {
        return new ThriftConnection<C>(host, port, framed);
    }

}