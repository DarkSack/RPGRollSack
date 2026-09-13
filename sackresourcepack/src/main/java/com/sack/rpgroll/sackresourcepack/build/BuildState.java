package com.sack.rpgroll.sackresourcepack.build;

/**
 * Lo que se recuerda del último build, en {@code build/state.yml}.
 *
 * @param signature     huella de los módulos y de sus archivos al construir
 * @param sha1          SHA-1 del ZIP que salió de ese build
 * @param builtAtMillis cuándo se construyó
 */
public record BuildState(String signature, String sha1, long builtAtMillis) {
}
