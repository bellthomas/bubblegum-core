package io.hbt.bubblegum.core.kademlia;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.Pair;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
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
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;


class KeyManager {

    private boolean initialised = false;
    private String keyServerSubmitURL = Configuration.OPENPGP_KEY_SERVER_URL + "/pks/add"; // POST 'keytext'
    private String keyServerRetrieveURL = Configuration.OPENPGP_KEY_SERVER_URL + "/pks/lookup?options=mr&op=get&search=";

    private PGPSecretKey pgpKey;

    private BubblegumNode node;
    private HashMap<String, Pair<PGPPublicKey, Long>> keyCache = new HashMap<>();

    KeyManager(BubblegumNode node) {
        if(Configuration.ENABLE_PGP) {
            Security.addProvider(new BouncyCastleProvider());
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

            String uid = this.node.toPGPUID();
            PGPSecretKey key = this.generateKey(kp, uid, this.node.getIdentifier().toCharArray());
            String keyID = Long.toHexString(key.getPublicKey().getKeyID());
            System.out.println("Generated PGP Key: 0x" + keyID);

            // Convert PGP public key to String
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ArmoredOutputStream aos = new ArmoredOutputStream(baos);
            key.getPublicKey().encode(aos);
            aos.close();
            publishKey(baos.toString());

            if (verifyKey) {
                try {
                    int retries = 0;
                    while (retries < 15 && !verifyKey("0x" + keyID, uid)) {
                        Thread.sleep(2000);
                        retries++;
                    }

                    if (retries < 15) {
                        this.pgpKey = key;
                        this.node.getDatabase().saveUserMeta(this.node, "pgp", "0x" + keyID);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                this.pgpKey = key;
                this.node.getDatabase().saveUserMeta(this.node, "pgp", "0x" + keyID);
            }
        }
    }

    private byte[] decrypt(RouterNode node, String pgpID, byte[] payload) {
        // Remove outer layer and inner one as well
        return null;
    }

    private PGPPublicKey getPublicKey(String id) {
        if(id.equals(this.node.toPGPUID())) {
            return this.pgpKey.getPublicKey();
        }

        if(this.keyCache.containsKey(id)) {
            if(System.currentTimeMillis() - this.keyCache.get(id).getSecond() < Configuration.KEY_CACHE_EXPIRY) {
                return this.keyCache.get(id).getFirst();
            }
        }

        // We don't have it, so request it and drop current packet.
        this.requestKey(id);
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
            new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.CAST5, sha1Calc).setProvider("BC").build(passphrase)
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
}
