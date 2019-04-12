package io.hbt.bubblegum.core.kademlia;

import com.google.protobuf.ByteString;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaBinaryPayload.KademliaBinaryPayload;
import io.hbt.bubblegum.core.kademlia.protobuf.BgKademliaSealedPayload.KademliaSealedPayload;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
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
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
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

    private Cipher rsa;

    KeyManager(BubblegumNode node) {
        if(Configuration.ENABLE_PGP) {
            Security.addProvider(provider);
            this.node = node;
            try {
                this.reKey(false);
                this.rsa = Cipher.getInstance("RSA");
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

    KademliaSealedPayload encryptPacket(RouterNode node, byte[] payload) {
        // TODO check for nulls
        PGPPublicKey publicKey  = this.getPublicKey(node.toPGPUID());
        if(publicKey != null) {
            KademliaSealedPayload.Builder sealed = KademliaSealedPayload.newBuilder();

            long start = System.currentTimeMillis();
            byte[] key_a = AES.generateKey();
            byte[] key_b = AES.generateKey();

            byte[] cipher = AES.encrypt(payload, key_a);
            cipher = AES.encrypt(cipher, key_b);

            byte[] inner = this.encryptWithPrivate(key_a);
            byte[] outer = this.encryptWithPublic(publicKey, key_b);

            sealed.setKeyA(ByteString.copyFrom(outer));
            sealed.setKeyA(ByteString.copyFrom(inner));
            sealed.setData(ByteString.copyFrom(cipher));
            System.out.println("Packet encryption overhead: " + (System.currentTimeMillis() - start) + "ms");

            return sealed.build();
        }
        return null;
    }

    byte[] decryptPacket(RouterNode node, KademliaSealedPayload sealed) {
        // Remove outer layer and inner one as well
        // TODO check for nulls
        PGPPublicKey publicKey  = this.getPublicKey(node.toPGPUID());
        if(publicKey != null) {

            long start = System.currentTimeMillis();
            byte[] unwrapOuter = this.decryptWithPrivate(sealed.getKeyA().toByteArray());
            byte[] unwrapInner = this.decryptWithPublic(publicKey, sealed.getKeyB().toByteArray());

            byte[] unwrapCipher = AES.decrypt(sealed.getData().toByteArray(), unwrapOuter);
            unwrapCipher = AES.decrypt(unwrapCipher, unwrapInner);
            System.out.println("Packet decryption overhead: " + (System.currentTimeMillis() - start) + "ms");

            return unwrapCipher;
        }
        return null;
    }

    byte[] crypt(Key k, int mode, byte[] data) {
        try {
            if (rsa != null) {
                synchronized (rsa) {
                    rsa.init(mode, k);
                    return rsa.doFinal(data);
                }
            }
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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    byte[] decryptWithPrivate(byte[] data) {
        return crypt(this.keyPair.getPrivate(), Cipher.DECRYPT_MODE, data);
    }

    byte[] encryptWithPublic(PGPPublicKey key, byte[] data) {
        try {
            PublicKey pk = new JcaPGPKeyConverter().getPublicKey(key);
            return crypt(pk, Cipher.ENCRYPT_MODE, data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    byte[] encryptWithPrivate(byte[] data) {
        return crypt(this.keyPair.getPrivate(), Cipher.ENCRYPT_MODE, data);
    }

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
        static SecureRandom random = new SecureRandom();
        static byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        static IvParameterSpec ivspec = new IvParameterSpec(iv);

        public static byte[] generateKey() {
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            return bytes;
        }

        public static byte[] encrypt (byte[] plainText, byte[] key) {
            try {
                Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKey originalKey = new SecretKeySpec(key, "AES");
                aes.init(Cipher.ENCRYPT_MODE, originalKey, ivspec);
                return aes.doFinal(plainText);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static byte[] decrypt (byte[] cipherText, byte[] key) {
            try {
                Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKey originalKey = new SecretKeySpec(key, 0, key.length, "AES");
                aes.init(Cipher.DECRYPT_MODE, originalKey, ivspec);
                return aes.doFinal(cipherText);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static void main(String[] args) {
        KeyManager km = new KeyManager(null);


        System.out.println("Starting");
        StringBuilder sb = new StringBuilder();
        long a, b;
        for(int j = 0; j < 1000; j++) {
            for (int i = 0; i < 100; i++) sb.append("a");

//            a = System.currentTimeMillis();
//            byte[] result = km.encryptWithPublic(km.pgpKey.getPublicKey(), sb.toString().getBytes());
//            b = System.currentTimeMillis();
//            String s = new String(Base64.getEncoder().encode(result));

            // Encrypt packet
            a = System.currentTimeMillis();
            byte[] key_a = AES.generateKey();
            byte[] key_b = AES.generateKey();

            String plaintext = sb.toString();
            byte[] cipher = AES.encrypt(plaintext.getBytes(), key_a);
            cipher = AES.encrypt(cipher, key_b);

            byte[] inner = km.encryptWithPrivate(key_a);
            byte[] outer = km.encryptWithPublic(km.pgpKey.getPublicKey(), key_b);
            b = System.currentTimeMillis();
            System.out.println((b-a) + "ms");

            // Decrypt packet phase
            a = System.currentTimeMillis();
            byte[] unwrapOuter = km.decryptWithPrivate(outer);
            byte[] unwrapInner = km.decryptWithPublic(km.pgpKey.getPublicKey(), inner);

            byte[] unwrapCipher = AES.decrypt(cipher, unwrapOuter);
            unwrapCipher = AES.decrypt(unwrapCipher, unwrapInner);
            b = System.currentTimeMillis();
            System.out.println((b-a) + "ms");

            System.out.println(new String(unwrapCipher));




            System.out.println("Plaintext: " + plaintext.length() + " bytes \n\n");
        }
    }

}
