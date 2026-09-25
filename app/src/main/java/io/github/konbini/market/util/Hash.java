package io.github.konbini.market.util;

import java.security.MessageDigest;

public class Hash {
    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(s.getBytes("UTF-8"));
            byte[] b = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<b.length;i++){
                String h = Integer.toHexString(b[i] & 0xff);
                if (h.length()==1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
