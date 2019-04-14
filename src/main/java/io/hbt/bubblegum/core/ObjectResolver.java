package io.hbt.bubblegum.core;

import com.google.common.base.Charsets;
import io.hbt.bubblegum.core.auxiliary.BufferPool;
import io.hbt.bubblegum.core.auxiliary.MIMEHelper;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.auxiliary.ObjectResolutionDetails;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.auxiliary.ProtobufHelper;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaMessage;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObjectResolver {

    private boolean running = false;
    private int currentPort = -1;
    private HashMap<String, ObjectResolutionRequestRecord> activeRequests = new HashMap<>();
    private Random r = new Random();
    private Path assetsFolder;

    ObjectResolver() {
        if(Configuration.ENABLE_OBJECT_RESOLVER) {
            this.assetsFolder = Paths.get(Configuration.RESOLVER_ASSETS_FOLDER);
            if(this.prepareAssetsFolder()) {
                this.running = true;
                new Thread(() -> {
                    while (this.running) {
                        try (var listener = new ServerSocket(0)) {
                            this.currentPort = listener.getLocalPort();
                            System.out.println("OR started on port " + this.currentPort);
                            ObjectResolutionServer.run(Configuration.RESOLVER_SERVER_THREADS, listener, this.activeRequests);
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.err.println("ObjectResolutionServer restarted");
                            //e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }

    private boolean prepareAssetsFolder() {
        if(Files.exists(this.assetsFolder)) return true;
        else {
            try {
                Files.createDirectory(this.assetsFolder);
                return true;
            } catch (IOException e) {
                System.err.println("Couldn't create assets folder!");
                return false;
            }
        }
    }

    public boolean declareNewResource(String uri) {
        if(ObjectResolver.hasResource(uri)) {

        }
        return false;
    }

    public static boolean hasResource(String uri) {
        return Files.exists(Paths.get(Configuration.RESOLVER_ASSETS_FOLDER + uri));
    }

    public BgKademliaMessage.KademliaMessage newRequest(BubblegumNode local, RouterNode to, String eid, String hostname, String uri) {
        if(ObjectResolver.hasResource(uri)) {
            byte[] array = new byte[16];
            r.nextBytes(array);
            String encryptionKey = "qwertyuiopasdfgh";//new String(array, Charsets.US_ASCII);
            String requestKey = UUID.randomUUID().toString();
            System.out.println(String.join(", ", hostname, uri, encryptionKey, requestKey));
            this.activeRequests.put(
                requestKey,
                new ObjectResolutionRequestRecord(hostname, uri, requestKey, encryptionKey)
            );
            return ProtobufHelper.buildResourceResponse(
                local, to, eid, NetworkingHelper.getLocalInetAddress().getHostAddress(),
                this.currentPort, requestKey, encryptionKey, MIMEHelper.fileNameToMimeType(uri));
        }
        return ProtobufHelper.buildResourceResponse(
            local, to, eid, "", -1, "", "", "");
    }


    public Pair<Socket, InputStream> client(ObjectResolutionDetails details) {
        try {
            Socket socket = new Socket(details.hostname, details.port);
            var out = new PrintWriter(socket.getOutputStream(), true);
            out.println(details.requestKey); // 16 bytes in ascii

            byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            final Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey originalKey = new SecretKeySpec(details.encryptionKey.getBytes(Charsets.US_ASCII), "AES");
            c.init(Cipher.DECRYPT_MODE, originalKey, ivspec);
            CipherInputStream in = new CipherInputStream(socket.getInputStream(), c);
            return new Pair<>(socket, in);
        } catch (Exception e) {
            return null;
        }
    }


    private static class ObjectResolutionServer {

        /**
         * Runs the server. When a client connects, the server spawns a new thread to do
         * the servicing and immediately returns to listening. The application limits the
         * number of threads via a thread pool (otherwise millions of clients could cause
         * the server to run out of resources by allocating too many threads).
         */
        public static void run(int threads, ServerSocket listener, HashMap<String, ObjectResolutionRequestRecord> requests) throws IOException {
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            while (true) {
                pool.execute(new ObjectResolutionHandler(listener.accept(), requests));
            }
        }

        private static class ObjectResolutionHandler implements Runnable  {
            private Socket socket;
            private HashMap<String, ObjectResolutionRequestRecord> requests;

            ObjectResolutionHandler(Socket socket, HashMap<String, ObjectResolutionRequestRecord> requests) {
                this.socket = socket;
                this.requests = requests;
            }

            @Override
            public void run() {
                System.out.println("Connected: " + socket);
                try {
                    var in = new Scanner(socket.getInputStream());
//                    var out = new PrintWriter(socket.getOutputStream(), true);
                    StringBuilder sb = new StringBuilder();

                    if(in.hasNextLine()) {
                        String requestKey = in.nextLine();
                        if(this.requests.containsKey(requestKey)) {
                            // TODO remove
                            ObjectResolutionRequestRecord record = this.requests.get(requestKey);
                            System.out.println(socket.getInetAddress().getHostAddress());
                            if(record.hostname.equals(socket.getInetAddress().getHostAddress())) {
                                if(ObjectResolver.hasResource(record.uri)) {
                                    byte[] key = record.encryptionKey.getBytes(Charsets.US_ASCII);
                                    FileInputStream fileInput = new FileInputStream(Configuration.RESOLVER_ASSETS_FOLDER + record.uri);
                                    byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                    IvParameterSpec ivspec = new IvParameterSpec(iv);

                                    final Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                    SecretKey originalKey = new SecretKeySpec(key, "AES");
                                    c.init(Cipher.ENCRYPT_MODE, originalKey, ivspec);
                                    CipherOutputStream os = new CipherOutputStream(socket.getOutputStream(), c);

                                    byte[] buffer = BufferPool.getOrCreateBuffer();
                                    int i;
                                    while ((i = fileInput.read(buffer)) != -1) os.write(buffer, 0, i);
                                    BufferPool.release(buffer);

                                    os.flush();
                                    os.close();
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Error: " + socket);
                    e.printStackTrace();
                } finally {
                    try { socket.close(); } catch (IOException e) {}
                    System.out.println("Server Closed: " + socket);
                }
            }
        }
    }

    private class ObjectResolutionRequestRecord {
        public final String hostname, uri, requestKey, encryptionKey;
        public ObjectResolutionRequestRecord(String hostname, String uri, String requestKey, String encryptionKey) {
            this.hostname = hostname;
            this.uri = uri;
            this.requestKey = requestKey;
            this.encryptionKey = encryptionKey;
        }
    }

    public static void main(String[] args) {
        ObjectResolver or = new ObjectResolver();
//        or.newRequest("127.0.0.1", "cl.jpg");
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