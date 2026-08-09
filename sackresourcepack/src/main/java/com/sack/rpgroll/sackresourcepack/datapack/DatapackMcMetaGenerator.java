package com.sack.rpgroll.sackresourcepack.datapack;

import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Genera el {@code pack.mcmeta} de un datapack — misma clave JSON
 * ({@code pack_format}) que el de un resource pack, pero con una escala de
 * números completamente distinta (los "pack format" de datapack y resource
 * pack no comparten numeración entre versiones de Minecraft), por eso usa
 * su propio valor de configuración separado en vez de reusar
 * {@code pack-format}.
 */
public class DatapackMcMetaGenerator {

    public void generate(File outputDirectory, int packFormat, List<AssetModule> modules) {

        String moduleList = modules.stream().map(AssetModule::id).collect(Collectors.joining(", "));

        String json = "{\n"
                + "  \"pack\": {\n"
                + "    \"pack_format\": " + packFormat + ",\n"
                + "    \"description\": \"Datapack generado por SackResourcePack — módulos: " + escape(moduleList)
                + "\"\n"
                + "  }\n"
                + "}\n";

        try {
            Files.writeString(new File(outputDirectory, "pack.mcmeta").toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo escribir el pack.mcmeta del datapack", e);
        }
    }

    private String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
