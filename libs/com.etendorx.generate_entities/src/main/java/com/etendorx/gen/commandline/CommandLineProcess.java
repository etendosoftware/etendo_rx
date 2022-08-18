package com.etendorx.gen.commandline;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class CommandLineProcess {

    private static final Logger log = LogManager.getLogger();

    public static final String GENERATE_METADATA_OPT = "g";
    public static final String EXCLUDE_MOD_OPT = "e";
    public static final String INCLUDE_MOD_OPT = "i";

    private boolean generateMetadata = true;
    private List<String> excludedModules = new ArrayList<>();
    private List<String> includedModules = new ArrayList<>();

    public CommandLineProcess(String... args) {
        Optional<CommandLine> cmdOptional = generateCommandLine(args);
        cmdOptional.ifPresent(this::parseCommandLine);
    }

    public Optional<CommandLine> generateCommandLine(String... args) {
        try {
            Options options = new Options();
            options.addOption("g", "generate-metadata", true, "Generate the metadata.json file.");
            options.addOption("e", "exclude", true, "List of comma separated modules to exclude from the metadata generation.");
            options.addOption("i", "include", true, "List of comma separated modules to include in the metadata generation.");
            CommandLineParser parser = new DefaultParser();
            return Optional.of(parser.parse(options, args));
        } catch (Exception e) {
            log.debug("Error parsing the command line parameters", e);
        }
       return Optional.empty();
    }

    public void parseCommandLine(CommandLine cmd) {
        try {
            if (cmd.hasOption(GENERATE_METADATA_OPT)) {
                this.generateMetadata = getCommandLineOptionWrapper(() -> {
                    String g = cmd.getOptionValue(GENERATE_METADATA_OPT);
                    return Boolean.parseBoolean(g);
                });
            }

            if (cmd.hasOption(EXCLUDE_MOD_OPT)) {
                this.excludedModules = getCommandLineOptionWrapper(() -> {
                    String e = cmd.getOptionValue(EXCLUDE_MOD_OPT);
                    return parseStringToList(e);
                });
            }

            if (cmd.hasOption(INCLUDE_MOD_OPT)) {
                this.includedModules = getCommandLineOptionWrapper(() -> {
                  String i = cmd.getOptionValue(INCLUDE_MOD_OPT);
                  return parseStringToList(i);
                });
            }

        } catch (CommandLineException cmdException) {
            log.error("Error obtaining command line options", cmdException);
            throw cmdException;
        } catch (Exception e) {
            log.debug("Error parsing command line", e);
        }
    }

    static List<String> parseStringToList(String str) {
        return Arrays.asList(str.split(","));
    }

    static <T> T getCommandLineOptionWrapper(Supplier<T> s) {
        try {
            return s.get();
        } catch (Exception e) {
            throw new CommandLineException(e);
        }
    }

    public boolean isGenerateMetadata() {
        return generateMetadata;
    }

    public void setGenerateMetadata(boolean generateMetadata) {
        this.generateMetadata = generateMetadata;
    }

    public List<String> getExcludedModules() {
        return excludedModules;
    }

    public void setExcludedModules(List<String> excludedModules) {
        this.excludedModules = excludedModules;
    }

    public List<String> getIncludedModules() {
        return includedModules;
    }

    public void setIncludedModules(List<String> includedModules) {
        this.includedModules = includedModules;
    }

}
