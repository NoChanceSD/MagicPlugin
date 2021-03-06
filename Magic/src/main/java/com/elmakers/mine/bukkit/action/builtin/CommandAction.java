package com.elmakers.mine.bukkit.action.builtin;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import com.elmakers.mine.bukkit.spell.BaseSpell;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.conversations.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Can run any Bukkit command as a Spell.
 *
 * This includes running as Console, or opping/deopping the player if needed.
 *
 * This spell can also act as a targeting spell, running commands using the
 * target location or entity.
 *
 * The following parameters will all be substituted in the "command" string
 * if found:
 *
 * @_ - A spell, useful for command-line casting
 * @spell - name of spell being cast
 * @p - mage name
 * @uuid - mage uuid
 * @world, @x, @y, @z - mage location
 *
 * If targeting is used ("target: none" to disable), the following will also be escaped:
 *
 * @t - target mage name
 * @tuuid - target entity uuid
 * @tworld, @tx, @ty, @tz - target location
 *
 * If @arg is present in the command string a conversation will be created with the player to gather the input.
 */
public class CommandAction extends BaseSpellAction {
    public final static String[] PARAMETERS = {
            "command", "console", "op", "local_echo", "modal", "timeout", "escape_sequence"
    };

    private List<String> commands = new ArrayList<String>();
    private boolean asConsole;
    private boolean opPlayer;
    private boolean localEcho;
    private boolean modal;
    private int timeout;
    private String escapeSequence;

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters) {
        super.prepare(context, parameters);
        asConsole = parameters.getBoolean("console", false);
        opPlayer = parameters.getBoolean("op", false);
        localEcho = parameters.getBoolean("local_echo", true);
        modal = parameters.getBoolean("modal", false);
        timeout = parameters.getInt("timeout", 0);
        escapeSequence = parameters.getString("escape_sequence", "");
    }

    @Override
    public void initialize(Spell spell, ConfigurationSection parameters) {
        super.initialize(spell, parameters);
        commands.clear();

        if (parameters.contains("command"))
        {
            String command = parameters.getString("command");
            if (command != null && command.length() > 0)
            {
                commands.add(command);
            }
        }
        else
        {
            commands.addAll(parameters.getStringList("commands"));
        }
    }

    @Override
    public SpellResult perform(CastContext context) {
        Mage mage = context.getMage();
        MageController controller = context.getController();
        CommandSender sender = asConsole ? Bukkit.getConsoleSender() : mage.getCommandSender();
        if (sender == null) {
            return SpellResult.FAIL;
        }
        Queue<String> conversationCommands = new ArrayDeque<String>(commands.size());
        boolean isOp = sender.isOp();
        if (opPlayer && !isOp) {
            sender.setOp(true);
        }
        for (String command : commands) {
            try {
                String converted = context.parameterize(command);
                if (converted.contains("@arg")) {
                    conversationCommands.add(converted);
                } else {
                    controller.getPlugin().getServer().dispatchCommand(sender, converted);
                }
            } catch (Exception ex) {
                controller.getLogger().log(Level.WARNING, "Error running command: " + command, ex);
            }
        }
        if (opPlayer && !isOp) {
            sender.setOp(false);
        }
        if (!conversationCommands.isEmpty()) {
            runConversations(context, conversationCommands, opPlayer);
        }
        return SpellResult.CAST;
    }

    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters)
    {
        super.getParameterNames(spell, parameters);
        parameters.addAll(Arrays.asList(PARAMETERS));
    }

    @Override
    public void getParameterOptions(Spell spell, String parameterKey, Collection<String> examples)
    {
        if (parameterKey.equals("command")) {
            examples.add("spawn");
            examples.add("clear");
        } else if (parameterKey.equals("escape_sequence")) {
            examples.add("$$");
            examples.add("!");
            examples.add("cancel");
        } else if (parameterKey.equals("timeout")) {
            examples.add("5");
            examples.add("10");
        } else if (parameterKey.equals("op") || parameterKey.equals("console")
                || parameterKey.equals("local_echo") || parameterKey.contains("modal")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_BOOLEANS));
        } else {
            super.getParameterOptions(spell, parameterKey, examples);
        }
    }

    private void runConversations(CastContext context, Queue<String> commands, boolean opPlayer) {
        CommandSender sender = context.getMage().getCommandSender();
        if (!(sender instanceof Conversable)) {
            return;
        }
        Conversable conversable = (Conversable) sender;
        ArgumentsPrompt argumentsPrompt = new ArgumentsPrompt(context, sender, commands, opPlayer);
        ConversationFactory factory = new ConversationFactory(context.getController().getPlugin());
        factory.withLocalEcho(localEcho).withModality(modal).withFirstPrompt(argumentsPrompt);
        if (timeout > 0) {
            factory.withTimeout(timeout);
        }
        if (!escapeSequence.isEmpty()) {
            factory.withEscapeSequence(escapeSequence);
        }
        conversable.beginConversation(factory.buildConversation(conversable));
    }

    private static class ArgumentsPrompt implements Prompt {
        private final CastContext context;
        private final CommandSender sender;
        private final String currentCommand;
        private final int argCount;
        private final String[] arguments;
        private final Queue<String> remainingCommands;
        private final boolean opPlayer;
        private Integer currentArg = 1;

        public ArgumentsPrompt(CastContext context, CommandSender sender, Queue<String> remainingCommands, boolean opPlayer) {
            this.context = context;
            this.sender = sender;
            this.currentCommand = remainingCommands.poll();
            this.argCount = StringUtils.countMatches(currentCommand, "@arg");
            this.arguments = new String[argCount];
            this.remainingCommands = remainingCommands;
            this.opPlayer = opPlayer;
        }

        @Override
        public String getPromptText(ConversationContext conversationContext) {
            return context.getMessage("command_prompt", "Input argument $argnum of /$command")
                    .replace("$argnum", currentArg.toString()).replace("$command", currentCommand);
        }

        @Override
        public boolean blocksForInput(ConversationContext conversationContext) {
            return true;
        }

        @Override
        public Prompt acceptInput(ConversationContext conversationContext, String s) {
            arguments[currentArg - 1] = s;
            currentArg++;
            if (currentArg > argCount) {
                runCommand(StringUtils.replaceEach(currentCommand, new String[]{"@arg"}, getArguments()));
                if (!remainingCommands.isEmpty()) {
                    return new ArgumentsPrompt(context, sender, remainingCommands, opPlayer);
                } else {
                    return END_OF_CONVERSATION;
                }
            }
            return this;
        }

        public String[] getArguments() {
            return arguments;
        }

        private void runCommand(String command) {
            boolean isOp = sender.isOp();
            if (!isOp && opPlayer) {
                sender.setOp(true);
            }
            context.getController().getPlugin().getServer().dispatchCommand(sender, command);
        }
    }
}
