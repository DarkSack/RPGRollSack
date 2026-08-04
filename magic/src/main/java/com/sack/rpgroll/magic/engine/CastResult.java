package com.sack.rpgroll.magic.engine;

import java.util.List;

public record CastResult(boolean success, List<String> reasons) {

    public static CastResult ok() {
        return new CastResult(true, List.of());
    }

    public static CastResult failure(String reason) {
        return new CastResult(false, List.of(reason));
    }

    public static CastResult failure(List<String> reasons) {
        return new CastResult(false, List.copyOf(reasons));
    }

}
