package com.sack.rpgroll.chat;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;

import com.sack.rpgroll.chat.antispam.AntiSpamConfig;
import com.sack.rpgroll.chat.antispam.AntiSpamManager;
import com.sack.rpgroll.chat.antispam.MessageFilter;
import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.command.ChannelCommand;
import com.sack.rpgroll.chat.command.ChatAdminCommand;
import com.sack.rpgroll.chat.command.ChatLogCommand;
import com.sack.rpgroll.chat.command.EmoteCommand;
import com.sack.rpgroll.chat.command.IgnoreCommand;
import com.sack.rpgroll.chat.command.LanguageCommand;
import com.sack.rpgroll.chat.command.RPActionCommand;
import com.sack.rpgroll.chat.command.ReactCommand;
import com.sack.rpgroll.chat.command.SocialSpyCommand;
import com.sack.rpgroll.chat.command.WhisperCommand;
import com.sack.rpgroll.chat.context.ChatContextResolver;
import com.sack.rpgroll.chat.emote.EmoteManager;
import com.sack.rpgroll.chat.gui.ChatPromptManager;
import com.sack.rpgroll.chat.ignore.IgnoreManager;
import com.sack.rpgroll.chat.integration.ChatPlaceholders;
import com.sack.rpgroll.chat.language.LanguageManager;
import com.sack.rpgroll.chat.language.LanguageService;
import com.sack.rpgroll.chat.language.PlayerLanguageStateManager;
import com.sack.rpgroll.chat.listener.ChatListener;
import com.sack.rpgroll.chat.listener.PlayerSessionListener;
import com.sack.rpgroll.chat.log.ChatLogManager;
import com.sack.rpgroll.chat.mention.MentionResolver;
import com.sack.rpgroll.chat.pipeline.ChannelRouter;
import com.sack.rpgroll.chat.pipeline.ChatMessagePipeline;
import com.sack.rpgroll.chat.pipeline.MessageFormatter;
import com.sack.rpgroll.chat.player.PlayerChannelStateManager;
import com.sack.rpgroll.chat.reaction.ReactionManager;
import com.sack.rpgroll.chat.role.ChatRoleManager;
import com.sack.rpgroll.chat.whisper.WhisperManager;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChatPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("channels", "languages", "roles", "emotes");

    private LangManager langManager;
    private ChannelManager channelManager;
    private LanguageManager languageManager;
    private ChatRoleManager roleManager;
    private EmoteManager emoteManager;

    private PlayerLanguageStateManager playerLanguageStateManager;
    private LanguageService languageService;
    private PlayerChannelStateManager playerChannelStateManager;
    private IgnoreManager ignoreManager;
    private WhisperManager whisperManager;
    private ChatLogManager logManager;
    private ReactionManager reactionManager;
    private ChannelRouter channelRouter;
    private ChatMessagePipeline pipeline;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        channelManager = new ChannelManager(this);
        channelManager.initialize();

        languageManager = new LanguageManager(this);
        languageManager.initialize();

        roleManager = new ChatRoleManager(this);
        roleManager.initialize();

        emoteManager = new EmoteManager(this);
        emoteManager.initialize();

        playerLanguageStateManager = new PlayerLanguageStateManager(this);
        languageService = new LanguageService(languageManager, playerLanguageStateManager);

        playerChannelStateManager = new PlayerChannelStateManager(this, channelManager);
        ignoreManager = new IgnoreManager(this);
        whisperManager = new WhisperManager(langManager);
        logManager = new ChatLogManager(this);
        reactionManager = new ReactionManager(langManager);
        channelRouter = new ChannelRouter(ignoreManager);

        AntiSpamConfig antiSpamConfig = AntiSpamConfig.fromConfig(getConfig().getConfigurationSection("antispam"));
        AntiSpamManager antiSpamManager = new AntiSpamManager(antiSpamConfig);
        MessageFilter messageFilter = new MessageFilter(antiSpamConfig, loadReplacements());
        MentionResolver mentionResolver = new MentionResolver();
        ChatContextResolver contextResolver = new ChatContextResolver();
        MessageFormatter messageFormatter = new MessageFormatter(roleManager, contextResolver);

        pipeline = new ChatMessagePipeline(playerChannelStateManager, antiSpamManager, messageFilter, mentionResolver,
                languageService, channelRouter, messageFormatter, logManager, reactionManager, langManager);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);
        getServer().getPluginManager().registerEvents(new ChatListener(pipeline, this, langManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerSessionListener(playerChannelStateManager, ignoreManager, languageService), this);

        registerCommand("channel", new ChannelCommand(channelManager, playerChannelStateManager, langManager));
        registerCommand("ch", new ChannelCommand(channelManager, playerChannelStateManager, langManager));
        registerCommand("w", new WhisperCommand(whisperManager, ignoreManager, langManager));
        registerCommand("r", new WhisperCommand(whisperManager, ignoreManager, langManager));
        registerCommand("socialspy", new SocialSpyCommand(whisperManager, langManager));
        registerCommand("ignore", new IgnoreCommand(ignoreManager, langManager));

        RPActionCommand rpActionCommand = new RPActionCommand(playerChannelStateManager, channelRouter, langManager);
        registerCommand("me", rpActionCommand);
        registerCommand("do", rpActionCommand);

        EmoteCommand emoteCommand = new EmoteCommand(emoteManager, langManager);
        registerCommand("emote", emoteCommand);
        registerCommand("wave", emoteCommand);
        registerCommand("laugh", emoteCommand);
        registerCommand("sit", emoteCommand);
        registerCommand("cry", emoteCommand);
        registerCommand("dance", emoteCommand);

        registerCommand("react", new ReactCommand(reactionManager, langManager));
        registerCommand("chatlog", new ChatLogCommand(logManager, langManager));
        registerCommand("language", new LanguageCommand(languageManager, languageService, langManager));
        registerCommand("chatadmin", new ChatAdminCommand(channelManager, languageManager, roleManager, emoteManager,
                chatPromptManager, langManager, this));

        registerPlaceholders();

        getLogger().info("✔ RPGRoll-Chat habilitado. " + channelManager.count() + " canal(es), "
                + languageManager.count() + " idioma(s), " + emoteManager.count() + " emote(s).");
    }

    private Map<String, String> loadReplacements() {

        Map<String, String> replacements = new LinkedHashMap<>();
        var section = getConfig().getConfigurationSection("replacements");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                replacements.put(key, section.getString(key, ""));
            }
        }

        return replacements;
    }

    @Override
    public void onDisable() {

        if (playerChannelStateManager != null) {
            playerChannelStateManager.saveAll();
        }
        if (ignoreManager != null) {
            ignoreManager.saveAll();
        }
        if (playerLanguageStateManager != null) {
            playerLanguageStateManager.saveAll();
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {

        String[] info = metaFor(name);

        com.sack.rpgroll.common.command.BrigadierCommands.register(this, name, info[0],
                info[1].isEmpty() ? null : info[1], executor);
    }

    /**
     * Descripción y permiso de cada comando.
     * <p>
     * Vivían en {@code plugin.yml}, pero los comandos se registran por
     * Brigadier (ver {@code BrigadierCommands}) para que {@code execute as}
     * entregue al jugador real, y ahí ya no hay YAML de donde leerlos.
     */
    private String[] metaFor(String name) {

        return switch (name) {
            case "channel" -> new String[] { "Unirse/salir/listar canales de chat", "" };
            case "ch" -> new String[] { "Alias de /channel", "" };
            case "w" -> new String[] { "Enviar un mensaje privado", "" };
            case "r" -> new String[] { "Responder al último whisper", "" };
            case "socialspy" -> new String[] { "Alternar espía de whispers (staff)", "rpgrollchat.socialspy" };
            case "ignore" -> new String[] { "Ignorar jugadores/guilds/canales", "" };
            case "me" -> new String[] { "Acción narrativa en tercera persona", "" };
            case "do" -> new String[] { "Descripción narrativa fuera de personaje (OOC)", "" };
            case "emote" -> new String[] { "Ejecutar una emote (wave, laugh, sit, cry, dance, ...)", "" };
            case "wave" -> new String[] { "Emote de saludo", "" };
            case "laugh" -> new String[] { "Emote de risa", "" };
            case "sit" -> new String[] { "Emote de sentarse", "" };
            case "cry" -> new String[] { "Emote de llanto", "" };
            case "dance" -> new String[] { "Emote de baile", "" };
            case "react" -> new String[] { "Reaccionar al último mensaje visto en un canal", "" };
            case "chatlog" -> new String[] { "Buscar/exportar/moderar el historial de chat", "rpgrollchat.staff" };
            case "language" -> new String[] { "Aprender/hablar idiomas", "" };
            case "chatadmin" -> new String[] { "Comandos administrativos de chat", "rpgrollchat.admin.*" };
            default -> new String[] { "", "" };
        };
    }

    private void registerPlaceholders() {

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new ChatPlaceholders(this, channelManager, playerChannelStateManager, languageService, ignoreManager)
                .register();
        getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgrollchat_...%)");
    }

    // ============ API pública para otros addons ============

    public LangManager getLangManager() {
        return langManager;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public ChatMessagePipeline getPipeline() {
        return pipeline;
    }

    public ChannelRouter getChannelRouter() {
        return channelRouter;
    }

}
