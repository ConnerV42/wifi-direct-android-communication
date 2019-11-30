package com.breeze.encryption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.*;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.breeze.datatypes.BrzMessage;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

public final class BrzEncryption
{
    public static final String DEFAULT_DEVICE_KEYPAIR_NAME = "MY_BREEZE_KEY";


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
                                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP).
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

    public static byte[] signWithPrivateKey(byte[] data) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, InvalidKeyException, SignatureException, UnrecoverableEntryException {
        /*
         * Use a PrivateKey in the KeyStore to create a signature over
         * some data.
         */
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        KeyStore.Entry entry = ks.getEntry("MY_KEY", null);
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Log.w("TAG", "Not an instance of a PrivateKeyEntry");
            Void o = null;
        }
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
        s.update(data);
        byte[] signature = s.sign();
        return signature;
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
            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
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
            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
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

}
