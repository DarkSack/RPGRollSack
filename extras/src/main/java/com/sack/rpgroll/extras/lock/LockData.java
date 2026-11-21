package com.sack.rpgroll.extras.lock;

import java.util.UUID;

/** La cerradura de un bloque: quién la puso y su código de 4 dígitos. Se guarda como {@code uuid;código}. */
record LockData(UUID owner, String code) {

    static boolean validCode(String code) {
        return code != null && code.length() == 4 && code.chars().allMatch(Character::isDigit);
    }

    String encode() {
        return owner + ";" + code;
    }

    /** Null si el valor no es una cerradura bien escrita. */
    static LockData decode(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(";", 2);
        if (parts.length != 2 || !validCode(parts[1])) {
            return null;
        }
        try {
            return new LockData(UUID.fromString(parts[0]), parts[1]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
