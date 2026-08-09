package com.sack.rpgroll.sackresourcepack.distribution.s3;

/**
 * @param endpoint   host del proveedor S3-compatible (ej. "s3.amazonaws.com", "s3.us-west-002.backblazeb2.com",
 *                   o el host:puerto de un MinIO propio)
 * @param pathStyle  true = {@code https://endpoint/bucket/key} (MinIO y muchos proveedores no-AWS lo requieren);
 *                   false = {@code https://bucket.endpoint/key} (estilo "virtual-hosted", el que usa AWS por defecto)
 */
public record S3UploadSettings(String endpoint, String bucket, String region, String objectKey, String accessKey,
        String secretKey, boolean pathStyle) {

    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank() && bucket != null && !bucket.isBlank() && accessKey != null
                && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }

}
