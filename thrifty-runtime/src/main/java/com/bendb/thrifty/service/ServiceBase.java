package com.bendb.thrifty.service;

import com.bendb.thrifty.protocol.MessageMetadata;
import com.bendb.thrifty.protocol.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ServiceBase implements Closeable {
    public interface ServiceListener {
        void onTransportClosed();
        void onError(Throwable error);
    }

    private final Map<Integer, MethodCall> calls = new HashMap<>();
    private final AtomicInteger seqId = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Queue<MethodCall> outbox = new LinkedList<>();

    private final Protocol protocol;
    private final ServiceListener listener;
    private final RunLoop writer;
    private final RunLoop reader;

    protected ServiceBase(Protocol protocol,  ServiceListener listener) {
        this.protocol = protocol;
        this.listener = listener;
        this.writer = new WriterThread();
        this.reader = new ReaderThread();

        writer.setDaemon(true);
        reader.setDaemon(true);

        writer.start();
        reader.start();
    }

    protected void invoke(
            String name,
            boolean isOneWay,
            Sender sender,
            Receiver receiver) throws IOException {

        if (!running.get()) {
            throw new IOException("Cannot write to a closed service client");
        }


        int id = seqId.incrementAndGet();
        byte callType = isOneWay
                ? TMessageType.ONEWAY
                : TMessageType.CALL;

        MethodCall methodCall = new MethodCall(name, id, callType, sender, receiver);

        synchronized (outbox) {
            outbox.add(methodCall);
            outbox.notify();
        }
    }

    @Override
    public void close() throws IOException {
        close(null);
    }

    private void close(Throwable error) {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        reader.interrupt();
        writer.interrupt();

        outbox.notifyAll();
        calls.notifyAll();

        outbox.clear();
        calls.clear();

        try {
            protocol.close();
        } catch (IOException ignored) {
            // nope
        }

        if (error != null) {
            listener.onError(error);
        } else {
            listener.onTransportClosed();
        }
    }

    /**
     * A closure capturing all data necessary to send and receive an asynchronous
     * service method call.
     */
    private static class MethodCall {
        public final String name;
        public final int sequenceId;
        public final byte callTypeId;
        public final Sender sender;
        public final Receiver receiver;

        public MethodCall(
                String name,
                int sequenceId,
                byte callTypeId,
                Sender sender,
                Receiver receiver) {
            this.name = name;
            this.sequenceId = sequenceId;
            this.callTypeId = callTypeId;
            this.sender = sender;
            this.receiver = receiver;
        }
    }

    private abstract class RunLoop extends Thread {
        @Override
        public void run() {
            Throwable error = null;
            while (running.get()) {
                try {
                    act();
                } catch (InterruptedException e) {
                    // Either we were closed, in which case transition normally
                } catch (Exception e) {
                    error = e;
                    break;
                }
            }

            try {
                close(error);
            } catch (Throwable ignored) {
                // nope
            }
        }

        abstract void act() throws Exception;
    }

    private class WriterThread extends RunLoop {
        @Override
        void act() throws Exception {
            MethodCall call;
            synchronized (outbox) {
                while (outbox.isEmpty()) {
                    outbox.wait();
                }
                call = outbox.remove();
            }

            if (!running.get()) {
                return;
            }

            synchronized (calls) {
                calls.put(call.sequenceId, call);
            }

            protocol.writeMessageBegin(call.name, call.callTypeId, call.sequenceId);
            call.sender.send(protocol);
            protocol.writeMessageEnd();

            boolean hasMoreCalls;
            synchronized (outbox) {
                hasMoreCalls = !outbox.isEmpty();
            }

            if (!hasMoreCalls) {
                protocol.flush();
            }
        }
    }

    private class ReaderThread extends RunLoop {
        @Override
        void act() throws Exception {
            MessageMetadata messageMetadata = protocol.readMessageBegin();

            MethodCall call;
            synchronized (calls) {
                call = calls.get(messageMetadata.seqId);
            }

            if (call == null || !call.name.equals(messageMetadata.name)) {
                throw new IOException("Out-of-order response");
            }

            call.receiver.receive(protocol);
            protocol.readMessageEnd();
        }
    }
}
