package com.breeze.encryption;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Pattern;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Base64;
import android.util.Log;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class BrzEncryption {
    private final String DEFAULT_ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;

    private final String ASYM_CIPHER = "RSA/ECB/PKCS1Padding";
    private final String SYM_CIPHER = "AES/GCM/NoPadding";

    private KeyStore ks = null;

    public BrzEncryption() {
        try {
            this.ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize keystore");
        }
    }

    // Check if alias is valid
    private boolean aliasInvalid(String alias) {
        return alias == null || alias.isEmpty() || alias.length() > 50 ||
                Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!]").matcher(alias).find();
    }

    /**
     * @param alias The key's alias
     * @return true if the deletion was successfull, false if there was an error
     */
    public boolean deleteKeyEntry(String alias) {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            ks.deleteEntry(alias);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param alias the alias of the keypair to check the keystore for
     * @return true if the keypair is in the store, false if not
     */
    public boolean storeContainsKey(String alias) {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            return ks.containsAlias(alias);
        } catch (KeyStoreException e) {
            Log.i("KeyPair", "This device's Key Pair unable to be generated");
            return false;
        }
    }


    /*
     *
     *      SYMMETRIC KEY OPERATIONS
     *
     */


    public void saveSymKey(final String alias, final String secretKey) {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            byte[] encodedKey = Base64.decode(secretKey, Base64.DEFAULT);
            SecretKey key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");

            ks.setEntry(
                    alias,
                    new KeyStore.SecretKeyEntry(key),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SecretKey generateSymmetricKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            return keyGen.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public SecretKey generateAndSaveSymKey(final String alias) {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            SecretKey secretKey = generateSymmetricKey();
            ks.setEntry(
                    alias,
                    new KeyStore.SecretKeyEntry(secretKey),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build()
            );
            return secretKey;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String symmetricEncrypt(final String alias, final String message) {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            SecretKey secretKey = (SecretKey) ks.getKey(alias, null);
            byte[] cipherMessage = symmetricEncrypt(secretKey, message.getBytes());
            return Base64.encodeToString(cipherMessage, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] symmetricEncrypt(final SecretKey secretKey, final byte[] messageBytes) {
        try {
            Cipher cipher = Cipher.getInstance(SYM_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(messageBytes);
            byte[] initialVector = cipher.getIV();

            // Create a buffer that stores the InitalVector's length, the InitalVector, then the encrypted bytes
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + initialVector.length + encryptedBytes.length);
            byteBuffer.putInt(initialVector.length);
            byteBuffer.put(initialVector);
            byteBuffer.put(encryptedBytes);
            return byteBuffer.array();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String symmetricDecrypt(final String alias, final String message) {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            SecretKey secretKey = (SecretKey) ks.getKey(alias, null);
            byte[] messageBytes = Base64.decode(message, Base64.DEFAULT);
            byte[] decryptedBytes = symmetricDecrypt(secretKey, messageBytes);
            if (decryptedBytes == null) return null;
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] symmetricDecrypt(final SecretKey secretKey, final byte[] messageBytes) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(messageBytes);
            int ivLength = byteBuffer.getInt();
            if (ivLength < 12 || ivLength >= 16) { // check input parameter
                throw new IllegalArgumentException("invalid iv length");
            }

            byte[] initialVector = new byte[ivLength];
            byteBuffer.get(initialVector);

            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            Cipher cipher = Cipher.getInstance(SYM_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, initialVector));

            return cipher.doFinal(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
     *
     *      PRIVATE KEY OPERATIONS
     *
     */

    public void generateAndSaveKeyPair(String alias) throws Exception {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    "AndroidKeyStore"
            );
            if (ks.containsAlias(alias)) {
                throw new RuntimeException("Cannot create new key with alias" + alias + ": it already exists");
            } else {
                KeyGenParameterSpec.Builder builder =
                        new KeyGenParameterSpec.Builder(
                                alias,
                                KeyProperties.PURPOSE_DECRYPT).
                                setKeySize(2048).
                                setEncryptionPaddings(DEFAULT_ENCRYPTION_PADDING).
                                setDigests(KeyProperties.DIGEST_SHA256);

                keyPairGenerator.initialize(builder.build());
            }
        } catch (NoSuchAlgorithmException |
                InvalidAlgorithmParameterException | KeyStoreException | NoSuchProviderException e) {
            Log.i("KeyPair", "This device's Key Pair unable to be generated");
            throw new KeyStoreException("This device's Key Pair unable to be generated");
        }
    }

    public String asymmetricEncrypt(String publicKey, String message) {
        try {
            Log.i("ENCRYPTION: ENCRYPT", publicKey + " " + message);

            // Set up an encryption cipher with the public key
            byte[] keyBytes = Base64.decode(publicKey, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance(ASYM_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);

            // Encrypt the message with a "one off" secret key
            SecretKey secretKey = generateSymmetricKey();
            if (secretKey == null)
                throw new RuntimeException("Could not generate 'one off' secret key");
            byte[] encryptedMessageBytes = symmetricEncrypt(secretKey, message.getBytes());
            if (encryptedMessageBytes == null)
                throw new RuntimeException("Could not encrypt message using the 'one off' secret key");

            // Encrypt the secret key using the public key
            byte[] encryptedSecretKey = cipher.doFinal(secretKey.getEncoded());

            // Create a buffer with the encrypted secret key, then the encrypted message
            // by combining asymmetric and symmetric encryption, we can encrypt much longer messages
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + encryptedSecretKey.length + encryptedMessageBytes.length);
            byteBuffer.putInt(encryptedSecretKey.length);
            byteBuffer.put(encryptedSecretKey);
            byteBuffer.put(encryptedMessageBytes);
            byte[] finalEncryptedMessage = byteBuffer.array();

            return Base64.encodeToString(finalEncryptedMessage, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String asymmetricDecrypt(String alias, String message) {
        try {

            // Create decryption cipher with private key
            PrivateKey privateKey = getPrivateKeyFromStore(alias);
            Cipher cipher = Cipher.getInstance(ASYM_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // Toss the message into a buffer
            byte[] messageBytes = Base64.decode(message, Base64.DEFAULT);
            ByteBuffer byteBuffer = ByteBuffer.wrap(messageBytes);

            // Get the encrypted secret key
            int secretLength = byteBuffer.getInt();
            byte[] encryptedSecretKey = new byte[secretLength];
            byteBuffer.get(encryptedSecretKey);

            // Get the encrypted message
            byte[] encryptedMessage = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedMessage);

            // Decrypt the secret key
            byte[] decryptedSecretKey = cipher.doFinal(encryptedSecretKey);
            SecretKey secretKey = new SecretKeySpec(decryptedSecretKey, "AES");

            // Decrypt the message
            byte[] decryptedMessage = symmetricDecrypt(secretKey, encryptedMessage);
            if (decryptedMessage == null)
                throw new RuntimeException("Could not decrypt message using 'one off' secret key");
            return new String(decryptedMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param alias The alias that the private key is referenced by in the key store
     * @return The private key referenced by alias that's stored in the device's KeyStore
     */

    public PrivateKey getPrivateKeyFromStore(String alias) {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            ks.load(null, null);
            if (!ks.containsAlias(alias)) {
                throw new RuntimeException("Bad keystore alias, cannot find private key with alias: " + alias);
            }
            KeyStore.Entry entry = ks.getEntry(alias, null);
            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                Log.w("Bad privateKeyEntry", "Not an instance of a PrivateKeyEntry");
                return null;
            } else {
                return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            }

        } catch (CertificateException | IOException | NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param alias the alias of the keypair containing the public key in the keystore
     * @return the public key object from the store
     */

    public PublicKey getPublicKeyFromKeyStore(String alias) {
        if (aliasInvalid(alias))
            throw new IllegalArgumentException("Bad alias parameter for the keystore");

        try {
            ks.load(null, null);
            if (!ks.containsAlias(alias)) {
                throw new RuntimeException("Bad keystore alias, cannot find private key with alias: " + alias);
            }
            return ks.getCertificate(alias).getPublicKey();
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
            return null;

        }
    }
}
