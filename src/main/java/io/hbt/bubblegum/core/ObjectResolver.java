package io.hbt.bubblegum.core;

import com.google.common.base.Charsets;
import io.hbt.bubblegum.core.auxiliary.BufferPool;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class ObjectResolver {

    ObjectResolver() {
        if(Configuration.ENABLE_OBJECT_RESOLVER) {
            System.out.println("ObjectResolver started");
            CapitalizeServer.main();
//            try {
//                CapitalizeClient.main();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            while(true) {
//                try {
//                    Thread.sleep(1000);
//                    DateClient.main();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            new Thread(() -> AsyncEchoServer.go()).start();
//            try {
//                AsyncEchoClient.go();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }

    public static class CapitalizeServer {

        /**
         * Runs the server. When a client connects, the server spawns a new thread to do
         * the servicing and immediately returns to listening. The application limits the
         * number of threads via a thread pool (otherwise millions of clients could cause
         * the server to run out of resources by allocating too many threads).
         */
        public static void main() {
            try (var listener = new ServerSocket(59898)) {
                System.out.println("The capitalization server is running...");
                var pool = Executors.newFixedThreadPool(5);
                while (true) {
                    pool.execute(new Capitalizer(listener.accept()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static class Capitalizer implements Runnable  {
            private Socket socket;

            Capitalizer(Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                System.out.println("Connected: " + socket);
                try {
                    var in = new Scanner(socket.getInputStream());
//                    var out = new PrintWriter(socket.getOutputStream(), true);
                    StringBuilder sb = new StringBuilder();

                    if(in.hasNextLine()) {
                        byte[] key = in.nextLine().getBytes(Charsets.US_ASCII);
                        System.out.println("Key: " + new String(key, Charsets.US_ASCII));
                        FileInputStream fileInput = new FileInputStream("cl.jpg");
                        byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
                        IvParameterSpec ivspec = new IvParameterSpec(iv);

                        final Cipher c2 = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        SecretKey originalKey = new SecretKeySpec(key, "AES");
                        c2.init(Cipher.ENCRYPT_MODE, originalKey, ivspec);
                        CipherOutputStream os = new CipherOutputStream(socket.getOutputStream(), c2);
                        byte[] buffer = BufferPool.getOrCreateBuffer();
                        int i;
                        while((i = fileInput.read(buffer)) != -1) {
                            os.write(buffer, 0, i);
                        }
                        BufferPool.release(buffer);
                        os.flush();
                        os.close();
                    }

                } catch (Exception e) {
                    System.out.println("Error:" + socket);
                } finally {
                    try { socket.close(); } catch (IOException e) {}
                    System.out.println("Server Closed: " + socket);
                }
            }
        }
    }

    public static class CapitalizeClient {
        public static void main() throws Exception {
            try (var socket = new Socket("localhost", 59898)) {

                var out = new PrintWriter(socket.getOutputStream(), true);
                out.println("qwertyuiopasdfgh"); // 16 bytes in ascii

                byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
                IvParameterSpec ivspec = new IvParameterSpec(iv);

                final Cipher c2 = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKey originalKey = new SecretKeySpec("qwertyuiopasdfgh".getBytes(Charsets.US_ASCII), "AES");
                c2.init(Cipher.DECRYPT_MODE, originalKey, ivspec);
                CipherInputStream in = new CipherInputStream(socket.getInputStream(), c2);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] b = new byte[256];
                int numberOfBytedRead;
                while ((numberOfBytedRead = in.read(b)) >= 0) {
                    baos.write(b, 0, numberOfBytedRead);
                }
                System.out.println(new String(baos.toByteArray(), Charsets.US_ASCII));

                out.close();
            }
        }
    }

//
//    public static class AsyncEchoServer {
//        private AsynchronousServerSocketChannel serverChannel;
//        private Future<AsynchronousSocketChannel> acceptResult;
//        private AsynchronousSocketChannel clientChannel;
//
//        public AsyncEchoServer() {
//            try {
//                serverChannel = AsynchronousServerSocketChannel.open();
//                InetSocketAddress hostAddress = new InetSocketAddress("127.0.0.1", 4999);
//                serverChannel.bind(hostAddress);
//                acceptResult = serverChannel.accept();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        public void runServer() {
//            try {
//                clientChannel = acceptResult.get();
//                if ((clientChannel != null) && (clientChannel.isOpen())) {
//                    while (true) {
//
//                        ByteBuffer buffer = ByteBuffer.allocate(320);
//                        Future<Integer> readResult = clientChannel.read(buffer);
//
//                        // do some computation
//
//                        readResult.get();
//
//                        buffer.flip();
//                        String message = "Hi there!";//new String(buffer.array()).trim();
//                        if (message.equals("bye")) {
//                            break; // while loop
//                        }
//                        buffer = ByteBuffer.wrap(new String(message).getBytes());
//                        Future<Integer> writeResult = clientChannel.write(buffer);
//
//                        // do some computation
//                        writeResult.get();
//                        buffer.clear();
//
//                    } // while()
//
//                    clientChannel.close();
//                    serverChannel.close();
//
//                }
//            } catch (InterruptedException | ExecutionException | IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//
//        public static void go() {
//            AsyncEchoServer server = new AsyncEchoServer();
//            server.runServer();
//        }
//    }
//
//    public static class AsyncEchoClient {
//
//        private AsynchronousSocketChannel client;
//        private Future<Void> future;
//        private static AsyncEchoClient instance;
//
//        private AsyncEchoClient() {
//            try {
//                client = AsynchronousSocketChannel.open();
//                InetSocketAddress hostAddress = new InetSocketAddress("127.0.0.1", 4999);
//                future = client.connect(hostAddress);
//                start();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        public static AsyncEchoClient getInstance() {
//            if (instance == null)
//                instance = new AsyncEchoClient();
//            return instance;
//        }
//
//        private void start() {
//            try {
//                future.get();
//            } catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//            }
//        }
//
//        public String sendMessage(String message) {
//            byte[] byteMsg = message.getBytes();
//            ByteBuffer buffer = ByteBuffer.wrap(byteMsg);
//            Future<Integer> writeResult = client.write(buffer);
//
//            try {
//                writeResult.get();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            buffer.flip();
//            Future<Integer> readResult = client.read(buffer);
//            try {
//                readResult.get();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            String echo = new String(buffer.array()).trim();
//            buffer.clear();
//            return echo;
//        }
//
//        public void stop() {
//            try {
//                client.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        public static void go() throws Exception {
//            AsyncEchoClient client = AsyncEchoClient.getInstance();
//            client.start();
//            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//            String line;
//            System.out.println("Message to server:");
//            while ((line = br.readLine()) != null) {
//                String response = client.sendMessage(line);
//                System.out.println("response from server: " + response);
//                System.out.println("Message to server:");
//            }
//        }
//
//    }

    public static void main(String[] args) {
        new ObjectResolver();
    }
}



/*
       RandomAccessFile aFile = new RandomAccessFile
                ("test.txt", "r");
        FileChannel inChannel = aFile.getChannel();
        MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
        buffer.load();
        for (int i = 0; i < buffer.limit(); i++)
        {
            System.out.print((char) buffer.get());
        }
        buffer.clear(); // do something with the data and clear/compact it.
        inChannel.close();
        aFile.close();
 */