package com.sack.rpgroll.sackresourcepack.build;

import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;
import com.sack.rpgroll.sackresourcepack.validation.ValidationIssue;

import java.io.File;
import java.util.List;

/**
 * El resultado de construir el resource pack.
 *
 * @param zipFile          el ZIP generado (o el que ya había, si vino de caché)
 * @param sha1             SHA-1 del ZIP en hexadecimal; vacío si el build falló
 * @param issues           problemas de validación, de merge y de resolución
 * @param fromCache        true si no se reconstruyó porque nada había cambiado
 * @param modules          módulos incluidos, en orden de resolución
 * @param resolutionErrors dependencias que no se pudieron resolver
 */
public record BuildResult(
        File zipFile,
        String sha1,
        List<ValidationIssue> issues,
        boolean fromCache,
        List<AssetModule> modules,
        List<String> resolutionErrors) {

    public boolean hasErrors() {
        return !resolutionErrors.isEmpty()
                || issues.stream().anyMatch(issue -> issue.severity() == ValidationIssue.Severity.ERROR);
    }

}
