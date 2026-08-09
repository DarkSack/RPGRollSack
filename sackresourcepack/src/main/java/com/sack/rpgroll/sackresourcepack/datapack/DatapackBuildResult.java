package com.sack.rpgroll.sackresourcepack.datapack;

import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;

import java.io.File;
import java.util.List;

public record DatapackBuildResult(File mergedDirectory, List<AssetModule> modules, List<String> resolutionErrors) {

    public boolean hasErrors() {
        return !resolutionErrors.isEmpty();
    }

}
