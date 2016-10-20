package com.example.aymen.mytaplinx;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Date;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.example.aymen.mytaplinx.SampleAppKeys.EnumKeyType;

/**
 * Created by NXP on 6/28/2016.
 * SpongyCastleKeystoreHelper class is used to securely store and retreive cryptographic keys. The Provider that is used is Bouncy Castle for Android; also known as Spongy Castle.
 * Please note that the Spongy Castle Provider is still susceptible to Intercepting Root-Attacker.
 * As per a study Bouncy Castle for Android(Spongy Castle) was deemed more secure than the Android Keystore API to securely store cryptographic keys.
 * Please refer the following link:  https://www.cs.ru.nl/E.Poll/papers/AndroidSecureStorage.pdf
 */
public class SpongyCastleKeystoreHelper {




    private Context mContext;

    private String mAppDirectoryPath;

    //This Salt is used to encrypt the Spongy castle file based keystores. Some developers prefer the salt to be generated by User input. Some generate a salt
    //that is hard to crack.
    private String mSalt;

    /**
     * Tag for logging.
     */
    private static final String TAG = SpongyCastleKeystoreHelper.class.getName();

    private static final String PREFS_NAME = "keytore_prefs";

    private static final String RANDOM_ID = "random_id";


    /**
     * The Spongy Castle Provider needs to be inserted as a provider in list of providers.
     */
    public static  void initProvider(){
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    /**
     * Public Constructor.
     * @param context
     */
    public SpongyCastleKeystoreHelper(Context context){
        mContext = context;
        mAppDirectoryPath = mContext.getFilesDir().getAbsolutePath();
        //This Salt is used to encrpyt the Bouncy castle file based keystores. Some developers prefer the salt to be generated by User input. Some generate a salt
        //that is hard to crack.
        mSalt = getDeviceUniqueDigest();
    }



    /**
     * Stored the Key securely to the Keystore.
     * @param key
     * @param alias
     * @param keyType
     * @throws NullPointerException
     */
    public void storeKey(final byte[] key, final String alias,final EnumKeyType keyType) throws NullPointerException{
        if(key == null)
            throw  new NullPointerException("Parameter key should not be null.");

        if(alias == null)
            throw  new NullPointerException("Parameter alias should not be null.");

        if(keyType == null)
            throw  new NullPointerException("Parameter keyType should not be null.");

        switch (keyType){

            case EnumAESKey:
                storeToKeystoreFile(key,alias,keyType, "AES");
                break;

            case EnumDESKey:
                storeToKeystoreFile(key,alias,keyType, "DESede");
                break;

            case EnumMifareKey: {
                //Mifare Keys are not supported by Bouncy Castle, hence we encrypt them using Assymmetric key Algorithm and store them in shared preferences.
                storeMifareKey(key,alias);
                break;
            }


            default:
                break;
        }
    }


    /**
     * Return instance of key that is stored in the keystore.
     * @param alias
     * @return Key
     */
    public Key getKey(final String alias){
        if(alias == null)
            throw  new NullPointerException("Parameter alias should not be null.");

        try {
            KeyStore ks = KeyStore.getInstance(getKeystoreType(), getKeystoreProviderName());
            if (ks != null) {

                File file = getKeystoreFileHandle(alias);
                boolean isFileExists = file.exists();
                if (isFileExists) {
                    ks.load(new FileInputStream(file), null);
                    return (SecretKey) ks.getKey(alias, mSalt.toCharArray());
                }
            }
        }catch (Exception e){
            throw  new RuntimeException(e);
        }
        return null;
    }




    /**
     * Stores the Keys to the keystore fle.
     * @param key
     * @param alias
     * @param keyType
     * @param algorithmType
     */
    private void storeToKeystoreFile(final byte[] key,final String alias, final EnumKeyType keyType, final String algorithmType){

        if(keyType == EnumKeyType.EnumMifareKey)  //Bouncy castle does not support custom MIFARE keys.
            throw new RuntimeException("MIFARE keys cannot be stored using Bouncy castle provider.");

        try {
            File keystoreFile = getKeystoreFileHandle(alias);
            if (keystoreFile.exists())
                return;

            keystoreFile.createNewFile();
            KeyStore keystore = KeyStore.getInstance(getKeystoreType(), getKeystoreProviderName());
            keystore.load(null);

            SecretKeySpec secretKey = new SecretKeySpec(key, algorithmType);
            keystore.setKeyEntry(alias, secretKey, mSalt.toCharArray(), null);
            keystore.store(new FileOutputStream(keystoreFile), mSalt.toCharArray());
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Mifare Keys are not supported by Bouncy Castle, hence we encrypt them using Assymmetric key Algorithm and store it in shared preferences.
     * @param key
     * @param alias
     */
    private void storeMifareKey(final byte[] key, final  String alias) {
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String encryptedKey = prefs.getString(alias, null);
            if (encryptedKey != null)
                return;

            KeyPair keyPair = generateKeyPair();
            File keystoreFile = getKeystoreFileHandle(alias);
            if (keystoreFile.exists())
                return;

            keystoreFile.createNewFile();

            KeyStore keystore = KeyStore.getInstance(getKeystoreType(), getKeystoreProviderName());
            keystore.load(null);


            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            Certificate[] certificateArr = new Certificate[1];
            certificateArr[0] = getCertificate(privateKey, publicKey);

            keystore.setKeyEntry(alias, privateKey, mSalt.toCharArray(), certificateArr);
            keystore.store(new FileOutputStream(keystoreFile), mSalt.toCharArray());

            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", getKeystoreProviderName());
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = rsaCipher.doFinal(key);

            String encodedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
            prefs.edit().putString(alias, encodedString).commit();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns MIFARE Key stored in the Keystore.
     * @param alias
     * @return Key
     */
    public byte[] getMifareKey(final String alias){
        try{
            SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String encryptedKey = prefs.getString(alias, null);
            if (encryptedKey != null){
                byte[] cipherBytes = Base64.decode(encryptedKey, Base64.DEFAULT);
                PrivateKey privateKey = null;

                KeyStore ks = KeyStore.getInstance(getKeystoreType(), getKeystoreProviderName());
                if (ks != null) {
                    File file = getKeystoreFileHandle(alias);
                    boolean isFileExists = file.exists();
                    if (isFileExists) {
                        ks.load(new FileInputStream(file), null);
                        privateKey = (PrivateKey) ks.getKey(alias, mSalt.toCharArray());

                        if(privateKey != null){
                            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", getKeystoreProviderName());
                            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
                            return rsaCipher.doFinal(cipherBytes);
                        }//if
                    }//if
                }//if
            }//if
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Generates Asymmetric Key-Pair.
     * @return KeyPair
     */
    private  KeyPair generateKeyPair() {
        try {
            SecureRandom random = new SecureRandom();
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", getKeystoreProviderName());
            generator.initialize(spec, random);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param privateKey
     * @param publicKey
     * @return Certificate
     */
    private Certificate getCertificate(PrivateKey privateKey, PublicKey publicKey){

        try {
            //Serial Number
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            BigInteger serialNumber = BigInteger.valueOf(Math.abs(random.nextInt()));

            //Validity
            Date startDate = new Date(System.currentTimeMillis());
            Date expiryDate = new Date(System.currentTimeMillis() + (((1000L*60*60*24*30))*12)*3);

            X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
            nameBuilder.addRDN(BCStyle.CN,"NXP");
            nameBuilder.addRDN(BCStyle.O, "NXP");
            nameBuilder.addRDN(BCStyle.OU, "SMR");
            nameBuilder.addRDN(BCStyle.C, "IN");
            nameBuilder.addRDN(BCStyle.L, "Bangalore");

            X500Name issuer = nameBuilder.build();
            X500Name subject = issuer;


            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, serialNumber, startDate, expiryDate, subject, SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));
            JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256withRSA");
            ContentSigner signer = builder.build(privateKey);

            byte[] certBytes = certBuilder.build(signer).getEncoded();
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate)certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
            return certificate;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the File Handle to the Keystore file.
     * @param alias
     * @return File
     * @throws NullPointerException
     */
    private File getKeystoreFileHandle(final String alias) throws  NullPointerException{
        if(alias == null)
            throw new NullPointerException("Parameter alias should not be null.");

        String filePath = mAppDirectoryPath + File.separator + alias;
        File keystoreFile = new File(filePath);
        return keystoreFile;
    }


    /**
     * Returns Keystore Type. For bouncy castle it should be "BKS"
     * @return String
     */
    private String getKeystoreType(){
        return "BKS";
    }

    /**
     * Returns Keystore provider name.
     * @return String
     */
    private String getKeystoreProviderName(){
        return "SC";
    }


    /**
     * Generates a Digest string that is unique to the device.
     * @return String
     */
    private String getDeviceUniqueDigest(){

        String salt = "";
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String randomUid = prefs.getString(RANDOM_ID, null);
        if(randomUid == null){
            randomUid = UUID.randomUUID().toString();
            prefs.edit().putString(RANDOM_ID, randomUid).commit();
        }

        String secureId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        String pseudoId = getUniquePsuedoID();

        if(randomUid != null)
            salt += randomUid;
        if(secureId != null)
            salt += secureId;
        if(pseudoId != null)
            salt += pseudoId;

        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        m.update(salt.getBytes(), 0, salt.length());
        // get md5 bytes
        byte p_md5Data[] = m.digest();
        // create a hex string
        String m_szUniqueID = new String();
        for (int i=0;i<p_md5Data.length;i++) {
            int b =  (0xFF & p_md5Data[i]);
            // if it is a single digit, make sure it have 0 in front (proper padding)
            if (b <= 0xF) m_szUniqueID+="0";
            // add number to string
            m_szUniqueID+=Integer.toHexString(b);
        }
        // hex string to uppercase
        m_szUniqueID = m_szUniqueID.toUpperCase();

        return m_szUniqueID;
    }

    /**
     * Return pseudo unique ID
     * @return ID
     */
    private  String getUniquePsuedoID() {
        // If all else fails, if the user does have lower than API 9 (lower
        // than Gingerbread), has reset their device or 'Secure.ANDROID_ID'
        // returns 'null', then simply the ID returned will be solely based
        // off their Android device information. This is where the collisions
        // can happen.
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10)  + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their device, there will be a duplicate entry
        String serial = null;
        try {
            serial = Build.class.getField("SERIAL").get(null).toString();

            // Go ahead and return the serial for api => 9
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception exception) {
            // String needs to be initialized
            serial = "serial"; // some value
        }

        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

}
