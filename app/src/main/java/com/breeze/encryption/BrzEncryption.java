package com.breeze.encryption;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.*;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import javax.crypto.Cipher;

public class BrzEncryption
{
    public static KeyPair generateAndSaveKeyPair() throws Exception
    {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    "AndroidKeyStore"
            );
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyGenParameterSpec.Builder builder =
                    new KeyGenParameterSpec.Builder(
                            "MY_KEY",
                            KeyProperties.PURPOSE_DECRYPT).
                            setKeySize(1024).
                            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP).
                            setDigests(KeyProperties.DIGEST_SHA256);

            keyPairGenerator.initialize(builder.build());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | CertificateException | IOException |
                InvalidAlgorithmParameterException | KeyStoreException | NoSuchProviderException e ) {
            Log.i("KeyPair", "Key Pair unable to be generated");
        }
        throw new KeyStoreException("Key Pair unable to be generated");
    }

    public static Enumeration<String>  listKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        return ks.aliases();
    }
   public static PublicKey grabPublicKey(KeyPair kp)
   {
       PublicKey publicKey = null;
       try {

           publicKey = kp.getPublic();

       }catch(Exception e) {
           System.out.println("Unable to generate key pair or retrieve public key. Check.");
       }
       return publicKey;
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

    public static KeyPair generateChatKeyPair(String chatId)  throws Exception {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    "AndroidKeyStore"
            );
            KeyGenParameterSpec.Builder builder =
                    new KeyGenParameterSpec.Builder(
                            chatId,
                            KeyProperties.PURPOSE_DECRYPT).
                            setKeySize(1024).
                            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP).
                            setDigests(KeyProperties.DIGEST_SHA256);

            keyPairGenerator.initialize(builder.build());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException |
                InvalidAlgorithmParameterException | NoSuchProviderException e ) {
            Log.i("KeyPair", "Key Pair unable to be generated");
        }
        throw new KeyStoreException("Key Pair unable to be generated");
    }
}
