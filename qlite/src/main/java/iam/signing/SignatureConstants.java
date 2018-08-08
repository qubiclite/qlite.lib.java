package iam.signing;

enum SignatureConstants {;

    static final int KEY_SIZE = 1024;
    static final String KEY_PAIR_GENERATOR_ALGORITHM = "DSA";
    static final String SIGNATURE_ALGORITHM = "SHA1withDSA";
    static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
}