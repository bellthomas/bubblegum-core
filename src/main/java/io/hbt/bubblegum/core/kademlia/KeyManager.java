package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;


class KeyManager {

    private boolean initialised = false;
    private String keyServerSubmitURL = Configuration.OPENPGP_KEY_SERVER_URL + "/pks/add"; // POST 'keytext'
    private String keyServerRetrieveURL = Configuration.OPENPGP_KEY_SERVER_URL + "/pks/lookup?options=mr&op=get&search=";
    private BouncyCastleProvider provider = new BouncyCastleProvider();
    private PGPSecretKey pgpKey;
    private KeyPair keyPair;

    private BubblegumNode node;
    private HashMap<String, Pair<PGPPublicKey, Long>> keyCache = new HashMap<>();

    KeyManager(BubblegumNode node) {
        if(Configuration.ENABLE_PGP) {
            Security.addProvider(provider);
            this.node = node;
            try {
                this.reKey(false);
                this.initialised = true;
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    void reKey(boolean verifyKey) throws NoSuchAlgorithmException, NoSuchProviderException, PGPException, IOException, ProtocolException {

        if(Configuration.ENABLE_PGP) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
            kpg.initialize(Configuration.RSA_KEY_LENGTH);
            KeyPair kp = kpg.generateKeyPair();
            keyPair = kp;

            String uid = "uid";//this.node.toPGPUID();
            PGPSecretKey key = this.generateKey(kp, uid, "password".toCharArray());//this.node.getIdentifier().toCharArray());
            String keyID = Long.toHexString(key.getPublicKey().getKeyID());
            System.out.println("Generated PGP Key: 0x" + keyID);

            // Convert PGP public key to String
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ArmoredOutputStream aos = new ArmoredOutputStream(baos);
            key.getPublicKey().encode(aos);
            aos.close();
//            publishKey(baos.toString());

            if (verifyKey) {
                try {
                    int retries = 0;
                    while (retries < 15 && !verifyKey("0x" + keyID, uid)) {
                        Thread.sleep(2000);
                        retries++;
                    }

                    if (retries < 15) {
                        this.pgpKey = key;
//                        this.node.getDatabase().saveUserMeta(this.node, "pgp", "0x" + keyID);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                this.pgpKey = key;
//                this.node.getDatabase().saveUserMeta(this.node, "pgp", "0x" + keyID);
            }
        }
    }

    boolean ensurePGPKeyIsLocal(String keyID, String pgpID) {
        if(!this.keyCache.containsKey(pgpID) ||
            System.currentTimeMillis() - this.keyCache.get(pgpID).getSecond() > Configuration.KEY_CACHE_EXPIRY) {

            // Not local.
            boolean local = false;
            try {
                local = this.verifyKey(keyID, pgpID);
            } catch (Exception e) {
                // Best effort.
            } finally {
                return local;
            }
        }
        return true;
    }

    byte[] decryptPacket(RouterNode node, String pgpID, byte[] payload) {
        // Remove outer layer and inner one as well
        return null;
    }


    byte[] crypt(Key k, int mode, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(mode, k);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }



    byte[] decryptWithPublic(PGPPublicKey key, byte[] data) {
        try {
            PublicKey pk = new JcaPGPKeyConverter().getPublicKey(key);
            return crypt(pk, Cipher.DECRYPT_MODE, data);

        } catch (PGPException e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] decryptWithPrivate(byte[] data) {
        try {
            PBESecretKeyDecryptor keyDec = new JcePBESecretKeyDecryptorBuilder(
                new JcaPGPDigestCalculatorProviderBuilder().setProvider("BC").build())
                .setProvider("BC").build("password".toCharArray());
            PrivateKey pk = new JcaPGPKeyConverter().getPrivateKey(this.pgpKey.extractPrivateKey(keyDec));
            return crypt(pk, Cipher.DECRYPT_MODE, data);

        } catch (PGPException e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] encryptWithPublic(PGPPublicKey key, byte[] data) {
        try {
            PublicKey pk = new JcaPGPKeyConverter().getPublicKey(key);
            return crypt(pk, Cipher.ENCRYPT_MODE, data);

        } catch (PGPException e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] encryptWithPrivate(byte[] data) {
        try {
            PBESecretKeyDecryptor keyDec = new JcePBESecretKeyDecryptorBuilder(
                new JcaPGPDigestCalculatorProviderBuilder().setProvider("BC").build())
                .setProvider("BC").build("password".toCharArray());
            PrivateKey pk = new JcaPGPKeyConverter().getPrivateKey(this.pgpKey.extractPrivateKey(keyDec));
            return crypt(pk, Cipher.ENCRYPT_MODE, data);

        } catch (PGPException e) {
            e.printStackTrace();
        }
        return null;
    }

//    byte[] decryptWithPrivate(byte[] data) {
//        try {
//            JcaPGPObjectFactory objFact = new JcaPGPObjectFactory(data);
//            PGPEncryptedDataList encList = (PGPEncryptedDataList) objFact.nextObject();
//            PGPPublicKeyEncryptedData encD = (PGPPublicKeyEncryptedData) encList.get(0);
//            PBESecretKeyDecryptor keyDec = new JcePBESecretKeyDecryptorBuilder(
//                new JcaPGPDigestCalculatorProviderBuilder().setProvider("BC").build())
//                .setProvider("BC").build("password".toCharArray());
//
//            PGPPrivateKey privateKey1 = this.pgpKey.extractPrivateKey(keyDec);
//            PublicKeyDataDecryptorFactory dec1 =
//                new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(privateKey1);
//            InputStream in1 = encD.getDataStream(dec1);
//            int ch;
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            while( (ch = in1.read()) >= 0) baos.write(ch);
//            byte[] decrypted1 = baos.toByteArray();
//            return decrypted1;
//
//        } catch (PGPException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


//    final PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
//        new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256).setWithIntegrityPacket(true)
//            .setSecureRandom(new SecureRandom())
//            .setProvider(this.provider)
//    );
//    byte[] encryptWithPublic(PGPPublicKey key, byte[] data) {
//        try {
//            final ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//            encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(key).setProvider(this.provider));
//            OutputStream encryptor = encryptedDataGenerator.open(out, data.length);
//            encryptor.write(data);
//            encryptor.close();
//            return out.toByteArray();
//
//        } catch (Exception pgpe) {
//            return null;
//        }
//    }

    PGPPublicKey getPublicKey(String id) {
        if(id.equals(this.node.toPGPUID())) {
            return this.pgpKey.getPublicKey();
        }

        if(this.keyCache.containsKey(id)) {
            if(System.currentTimeMillis() - this.keyCache.get(id).getSecond() < Configuration.KEY_CACHE_EXPIRY) {
                return this.keyCache.get(id).getFirst();
            }
        }

        return null;
    }

    private void requestKey(String id) {

    }

    private static PGPSecretKey generateKey(KeyPair pair, String identity, char[] passphrase) throws PGPException {
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        PGPKeyPair keyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, pair, new Date());
        PGPSecretKey secretKey = new PGPSecretKey(
            PGPSignature.DEFAULT_CERTIFICATION,
            keyPair,
            identity,
            sha1Calc,
            null,
            null,
            new JcaPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1),
            new JcePBESecretKeyEncryptorBuilder(Configuration.PGP_SYM_ENC_GENERATOR, sha1Calc).setProvider("BC").build(passphrase)
        );

        return secretKey;
    }

    private boolean publishKey(String keytext) throws IOException {

        URL obj = new URL(keyServerSubmitURL);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        //add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String urlParameters = "keytext="+ URLEncoder.encode(keytext, "UTF-8");

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        if(responseCode == 200) {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString().contains("added successfully");
        }

        System.out.println("Fail");
        return false;
    }

    private boolean verifyKey(String keyID, String expectedUID) throws Exception {

        String url = this.keyServerRetrieveURL + keyID;

        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = con.getResponseCode();
        if(responseCode == 200) {

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            String resp = response.toString();
            InputStream keyIS = new ByteArrayInputStream(resp.getBytes(Charset.forName("UTF-8")));

            PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(keyIS), new JcaKeyFingerprintCalculator());

            Iterator keyRingIter = pgpPub.getKeyRings();
            boolean found = false;
            while (keyRingIter.hasNext() && !found) {
                PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIter.next();
                Iterator keyIter = keyRing.getPublicKeys();
                while (keyIter.hasNext() && !found) {
                    PGPPublicKey nextKey = (PGPPublicKey) keyIter.next();
                    if (nextKey.isEncryptionKey()) {
                        Iterator<String> users = nextKey.getUserIDs();
                        while(users.hasNext() && !found) {
                            if(users.next().equals(expectedUID)) {
                                System.out.println(expectedUID);
                                this.keyCache.put(expectedUID, new Pair<>(nextKey, System.currentTimeMillis()));
                                found = true;
                            }
                        }
                    }
                }
            }

            return found;
        } else {
            return false;
        }
    }

    static class AES {
        public static void start(String plainText) throws Exception
        {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);

            // Generate Key
            SecretKey key = keyGenerator.generateKey();

            // Generating IV.
            byte[] IV = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(IV);

            System.out.println("Original Text  : "+plainText);

            byte[] cipherText = encrypt(plainText.getBytes(),key, IV);
            System.out.println("Encrypted Text : "+Base64.getEncoder().encodeToString(cipherText));

            String decryptedText = decrypt(cipherText,key, IV);
            System.out.println("DeCrypted Text : "+decryptedText);

        }

        public static byte[] encrypt (byte[] plaintext,SecretKey key,byte[] IV ) throws Exception
        {
            //Get Cipher Instance
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            //Create SecretKeySpec
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");

            //Create IvParameterSpec
            IvParameterSpec ivSpec = new IvParameterSpec(IV);

            //Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            //Perform Encryption
            byte[] cipherText = cipher.doFinal(plaintext);

            return cipherText;
        }

        public static String decrypt (byte[] cipherText, SecretKey key,byte[] IV) throws Exception
        {
            //Get Cipher Instance
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            //Create SecretKeySpec
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");

            //Create IvParameterSpec
            IvParameterSpec ivSpec = new IvParameterSpec(IV);

            //Initialize Cipher for DECRYPT_MODE
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            //Perform Decryption
            byte[] decryptedText = cipher.doFinal(cipherText);

            return new String(decryptedText);
        }
    }

    public static void main(String[] args) {
        KeyManager km = new KeyManager(null);
        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4Compressor compressor = factory.fastCompressor();

        System.out.println("Starting");

//        byte[] result = km.encryptWithPublic(km.pgpKey.getPublicKey(), "hhhhhhhhhh".getBytes());
//        String s = new String(Base64.getEncoder().encode(result));
//        System.out.println(s);
//        System.out.println(result.length);
//
//        int maxCompressedLength = compressor.maxCompressedLength(result.length);
//        byte[] compressed = new byte[maxCompressedLength];
//        int compressedLength = compressor.compress(result, 0, result.length, compressed, 0, maxCompressedLength);
//        System.out.println(compressedLength);
//
//        byte[] result2 = km.decryptWithPrivate(result);
//        System.out.println(new String(result2));

//        result = km.encryptWithPrivate("hello".getBytes());
//        s = new String(Base64.getEncoder().encode(result));
//        System.out.println(s);
//        System.out.println(result.length);
//        result2 = km.decryptWithPublic(km.pgpKey.getPublicKey(), result);
//        System.out.println(new String(result2));


//        try {
//            System.out.println(new String(km.pgpKey.));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        StringBuilder sb = new StringBuilder();
        long a, b;
        for(int j = 0; j < 10; j++) {
            for (int i = 0; i < 100; i++) sb.append("a");

            a = System.currentTimeMillis();
            byte[] result = km.encryptWithPublic(km.pgpKey.getPublicKey(), sb.toString().getBytes());
            b = System.currentTimeMillis();
            String s = new String(Base64.getEncoder().encode(result));
            System.out.println(100*(j+1) + ": " + s.length() + "  ["+(b-a)+"ms]");
        }
    }

}
