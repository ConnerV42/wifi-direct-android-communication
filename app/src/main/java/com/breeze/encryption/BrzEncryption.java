package com.breeze.encryption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

public final class BrzEncryption
{
    public static final String DEFAULT_DEVICE_KEYPAIR_NAME = "MY_BREEZE_KEY";
    public static final String DEFAULT_ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
    public static final String DEFAULT_CIPHER_INSTANCE_SETTING = "RSA/ECB/PKCS1Padding";

    /**
     * @param chatToEncrypt the chat we're setting the public and private keys of
     * @return a BrzChat with a public and private key for security
     */
    public static BrzChat encryptBrzChat(BrzChat chatToEncrypt){
        String id = chatToEncrypt.id;
        try {
            KeyPair kp = BrzEncryption.generateAndSaveKeyPair(id);
            PublicKey chatPub = kp.getPublic();
            PrivateKey chatPriv = kp.getPrivate();
            chatToEncrypt.setPublicKey(BrzEncryption.getPublicKeyAsString(chatPub));
            chatToEncrypt.setPrivateKey(BrzEncryption.getPrivateKeyAsString(chatPriv));
            return chatToEncrypt;
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    public static boolean addBrzChatKeys(BrzChat encryptedChat){
        return false;
    }
    public static boolean addChatKeysToKeyStore(PublicKey pubKey, PrivateKey privKey, String alias){
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException kse) {
            kse.printStackTrace();
            return false;
        }
        if (ks == null) {
            throw new RuntimeException("Bad keystore object, cannot decrypt BrzMessage");
        } else {
            try {
                ks.load(null);
                ks.setKeyEntry(alias, pubKey, null, null);
                ks.setKeyEntry(alias, privKey, null, null);
            } catch (CertificateException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return false;
            } catch (KeyStoreException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * @param key the public key to be converted to a string
     * @return a string representation of the public key
     */
    public static String getPublicKeyAsString(PublicKey key)
    {
        try{
            KeyFactory factory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec spec = factory.getKeySpec(key, X509EncodedKeySpec.class);
            return Base64.encodeToString(spec.getEncoded(), Base64.DEFAULT);
        } catch(NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param key the private key to be used to create a string
     * @return A string representation of the private key
     */
    public static String getPrivateKeyAsString(PrivateKey key)
    {
        try {
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec spec = fact.getKeySpec(key,
                    PKCS8EncodedKeySpec.class);
            return Base64.encodeToString(spec.getEncoded(), Base64.DEFAULT);
        } catch(NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * @param pubKeyString the string to be used to build a public key object
     * @return A PublicKey object created from the string
     */
    public static PublicKey getPublicKeyFromString(String pubKeyString)
    {
        try{
            byte [] data = Base64.decode(pubKeyString, Base64.DEFAULT);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePublic(spec);
        } catch(InvalidKeySpecException | NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param privateKeyString the string the private key object is built from
     * @return a private key object created from the string passed in.
     */
    public static PrivateKey getPrivateKeyFromString(String privateKeyString)
    {
        try {
            byte[] clear = Base64.decode(privateKeyString, Base64.DEFAULT);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(clear);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PrivateKey ret = fact.generatePrivate(spec);
            Arrays.fill(clear, (byte) 0);
            return ret;
        }catch(InvalidKeySpecException | NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * @param alias the alias of the keypair to check the keystore for
     * @return true if the keypair is in the store, false if not
     */
    public static boolean storeContainsKey(String alias) {
        try{
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    "AndroidKeyStore"
            );
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            return ks.containsAlias(alias);
        }
        catch (NoSuchAlgorithmException | CertificateException | IOException |
                KeyStoreException | NoSuchProviderException e ) {
            Log.i("KeyPair", "This device's Key Pair unable to be generated");
            return false;
        }
    }

    public static KeyPair generateAndSaveKeyPair(String alias) throws Exception
    {

        if(alias == null || alias.isEmpty() || alias.length() > 100)
        {
            throw new IllegalArgumentException("Bad alias parameter for the keystore");
        }
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    "AndroidKeyStore"
            );
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if(ks.containsAlias(alias))
            {
                throw new RuntimeException("Cannot create new key with alias" + alias + ": it already exists");
            }
            else {
                KeyGenParameterSpec.Builder builder =
                        new KeyGenParameterSpec.Builder(
                                alias,
                                KeyProperties.PURPOSE_DECRYPT).
                                setKeySize(1024).
                                setEncryptionPaddings(BrzEncryption.DEFAULT_ENCRYPTION_PADDING).
                                setDigests(KeyProperties.DIGEST_SHA256);

                keyPairGenerator.initialize(builder.build());
                return keyPairGenerator.generateKeyPair();
            }
        } catch (NoSuchAlgorithmException | CertificateException | IOException |
                InvalidAlgorithmParameterException | KeyStoreException | NoSuchProviderException e ) {
            Log.i("KeyPair", "This device's Key Pair unable to be generated");
        }
        throw new KeyStoreException("This device's Key Pair unable to be generated");
    }

    public static Enumeration<String>  listKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        return ks.aliases();
    }

    /**
     *
     * @param pubkey {PublicKey} The public key to sign the message with
     * @param message {BrzMessage} The message that's being encrypted with a message
     * @return {BrzMessage} A message with its body encrypted by the given public key
     */

    public static BrzMessage encryptMessageBody(PublicKey pubkey, BrzMessage message)
    {
        if(pubkey == null ||message.body.isEmpty() || message == null)
        {
            throw new IllegalArgumentException("Bad public key or message to encrypt");
        }
        try
        {
            Cipher inCipher = Cipher.getInstance(BrzEncryption.DEFAULT_CIPHER_INSTANCE_SETTING);
            inCipher.init(Cipher.ENCRYPT_MODE, pubkey);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outStream, inCipher);
            cipherOutputStream.write(android.util.Base64.decode(message.body, Base64.DEFAULT));
            cipherOutputStream.close();
            byte [] vals = outStream.toByteArray();
            message.body = android.util.Base64.encodeToString(vals, Base64.DEFAULT);
            return message;
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param privateKey the key to decrypt the message with
     * @param message the BrzMessage with the encrypted message body
     * @return the BrzMessage object passed in, but with a decrypted body
     */
    public static BrzMessage decryptMessageBody(PrivateKey privateKey, BrzMessage message) {
        if (privateKey == null ||  message.body.isEmpty() || message == null) {
            throw new IllegalArgumentException("Bad public key or message to encrypt");
        }
        try {
            Cipher inCipher = Cipher.getInstance(BrzEncryption.DEFAULT_CIPHER_INSTANCE_SETTING);
            inCipher.init(Cipher.DECRYPT_MODE, privateKey);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outStream, inCipher);
            cipherOutputStream.write(android.util.Base64.decode(message.body, Base64.DEFAULT));
            cipherOutputStream.close();
            byte[] vals = outStream.toByteArray();
            message.body = android.util.Base64.encodeToString(vals, Base64.DEFAULT);
            return message;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param privateKeyAlias The alias that the private key is referenced by in the key store
     * @return The private key referenced by alias that's stored in the device's KeyStore
     */

    public static PrivateKey getPrivateKeyFromStore(String privateKeyAlias) {
        if (privateKeyAlias == null || privateKeyAlias.isEmpty()) {
            throw new IllegalArgumentException("Bad private key alias");
        }
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException kse) {
            kse.printStackTrace();
            return null;
        }
        if (ks == null) {
            throw new RuntimeException("Bad keystore object, cannot decrypt BrzMessage");
        } else {
            try {
                if (!ks.containsAlias(privateKeyAlias)) {
                    throw new RuntimeException("Bad keystore alias, cannot find private key with alias: " + privateKeyAlias);
                }
                ks.load(null);
                KeyStore.Entry entry = ks.getEntry(privateKeyAlias, null);
                if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                    Log.w("Bad privateKeyEntry", "Not an instance of a PrivateKeyEntry");
                    return null;
                } else {
                    return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
                }

            } catch (CertificateException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            } catch (UnrecoverableEntryException e) {
                e.printStackTrace();
                return null;
            } catch (KeyStoreException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     *
     * @param alias the alias of the keypair containing the public key in the keystore
     * @return the public key object from the store
     */

    public static PublicKey getPublicKeyFromKeyStore(String alias)
    {
        if (alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException("Bad public key alias");
        }
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException kse) {
            kse.printStackTrace();
            return null;
        }
        if (ks == null) {
            throw new RuntimeException("Bad keystore object, cannot decrypt BrzMessage");
        } else {
            try {
                if (!ks.containsAlias(alias)) {
                    throw new RuntimeException("Bad keystore alias, cannot find private key with alias: " + alias);
                }
                ks.load(null);
                return ks.getCertificate(alias).getPublicKey();
            } catch (CertificateException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            } catch (KeyStoreException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}
