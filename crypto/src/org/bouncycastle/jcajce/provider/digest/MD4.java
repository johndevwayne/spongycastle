package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jce.provider.JCEKeyGenerator;
import org.bouncycastle.jce.provider.JCEMac;

public class MD4
{
    /**
     * MD4 HMac
     */
    public static class HashMac
        extends JCEMac
    {
        public HashMac()
        {
            super(new HMac(new MD4Digest()));
        }
    }

    public static class KeyGenerator
        extends JCEKeyGenerator
    {
        public KeyGenerator()
        {
            super("HMACMD4", 128, new CipherKeyGenerator());
        }
    }

    static public class Digest
        extends BCMessageDigest
        implements Cloneable
    {
        public Digest()
        {
            super(new MD4Digest());
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            Digest d = (Digest)super.clone();
            d.digest = new MD4Digest((MD4Digest)digest);

            return d;
        }
    }

    public static class Mappings
        extends DigestAlgorithmProvider
    {
        private static final String PREFIX = MD4.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("MessageDigest.MD4", PREFIX + "$Digest");
            provider.addAlgorithm("Alg.Alias.MessageDigest." + PKCSObjectIdentifiers.md4, "MD4");

            addHMACAlgorithm(provider, "MD4", PREFIX + "$HashMac", PREFIX + "$KeyGenerator");
        }
    }
}