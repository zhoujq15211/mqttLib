package com.lib.mqtt;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public class SSLContextGenerator {
    //私钥密码
    private static final String PASSWORD = "201890678330787";
    private static final String KEY_TYPE = "BKS";
    private static final String ALGORITHM = "X509";

//    public static SSLContext setCard(InputStream keyStoreStream) {
//        try {
//            //https固定模式,X.509是固定的模式
//            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
//            //关联证书的对象
//            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            keyStore.load(null);
//            String certificateAlias = Integer.toString(0);
//            //核心逻辑,信任什么证书,从Assets读取拷贝进去的证书
//            keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(keyStoreStream));
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            //信任关联器
//            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            //初始化证书对象
//            trustManagerFactory.init(keyStore);
//            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom()
//            );
//            return sslContext;
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        } catch (CertificateException mE) {
//            mE.printStackTrace();
//        } catch (NoSuchAlgorithmException mE) {
//            mE.printStackTrace();
//        } catch (IOException mE) {
//            mE.printStackTrace();
//        } catch (KeyManagementException mE) {
//            mE.printStackTrace();
//        }
//        return null;
//    }


    public static SSLContext getSslContext(InputStream keyStore, InputStream clientKey) {
        SSLContext sslContext = null;
        try {
            KeyManagerFactory keyManager = null;
            TrustManagerFactory tmf = null;
            if (clientKey != null) {
                KeyStore ts = KeyStore.getInstance(KEY_TYPE);
                ts.load(clientKey, PASSWORD.toCharArray());
                tmf = TrustManagerFactory.getInstance(ALGORITHM);
                tmf.init(ts);
            }
            if (keyStore != null) {
                keyManager = KeyManagerFactory.getInstance(ALGORITHM);
                KeyStore kks = KeyStore.getInstance(KEY_TYPE);
                kks.load(keyStore, PASSWORD.toCharArray());
                keyManager.init(kks, PASSWORD.toCharArray());
            }
            sslContext = SSLContext.getInstance("TLS");
            if (keyManager != null && tmf != null) {
                sslContext.init(keyManager.getKeyManagers(), tmf.getTrustManagers(), null);
            } else if (keyManager != null) {
                sslContext.init(keyManager.getKeyManagers(), null, null);
            } else if (tmf != null) {
                sslContext.init(null, tmf.getTrustManagers(), null);
            } else {
                sslContext = null;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } finally {
            closeStream(clientKey);
        }
        return sslContext;
    }

    private static void closeStream(Closeable... cs) {
        for (Closeable c : cs) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
