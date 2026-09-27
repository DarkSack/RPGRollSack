package com.sack.rpgroll.sackresourcepack.manifest;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * El manifiesto {@code pack.yml} de un módulo de assets en {@code
 * content/<id>/} — nunca se construye a mano fuera de {@link
 * ManifestScanner}, {@code directory} siempre apunta a la carpeta real
 * del módulo en disco (para resolver {@code assets/} relativo a ella).
 *
 * @param priority a igualdad de dependencias, menor prioridad se fusiona primero (los de mayor prioridad pueden pisar a los de menor)
 * @param depends  ids de otros módulos obligatorios — si falta alguno, el build falla con un error claro
 * @param optional ids de otros módulos opcionales — si están presentes se ordenan antes, si no, se ignoran sin error
 * @param exclude  rutas dentro de {@code assets/} que no entran al pack (prefijos, p. ej. {@code ns/carpeta/})
 */
public record AssetModule(
        String id,
        String name,
        String version,
        String author,
        String namespace,
        int priority,
        List<String> depends,
        List<String> optional,
        String description,
        File directory,
        List<String> exclude) {

    public AssetModule(String id, String name, String version, String author, String namespace, int priority,
            List<String> depends, List<String> optional, String description, File directory) {
        this(id, name, version, author, namespace, priority, depends, optional, description, directory, List.of());
    }

    public AssetModule {
        Objects.requireNonNull(id, "id no puede ser null");
        name = name == null || name.isBlank() ? id : name;
        version = version == null || version.isBlank() ? "1.0.0" : version;
        author = author == null ? "" : author;
        namespace = namespace == null || namespace.isBlank() ? id : namespace;
        depends = depends == null ? List.of() : List.copyOf(depends);
        optional = optional == null ? List.of() : List.copyOf(optional);
        description = description == null ? "" : description;
        Objects.requireNonNull(directory, "directory no puede ser null");
        exclude = exclude == null ? List.of() : List.copyOf(exclude);
    }

    /** true si la ruta (relativa a {@code assets/}) cae dentro de algún {@code exclude}. */
    public boolean excludes(String relativePath) {
        for (String prefix : exclude) {
            if (relativePath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public File assetsDirectory() {
        return new File(directory, "assets");
    }

    /** Carpeta {@code data/} del módulo (namespaces de datapack: tags, loot tables, recipes, functions...). */
    public File dataDirectory() {
        return new File(directory, "data");
    }

}
