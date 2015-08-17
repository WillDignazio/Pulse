package net.digitalbebop.http;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.FiberServerSocketChannel;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

public class FiberTest {

    private void writeToServer(String host, int port, byte[] data) throws IOException {
        Socket sock = new Socket(host, port);
        if (!sock.isConnected()) {
            throw new RuntimeException("Could not write to server");
        }

        OutputStream stream = sock.getOutputStream();
        stream.write(data);
        stream.flush();
        stream.close();
    }

    private int readFromServer(String host, int port, byte[] buffer) throws IOException {
        Socket sock = new Socket(host, port);
        if (!sock.isConnected()) {
            throw new RuntimeException("Could not read from server");
        }

        InputStream stream = sock.getInputStream();
        int got = stream.read(buffer);
        stream.close();
        sock.close();

        return got;
    }

    @Test
    public void basicReadArrayTest() throws SuspendExecution, ExecutionException, InterruptedException {

        new Fiber<Void>(() -> {
            byte[] testData = { 0, 1, 2, 3, 4, 5 };

            try {
                FiberServerSocketChannel channel = FiberServerSocketChannel.open().bind(new InetSocketAddress("127.0.0.1", 5555));

                writeToServer("127.0.0.1", 5555, testData);

                FiberSocketChannel child = channel.accept();
                FiberSocketChannelInputStream stream = new FiberSocketChannelInputStream(child);
                byte[] mimic = new byte[testData.length];

                Assert.assertEquals(testData.length, stream.read(mimic));
                Assert.assertArrayEquals(testData, mimic);

                child.close();
                channel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start().get();
    }

    @Test
    public void basicReadTest() throws SuspendExecution, ExecutionException, InterruptedException {

        new Fiber<Void>(() -> {
            byte[] testData = { 0, 1, 2, 3, 4, 5 };

            try {
                FiberServerSocketChannel channel = FiberServerSocketChannel.open().bind(new InetSocketAddress("127.0.0.1", 5555));

                writeToServer("127.0.0.1", 5555, testData);

                FiberSocketChannel child = channel.accept();
                FiberSocketChannelInputStream stream = new FiberSocketChannelInputStream(child);
                byte[] mimic = new byte[6];

                for (int bidx=0; bidx < mimic.length; ++bidx) {
                    mimic[bidx] = (byte)stream.read();
                }

                Assert.assertArrayEquals(testData, mimic);

                child.close();
                channel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start().get();
    }

    @Test
    public void echoServerTest() throws SuspendExecution, ExecutionException, InterruptedException, IOException {
        byte[] testData = { 0, 1, 2, 3, 4, 5 };

        Fiber serverFiber = new Fiber<Void>(() -> {
            try {
                FiberServerSocketChannel channel = FiberServerSocketChannel.open().bind(new InetSocketAddress("127.0.0.1", 5555));
                System.out.println("Server started.");

                Fiber clientFiber = new Fiber<>(() -> {
                    try {
                        FiberSocketChannel ch = FiberSocketChannel.open(new InetSocketAddress("127.0.0.1", 5555));
                        FiberSocketChannelOutputStream fcos = new FiberSocketChannelOutputStream(ch);
                        FiberSocketChannelInputStream fcis = new FiberSocketChannelInputStream(ch);

                        fcos.write(testData);
                        fcos.flush();
                        System.out.println("Client wrote out test data.");

                        byte[] clientMimic = new byte[testData.length];
                        Assert.assertEquals(testData.length, fcis.read(clientMimic));
                        Assert.assertArrayEquals(testData, clientMimic);

                        System.out.println("Client read in echo, data matched.");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                System.out.println("Opened channel to 5555");
                try {
                    FiberSocketChannel child = channel.accept();
                    FiberSocketChannelInputStream fis = new FiberSocketChannelInputStream(child);
                    FiberSocketChannelOutputStream fos = new FiberSocketChannelOutputStream(child);

                    System.out.println("Accepted socket connection.");

                    byte[] mimic = new byte[testData.length];
                    Assert.assertEquals(testData.length, fis.read(mimic));

                    System.out.println("Server read in socket data:");
                    for(int bidx=0; bidx < mimic.length; ++bidx) {
                        System.out.println(bidx + ".) " + mimic[bidx]);
                    }

                    fos.write(mimic);

                    System.out.println("Server wrote out socket data echo");

                    fos.flush();
                    fos.close();
                    fis.close();
                    child.close();

                    System.out.println("Finished server.");
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }

                channel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        serverFiber.join();
    }
}
