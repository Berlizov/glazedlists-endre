/**
 * Glazed Lists
 * http://glazedlists.dev.java.net/
 *
 * COPYRIGHT 2003 O'DELL ENGINEERING LTD.
 */
package ca.odell.glazedlists.net;

import java.util.*;
// for being a JUnit test case
import junit.framework.*;
// NIO is used for CTP
import java.nio.*;
import java.nio.channels.*;
import java.io.UnsupportedEncodingException;

/**
 * A CTPHandler where all data is known beforehand.
 */
class StaticCTPHandler implements CTPHandler {
    
    /** the actions to be performed on this connection */
    private List tasks = new ArrayList();
    
    /** whether this connection has connected */
    private boolean ready = false;
    
    /** whether this connection has disconnected */
    private boolean closed = false;
    
    
    /**
     * Add expected incoming data.
     */
    public void addExpected(String data) {
        tasks.add(new Expected(data));
    }
    
    /**
     * Add queued outgoing data.
     */
    public void addEnqueued(String data) {
        tasks.add(new Enqueued(data));
    }
    
    /**
     * Notify that this connection is ready for use.
     */
    public synchronized void connectionReady(CTPConnection source) {
        if(ready) throw new IllegalStateException("Connection already ready");
        ready = true;
        handlePendingTasks(source);
    }
    
    /**
     * Handle the specified incoming data.
     */
    public synchronized void receiveChunk(CTPConnection source, ByteBuffer data) {
        if(!data.hasRemaining()) return;
        
        if(tasks.size() == 0) throw new IllegalStateException("Unexpected data " + data);
        Expected expected = (Expected)tasks.get(0);
        int remain = expected.consume(data);
        if(remain == 0) {
            tasks.remove(0);
            handlePendingTasks(source);
        }
    }
    
    /**
     * Notify that this connection is no longer ready for use.
     */
    public synchronized void connectionClosed(CTPConnection source, Exception reason) {
        if(closed) throw new IllegalStateException("Connection already closed");
        closed = true;
        notifyAll();
    }
    
    /**
     * Handle pending tasks that can be performed immediately.
     */
    private void handlePendingTasks(CTPConnection connection) {
        while(tasks.size() > 0 && tasks.get(0) instanceof Enqueued) {
            Enqueued enqueued = (Enqueued)tasks.remove(0);
            connection.sendChunk(enqueued.getData());
        }
        if(tasks.isEmpty()) {
            connection.close();
        }
    }
    
    /**
     * Whether all tasks have completed.
     */
    public synchronized boolean isDone() {
        return (tasks.isEmpty() && closed);
    }
    
    /**
     * Waits for this connection to be completed.
     */
    public synchronized void waitForCompletion(long timeout) {
        if(closed) return;
        try {
            wait(timeout);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(!closed) throw new IllegalStateException("Complete timed out without close");
    }
}

/**
 * Models an expected incoming chunk.
 */
class Expected {
    private byte[] data;
    private int offset = 0;
    public Expected(String charData) {
        try {
            this.data = charData.getBytes("US-ASCII");
        } catch(UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
    /**
     * Consumes the specified data, which must match the expected data. If this does
     * not match, an Exception was thrown.
     *
     * @return the number of bytes remaining to be consumed.
     */
    public int consume(ByteBuffer lunch) {
        for( ; offset < data.length && lunch.hasRemaining(); offset++) {
            if(lunch.get() != data[offset]) throw new IllegalStateException();
        }
        return data.length - offset;
    }
    /**
     * Get the number of bytes that this has left to consume.
     */
    public int bytesLeft() {
        return data.length - offset;
    }
}

/**
 * Models an outgoing chunk.
 */
class Enqueued {
    private ByteBuffer data;
    public Enqueued(String charData) {
        try {
            this.data = ByteBuffer.wrap(charData.getBytes("US-ASCII"));
        } catch(UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
    public ByteBuffer getData() {
        return data;
    }
}

/**
 * A static handler for handing predictable test data.
 */
class StaticCTPHandlerFactory implements CTPHandlerFactory {
    private List handlers = new ArrayList();
    public void addHandler(CTPHandler handler) {
        handlers.add(handler);
    }
    public CTPHandler constructHandler() {
        if(handlers.isEmpty()) throw new IllegalStateException("No more handlers");
        return (CTPHandler)handlers.remove(0);
    }
}
