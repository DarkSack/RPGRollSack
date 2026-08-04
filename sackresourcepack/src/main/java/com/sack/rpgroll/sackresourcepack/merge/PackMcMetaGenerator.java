package com.sack.rpgroll.sackresourcepack.merge;

import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code pack.mcmeta} NO es un archivo por módulo — un resource pack solo
 * tiene uno, en la raíz. Se genera acá, una vez, a partir de qué módulos
 * entraron en este build — si algún módulo trae su propio pack.mcmeta por
 * error, se ignora (no tiene sentido fusionarlo).
 */
public class PackMcMetaGenerator {

    public void generate(File outputDirectory, int packFormat, List<AssetModule> modules) {

        String moduleList = modules.stream().map(AssetModule::id).collect(Collectors.joining(", "));

        String json = "{\n"
                + "  \"pack\": {\n"
                + "    \"pack_format\": " + packFormat + ",\n"
                + "    \"description\": \"Generado por SackResourcePack — módulos: " + escape(moduleList) + "\"\n"
                + "  }\n"
                + "}\n";

        try {
            Files.writeString(new File(outputDirectory, "pack.mcmeta").toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo escribir pack.mcmeta", e);
        }
    }

    private String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
