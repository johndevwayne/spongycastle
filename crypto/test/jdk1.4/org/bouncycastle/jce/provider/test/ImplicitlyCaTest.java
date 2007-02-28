package org.bouncycastle.jce.provider.test;

import org.bouncycastle.util.test.SimpleTest;
import org.bouncycastle.util.test.FixedSecureRandom;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.jce.interfaces.ConfigurableProvider;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.security.SecureRandom;
import java.security.Signature;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.Security;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.math.BigInteger;
import java.io.IOException;
import java.io.ByteArrayInputStream;

public class ImplicitlyCaTest
    extends SimpleTest
{
    byte[] k1 = Hex.decode("d5014e4b60ef2ba8b6211b4062ba3224e0427dd3");
    byte[] k2 = Hex.decode("345e8d05c075c3a508df729a1685690e68fcfb8c8117847e89063bca1f85d968fd281540b6e13bd1af989a1fbf17e06462bf511f9d0b140fb48ac1b1baa5bded");

    SecureRandom random = new FixedSecureRandom(new byte[][] { k1, k2 });

    public void performTest()
        throws Exception
    {

        KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "BC");

        ECCurve curve = new ECCurve.Fp(
            new BigInteger("883423532389192164791648750360308885314476597252960362792450860609699839"), // q
            new BigInteger("7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc", 16), // a
            new BigInteger("6b016c3bdcf18941d0d654921475ca71a9db2fb27d1d37796185c2942c0a", 16)); // b

        ECParameterSpec ecSpec = new ECParameterSpec(
            curve,
            curve.decodePoint(Hex.decode("020ffa963cdca8816ccc33b8642bedf905c3d358573d3f27fbbd3b3cb9aaaf")), // G
            new BigInteger("883423532389192164791648750360308884807550341691627752275345424702807307")); // n

        ConfigurableProvider config = (ConfigurableProvider)Security.getProvider("BC");

        config.setParameter(ConfigurableProvider.EC_IMPLICITLY_CA, ecSpec);

        g.initialize(null, new SecureRandom());

        KeyPair p = g.generateKeyPair();

        ECPrivateKey sKey = (ECPrivateKey)p.getPrivate();
        ECPublicKey vKey = (ECPublicKey)p.getPublic();

        testECDSA(sKey, vKey);

        testBCParamsAndQ(sKey, vKey);

        testEncoding(sKey, vKey);

        testKeyFactory();
    }

    private void testKeyFactory()
        throws Exception
    {
        KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", "BC");

        ECCurve curve = new ECCurve.Fp(
            new BigInteger("883423532389192164791648750360308885314476597252960362792450860609699839"), // q
            new BigInteger("7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc", 16), // a
            new BigInteger("6b016c3bdcf18941d0d654921475ca71a9db2fb27d1d37796185c2942c0a", 16)); // b

        ECParameterSpec ecSpec = new ECParameterSpec(
            curve,
            curve.decodePoint(Hex.decode("020ffa963cdca8816ccc33b8642bedf905c3d358573d3f27fbbd3b3cb9aaaf")), // G
            new BigInteger("883423532389192164791648750360308884807550341691627752275345424702807307")); // n

        ConfigurableProvider config = (ConfigurableProvider)Security.getProvider("BC");

        config.setParameter(ConfigurableProvider.EC_IMPLICITLY_CA, ecSpec);

        g.initialize(null, new SecureRandom());

        KeyPair p = g.generateKeyPair();

        ECPrivateKey sKey = (ECPrivateKey)p.getPrivate();
        ECPublicKey vKey = (ECPublicKey)p.getPublic();

        KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");

        vKey = (ECPublicKey)fact.generatePublic(new ECPublicKeySpec(vKey.getQ(), null));
        sKey = (ECPrivateKey)fact.generatePrivate(new ECPrivateKeySpec(sKey.getD(), null));
                        
        testECDSA(sKey, vKey);

        testBCParamsAndQ(sKey, vKey);

        testEncoding(sKey, vKey);
    }

    private void testECDSA(
        ECPrivateKey sKey,
        ECPublicKey vKey)
        throws Exception
    {
        byte[]           data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        Signature        s = Signature.getInstance("ECDSA", "BC");

        s.initSign(sKey);

        s.update(data);

        byte[] sigBytes = s.sign();

        s = Signature.getInstance("ECDSA", "BC");

        s.initVerify(vKey);

        s.update(data);

        if (!s.verify(sigBytes))
        {
            fail("ECDSA verification failed");
        }
    }

    private void testEncoding(
        ECPrivateKey sKey,
        ECPublicKey vKey)
        throws Exception
    {
        KeyFactory kFact = KeyFactory.getInstance("ECDSA", "BC");

        byte[] bytes = sKey.getEncoded();

        PrivateKeyInfo sInfo = PrivateKeyInfo.getInstance(new ASN1InputStream(bytes).readObject());
        
        if (!sInfo.getAlgorithmId().getParameters().equals(DERNull.INSTANCE))
        {
            fail("private key parameters wrong");
        }

        sKey = (ECPrivateKey)kFact.generatePrivate(new PKCS8EncodedKeySpec(bytes));

        bytes = vKey.getEncoded();

        SubjectPublicKeyInfo vInfo = SubjectPublicKeyInfo.getInstance(new ASN1InputStream(bytes).readObject());

        if (!vInfo.getAlgorithmId().getParameters().equals(DERNull.INSTANCE))
        {
            fail("public key parameters wrong");
        }
        
        vKey = (ECPublicKey)kFact.generatePublic(new X509EncodedKeySpec(bytes));

        testBCParamsAndQ(sKey, vKey);

        testECDSA(sKey, vKey);
    }

    private void testBCParamsAndQ(
        ECPrivateKey sKey,
        ECPublicKey vKey)
    {
        if (sKey.getParameters() != null)
        {
            fail("parameters exposed in private key");
        }

        if (vKey.getParameters() != null)
        {
            fail("parameters exposed in public key");
        }

        if (vKey.getQ().getCurve() != null)
        {
            fail("curve exposed in public point");
        }
    }

    public String getName()
    {
        return "ImplicitlyCA";
    }

    public static void main(
        String[]    args)
    {
        Security.addProvider(new BouncyCastleProvider());

        runTest(new ImplicitlyCaTest());
    }
}
