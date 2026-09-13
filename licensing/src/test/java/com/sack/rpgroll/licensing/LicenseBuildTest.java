package com.sack.rpgroll.licensing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LicenseBuildTest {

    /**
     * Los tests corren con la compilación normal, la misma que va al release.
     * Si esto falla, alguien dejó el proyecto construyendo jars que aceptan
     * -Drpgroll.devmode — y con él, saltarse la licencia desde el start.bat.
     */
    @Test
    void aNormalBuildIsNeverADevBuild() {
        assertFalse(LicenseBuild.DEV_BUILD);
    }
}
