package qlvm.functions.string;

import qlvm.QLVM;
import qlvm.functions.Function;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class FunctionHash extends Function {

    private static final MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public String getName() { return "hash"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String orig = par[0].substring(1, par[0].length()-1);
        String hash = hash(orig);
        return "'"+hash+"'";
    }

    private static String hash(String orig) {
        byte[] bytes = digest.digest(orig.getBytes(StandardCharsets.UTF_8));
        return convertBytesToHexString(bytes);
    }

    private static String convertBytesToHexString(byte[] bytes) {
        StringBuilder hexBuffer = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            hexBuffer.append(hex.length() == 1 ? 0 : hex);
        }
        return hexBuffer.toString();
    }
}