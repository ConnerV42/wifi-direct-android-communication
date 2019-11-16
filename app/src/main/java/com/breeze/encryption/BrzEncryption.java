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
import javax.crypto.Cipher;

public class BrzEncryption
{
    PrivateKey privateKey;
    PublicKey publicKey;


    public static KeyPair getKeyPair() throws Exception
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
            return null;
        }
    }

    public boolean createKey()
    {
        return true;
    }

    public static void listKeyStore() throws Exception
    {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);

        Enumeration<String> aliases = ks.aliases();
        System.out.println(aliases);
    }


   public PrivateKey grabPrivateKey(KeyPair kp)
   {
       try {

           privateKey = kp.getPrivate();

       }catch(Exception e) {
           System.out.println("Unable to generate key pair or retrieve private key. Check.");
       }
       return privateKey;
   }

   public PublicKey grabPublicKey(KeyPair kp)
   {
       try {

           publicKey = kp.getPublic();

       }catch(Exception e) {
           System.out.println("Unable to generate key pair or retrieve public key. Check.");
       }
       return publicKey;
   }
}
