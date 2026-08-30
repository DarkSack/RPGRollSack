package com.sack.rpgroll.sackresourcepack.distribution.s3;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwsSignatureV4Test {

    private final AwsSignatureV4 signer = new AwsSignatureV4();

    @Test
    void signProducesAuthorizationHeaderWithExpectedAlgorithmAndAccessKey() {
        AwsSignatureV4.SignedRequest signed = signer.sign("PUT", "bucket.s3.amazonaws.com", "/pack.zip",
                "hello".getBytes(StandardCharsets.UTF_8), "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");

        assertTrue(signed.authorizationHeader().startsWith("AWS4-HMAC-SHA256 Credential=AKIAEXAMPLE/"));
        assertTrue(signed.authorizationHeader().contains("/us-east-1/s3/aws4_request"));
        assertTrue(signed.authorizationHeader().contains("SignedHeaders=host;x-amz-content-sha256;x-amz-date"));
        assertTrue(signed.authorizationHeader().contains("Signature="));
    }

    @Test
    void signatureIsA64CharacterHexString() {
        AwsSignatureV4.SignedRequest signed = signer.sign("PUT", "bucket.s3.amazonaws.com", "/pack.zip",
                "hello".getBytes(StandardCharsets.UTF_8), "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");

        String signaturePrefix = "Signature=";
        int index = signed.authorizationHeader().indexOf(signaturePrefix);
        String signature = signed.authorizationHeader().substring(index + signaturePrefix.length());

        assertEquals(64, signature.length());
        assertTrue(signature.matches("[0-9a-f]{64}"));
    }

    @Test
    void contentSha256HexIsA64CharacterLowercaseHexString() {
        AwsSignatureV4.SignedRequest signed = signer.sign("PUT", "bucket.s3.amazonaws.com", "/pack.zip",
                "hello".getBytes(StandardCharsets.UTF_8), "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");

        assertEquals(64, signed.contentSha256Hex().length());
        assertTrue(signed.contentSha256Hex().matches("[0-9a-f]{64}"));
    }

    @Test
    void contentSha256HexIsDeterministicForTheSamePayload() {
        byte[] payload = "same content".getBytes(StandardCharsets.UTF_8);

        AwsSignatureV4.SignedRequest first = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip", payload,
                "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");
        AwsSignatureV4.SignedRequest second = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip", payload,
                "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");

        assertEquals(first.contentSha256Hex(), second.contentSha256Hex());
    }

    @Test
    void differentPayloadsProduceDifferentContentHashes() {
        AwsSignatureV4.SignedRequest a = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip",
                "payload-a".getBytes(StandardCharsets.UTF_8), "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");
        AwsSignatureV4.SignedRequest b = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip",
                "payload-b".getBytes(StandardCharsets.UTF_8), "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");

        assertNotEquals(a.contentSha256Hex(), b.contentSha256Hex());
    }

    @Test
    void differentSecretKeysProduceDifferentSignaturesForIdenticalRequest() {
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);

        AwsSignatureV4.SignedRequest a = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip", payload,
                "AKIAEXAMPLE", "secret-one", "us-east-1", "s3");
        AwsSignatureV4.SignedRequest b = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip", payload,
                "AKIAEXAMPLE", "secret-two", "us-east-1", "s3");

        assertNotEquals(a.authorizationHeader(), b.authorizationHeader());
    }

    @Test
    void differentRegionsProduceDifferentCredentialScopeInAuthorizationHeader() {
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);

        AwsSignatureV4.SignedRequest a = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip", payload,
                "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");
        AwsSignatureV4.SignedRequest b = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip", payload,
                "AKIAEXAMPLE", "secretkey", "eu-west-1", "s3");

        assertTrue(a.authorizationHeader().contains("/us-east-1/"));
        assertTrue(b.authorizationHeader().contains("/eu-west-1/"));
        assertNotEquals(a.authorizationHeader(), b.authorizationHeader());
    }

    @Test
    void amzDateFollowsIso8601BasicFormat() {
        AwsSignatureV4.SignedRequest signed = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip",
                "hello".getBytes(StandardCharsets.UTF_8), "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");

        assertTrue(signed.amzDate().matches("\\d{8}T\\d{6}Z"));
    }

    @Test
    void emptyPayloadProducesTheWellKnownSha256OfEmptyString() {
        AwsSignatureV4.SignedRequest signed = signer.sign("PUT", "bucket.s3.amazonaws.com", "/a.zip", new byte[0],
                "AKIAEXAMPLE", "secretkey", "us-east-1", "s3");

        // SHA-256("") es una constante bien conocida — sirve de ancla contra la
        // especificación real, no solo contra el propio código.
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", signed.contentSha256Hex());
    }
}
