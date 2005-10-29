package org.bouncycastle.ocsp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.asn1.ocsp.ResponseData;
import org.bouncycastle.asn1.ocsp.SingleResponse;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;

/**
 * <pre>
 * BasicOCSPResponse       ::= SEQUENCE {
 *    tbsResponseData      ResponseData,
 *    signatureAlgorithm   AlgorithmIdentifier,
 *    signature            BIT STRING,
 *    certs                [0] EXPLICIT SEQUENCE OF Certificate OPTIONAL }
 * </pre>
 */
public class BasicOCSPResp
    implements java.security.cert.X509Extension
{
    BasicOCSPResponse   resp;
    ResponseData        data;
    X509Certificate[]   chain = null;

    public BasicOCSPResp(
        BasicOCSPResponse   resp)
    {
        this.resp = resp;
        this.data = resp.getTbsResponseData();
    }

    /**
     * Return the DER encoding of the tbsResponseData field.
     * @return DER encoding of tbsResponseData
     * @throws OCSPException in the event of an encoding error.
     */
    public byte[] getTBSResponseData()
        throws OCSPException
    {
        try
        {
            return resp.getTbsResponseData().getEncoded();
        }
        catch (IOException e)
        {
            throw new OCSPException("problem encoding tbsResponseData", e);
        }
    }
    
    public int getVersion()
    {
        return data.getVersion().getValue().intValue() + 1;
    }

    public RespID getResponderId()
    {
        return new RespID(data.getResponderID());
    }

    public Date getProducedAt()
    {
        SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmssz");

        return dateF.parse(data.getProducedAt().getTime(), new ParsePosition(0));
    }

    public SingleResp[] getResponses()
    {
        ASN1Sequence    s = data.getResponses();
        SingleResp[]    rs = new SingleResp[s.size()];

        for (int i = 0; i != rs.length; i++)
        {
            rs[i] = new SingleResp(SingleResponse.getInstance(s.getObjectAt(i)));
        }

        return rs;
    }

    public X509Extensions getResponseExtensions()
    {
        return data.getResponseExtensions();
    }
    
    /**
     * RFC 2650 doesn't specify any critical extensions so we return true
     * if any are encountered.
     * 
     * @return true if any critical extensions are present.
     */
    public boolean hasUnsupportedCriticalExtension()
    {
        Set extns = getCriticalExtensionOIDs();
        if (extns != null && !extns.isEmpty())
        {
            return true;
        }

        return false;
    }

    private Set getExtensionOIDs(boolean critical)
    {
        Set             set = new HashSet();
        X509Extensions  extensions = this.getResponseExtensions();
        
        if (extensions != null)
        {
            Enumeration     e = extensions.oids();
    
            while (e.hasMoreElements())
            {
                DERObjectIdentifier oid = (DERObjectIdentifier)e.nextElement();
                X509Extension       ext = extensions.getExtension(oid);
    
                if (critical == ext.isCritical())
                {
                    set.add(oid.getId());
                }
            }
        }

        return set;
    }

    public Set getCriticalExtensionOIDs()
    {
        return getExtensionOIDs(true);
    }

    public Set getNonCriticalExtensionOIDs()
    {
        return getExtensionOIDs(false);
    }

    public byte[] getExtensionValue(String oid)
    {
        X509Extensions exts = this.getResponseExtensions();

        if (exts != null)
        {
            X509Extension   ext = exts.getExtension(new DERObjectIdentifier(oid));

            if (ext != null)
            {
                ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
                DEROutputStream dOut = new DEROutputStream(bOut);

                try
                {
                    dOut.writeObject(ext.getValue());

                    return bOut.toByteArray();
                }
                catch (Exception e)
                {
                    throw new RuntimeException("error encoding " + e.toString());
                }
            }
        }

        return null;
    }
    
    public String getSignatureAlgOID()
    {
        return resp.getSignatureAlgorithm().getObjectId().getId();
    }

    /**
     * @deprecated RespData class is no longer required as all functionality is
     * available on this class.
     * @return the RespData object
     */
    public RespData getResponseData()
    {
        return new RespData(resp.getTbsResponseData());
    }

    public byte[] getSignature()
    {
        return resp.getSignature().getBytes();
    }

    private List getCertList(
        String provider) 
        throws OCSPException, NoSuchProviderException
    {
        List                    certs = new ArrayList();
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        ASN1OutputStream        aOut = new ASN1OutputStream(bOut);
        CertificateFactory      cf;

        try
        {
            cf = CertificateFactory.getInstance("X.509", provider);
        }
        catch (CertificateException ex)
        {
            throw new OCSPException("can't get certificate factory.", ex);
        }

        //
        // load the certificates and revocation lists if we have any
        //
        ASN1Sequence s = resp.getCerts();

        if (s != null)
        {
            Enumeration e = s.getObjects();

            while (e.hasMoreElements())
            {
                try
                {
                    aOut.writeObject(e.nextElement());

                    certs.add(cf.generateCertificate(
                        new ByteArrayInputStream(bOut.toByteArray())));
                }
                catch (IOException ex)
                {
                    throw new OCSPException(
                            "can't re-encode certificate!", ex);
                }
                catch (CertificateException ex)
                {
                    throw new OCSPException(
                            "can't re-encode certificate!", ex);
                }

                bOut.reset();
            }
        }
        
        return certs;
    }
    
    public X509Certificate[] getCerts(
        String  provider)
        throws OCSPException, NoSuchProviderException
    {
        List                    certs = getCertList(provider);
            
        return (X509Certificate[])certs.toArray(new X509Certificate[certs.size()]);
    }

    /**
     * Return the certificates, if any associated with the response.
     * @param type type of CertStore to create
     * @param provider provider to use
     * @return a CertStore, possibly empty
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws OCSPException
     */
    public CertStore getCertificates(
        String type,
        String provider) 
        throws NoSuchAlgorithmException, NoSuchProviderException, OCSPException
    {
        try
        {
            return CertStore.getInstance(type, 
                new CollectionCertStoreParameters(this.getCertList(provider)), provider);
        }
        catch (InvalidAlgorithmParameterException e)
        {
            throw new OCSPException("can't setup the CertStore", e);
        }
    }
    
    /**
     * verify the signature against the tbsResponseData object we contain.
     */
    public boolean verify(
        PublicKey   key,
        String      sigProvider)
        throws OCSPException, NoSuchProviderException
    {
        try
        {
            java.security.Signature signature = java.security.Signature.getInstance(this.getSignatureAlgOID(), sigProvider);

            signature.initVerify(key);

            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            DEROutputStream         dOut = new DEROutputStream(bOut);

            dOut.writeObject(resp.getTbsResponseData());

            signature.update(bOut.toByteArray());

            return signature.verify(this.getSignature());
        }
        catch (NoSuchProviderException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new OCSPException("exception processing sig: " + e, e);
        }
    }

    /**
     * return the ASN.1 encoded representation of this object.
     */
    public byte[] getEncoded()
        throws IOException
    {
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        ASN1OutputStream        aOut = new ASN1OutputStream(bOut);

        aOut.writeObject(resp);

        return bOut.toByteArray();
    }
    
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }
        
        if (!(o instanceof BasicOCSPResp))
        {
            return false;
        }
        
        BasicOCSPResp r = (BasicOCSPResp)o;
        
        return resp.equals(r.resp);
    }
    
    public int hashCode()
    {
        return resp.hashCode();
    }
}
