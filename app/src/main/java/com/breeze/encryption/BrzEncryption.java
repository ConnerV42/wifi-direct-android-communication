package com.breeze.encryption;

import java.security.*;

public class BrzEncryption extends KeyPairGenerator {
    public static KeyPair getKeys()
    {
        KeyPairGenerator creator = BrzEncryption.getInstance("RSA");
        return creator.generateKeyPair();
    }
}
