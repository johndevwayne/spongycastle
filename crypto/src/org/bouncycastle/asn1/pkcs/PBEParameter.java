package org.bouncycastle.asn1.pkcs;

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

public class PBEParameter
    extends ASN1Object
{
    DERInteger      iterations;
    ASN1OctetString salt;

    public PBEParameter(
        byte[]      salt,
        int         iterations)
    {
        if (salt.length != 8)
        {
            throw new IllegalArgumentException("salt length must be 8");
        }
        this.salt = new DEROctetString(salt);
        this.iterations = new DERInteger(iterations);
    }

    public PBEParameter(
        ASN1Sequence  seq)
    {
        salt = (ASN1OctetString)seq.getObjectAt(0);
        iterations = (DERInteger)seq.getObjectAt(1);
    }

    public static PBEParameter getInstance(
        Object  obj)
    {
        if (obj instanceof PBEParameter)
        {
            return (PBEParameter)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new PBEParameter((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public BigInteger getIterationCount()
    {
        return iterations.getValue();
    }

    public byte[] getSalt()
    {
        return salt.getOctets();
    }

    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(salt);
        v.add(iterations);

        return new DERSequence(v);
    }
}
