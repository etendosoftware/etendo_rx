package com.etendorx.gen.process;

import com.etendoerp.etendorx.model.ETRXModelProvider;
import com.etendoerp.etendorx.model.ETRXModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.provider.OBProvider;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateMetadata {
    private static final Logger log = LogManager.getLogger();
    private static GenerateMetadata instance;

    private List<String> modulesDirectories = List.of("modules_core", "modules", "../modules_rx");
    private String defaultDirectory = "modules";

    private List<String> excludedModules = new ArrayList<>();
    private List<String> includedModules = new ArrayList<>();

    public static synchronized GenerateMetadata getInstance() {
        GenerateMetadata localInstance = instance;
        if (localInstance == null) {
            localInstance = OBProvider.getInstance().get(GenerateMetadata.class);
            instance = localInstance;
        }
        return localInstance;
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

    public List<String> getModulesDirectories() {
        return modulesDirectories;
    }

    public void setModulesDirectories(List<String> modulesDirectories) {
        this.modulesDirectories = modulesDirectories;
    }

    public String getDefaultDirectory() {
        return defaultDirectory;
    }

    public void setDefaultDirectory(String defaultDirectory) {
        this.defaultDirectory = defaultDirectory;
    }

    public void generate(String pathEtendoRx) {
        // Search the module location
        File etendoRxLocation = new File(pathEtendoRx);
        if (!etendoRxLocation.exists()) {
            throw new IllegalArgumentException("The Etendo RX location '"+ pathEtendoRx +"' does not exists.");
        }

        // Filter the excluded or included modules
        List<ETRXModule> filteredModules = ETRXModelProvider.getInstance().getEtendoRxModules().stream()
                .filter(moduleToGenerate -> {
                    // Filters the excluded modules
                    return excludedModules.stream().noneMatch(s -> s.equalsIgnoreCase(moduleToGenerate.getJavaPackage()));
                }).filter(moduleToGenerate -> {
                    // Filter the included modules if the 'includedModules' list is not empty and contains the 'moduleToGenerate'
                    return includedModules.isEmpty() || includedModules.stream().anyMatch(s -> s.equalsIgnoreCase(moduleToGenerate.getJavaPackage()));
                }).collect(Collectors.toList());

        if (!filteredModules.isEmpty()) {
            Map<ETRXModule, String> modulesJsonMap = ETRXModelProvider.getInstance().modulesToJsonMap(filteredModules);

            modulesJsonMap.forEach((module, json) -> {
                createMetadataJson(etendoRxLocation, module, json);
            });
        }
    }

    public boolean createMetadataJson(File etendoRxLocation, ETRXModule module, String jsonMetadata) {
        String moduleJavaPackage = module.getJavaPackage();
        log.info("* Starting generation of the metadata file for: " + moduleJavaPackage);

        try {
            File moduleLocation = null;

            for (String moduleDir : this.modulesDirectories) {
                log.info("Searching the module location in the '{}' directory", moduleDir);
                moduleLocation = new File(etendoRxLocation, moduleDir + File.separator + moduleJavaPackage);
                if (moduleLocation.exists()) {
                    break;
                }
            }

            if (moduleLocation == null || !moduleLocation.exists()) {
                log.info("Using the default module location '{}' to generate the metadata.", defaultDirectory);
                moduleLocation = new File(etendoRxLocation, defaultDirectory + File.separator + moduleJavaPackage);
            }

            File metadataLocation = new File(moduleLocation, "src-db" + File.separator + "das" + File.separator + "metadata.json");

            if (!metadataLocation.exists()) {
                new File(metadataLocation.getParent()).mkdirs();
                metadataLocation.createNewFile();
            }

            FileWriter writer = new FileWriter(metadataLocation, false);
            writer.write(jsonMetadata);
            writer.close();

            log.info("Metadata generated for '{}' in: {}", moduleJavaPackage, metadataLocation.getAbsolutePath());
            return true;
        } catch (Exception e) {
            log.error("Error creating the '{}' for the module: {}", "metadata.json", module.getJavaPackage(), e);
            return false;
        }
    }

}
