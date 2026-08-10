package com.sack.rpgroll.extras.hud;

import java.util.List;

public record HudSettings(boolean enabled, int intervalTicks, String separator, List<HudLineFormat> lines) {

    public static HudSettings disabled() {
        return new HudSettings(false, 20, "  ", List.of());
    }

}
