package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.core.AscensionRequirements;
import com.sack.rpgroll.ascension.core.AscensionRequirementsParser;
import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class SecretUnlockParser implements ContentParser<SecretUnlockRequirement> {

    @Override
    public SecretUnlockRequirement parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String rawType = config.getString("target-type");
        if (rawType == null) {
            throw new IllegalArgumentException("desbloqueo secreto '" + id + "' sin campo 'target-type'");
        }

        SecretTargetType targetType = SecretTargetType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        String targetId = config.getString("target-id");

        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("desbloqueo secreto '" + id + "' sin campo 'target-id'");
        }

        AscensionRequirements requirements = AscensionRequirementsParser.parse(
                config.getConfigurationSection("requirements"));

        return new SecretUnlockRequirement(id, targetType, targetId, requirements);
    }

}
