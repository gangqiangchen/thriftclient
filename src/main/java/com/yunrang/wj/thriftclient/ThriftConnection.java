package com.yunrang.wj.thriftclilent;

import java.io.IOException;
import java.lang.reflect.Constructor;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TSocket;

class ThriftConnection<T> extends Connection<T> {

    private String host;
    private int port;
    private boolean framed;

    private long SO_TIMEOUT = 5000;
    private TSocket socket;
    private TTransport transport;
    private TProtocol protocol;
    private boolean didFailConnect = false;
    private Constructor constructor = T.class.getDeclaredConstructor(TProtocol.class);
    private T client;

    public ThriftConnection(String host, int port, boolean framed) {
        this.host = host;
        this.port = port;
        this.framed = framed;
        socket = new TSocket(host, port, SO_TIMEOUT);
        if(framed) transport = new TFramedTransport(socket);
        else transport = socket;
        protocol = new TBinaryProtocol(transport);
        client = (T)constructor.newInstance(protocol);
    }

    public void ensureOpen() {
        if(transport.isOpen()) return;
        try {
            transport.open();
        } catch (TTransportException e) {
            didFailConnect = true;
        }
    }

    public void teardown() {
        try{
            transport.close();
        } catch (IOException e) {

        } catch (TException e) {

        }
    }

    public void flush() {}

    public boolean idHealthy() {
        return !didFailConnect && super.isHealthy;
    }
}

interface Client<T> {
    public T proxy();
    public boolean isHealthy();
}

abstract class Connection<T> {
    public T client;
    public String host;
    public int port;
    public boolean didFail;

    public abstract void ensureOpen();
    public abstract void teardown();
    public abstract void flush();

    public boolean isHealthy() {
        return !didFail;
    }

    public void markFailed() {
        didFail = true;
    }
}