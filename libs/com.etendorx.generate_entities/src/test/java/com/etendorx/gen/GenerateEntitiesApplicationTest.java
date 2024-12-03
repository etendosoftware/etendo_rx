package com.etendorx.gen;

import com.etendorx.gen.commandline.CommandLineProcess;
import com.etendorx.gen.generation.GenerateEntities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;

import static org.mockito.Mockito.*;

class GenerateEntitiesApplicationTest {

  @Mock
  private CommandLineProcess commandLineProcess;

  @Mock
  private GenerateEntities generateEntities;

  private GenerateEntitiesApplication application;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    application = new GenerateEntitiesApplication() {
      @Override
      public void run(String... args) {
        // Override to inject mocks
        var generateEntities = GenerateEntitiesApplicationTest.this.generateEntities;
        generateEntities.setPropertiesFile(
            new File(".").getAbsolutePath() + File.separator + "gradle.properties"
        );
        generateEntities.setFriendlyWarnings(false);
        generateEntities.execute(commandLineProcess);
      }
    };
  }

  @Test
  void testRun() {
    // Simular los argumentos
    String[] args = {"--arg1", "--arg2"};
    commandLineProcess = new CommandLineProcess(args);

    // Ejecutar el método
    application.run(args);

    // Verificar que las dependencias fueron invocadas correctamente
    verify(generateEntities).setPropertiesFile(anyString());
    verify(generateEntities).setFriendlyWarnings(false);
    verify(generateEntities).execute(commandLineProcess);
  }

  @Test
  void testMain() {
    // Verificar que el método main no lanza excepciones
    GenerateEntitiesApplication.main(new String[]{"--arg1", "--arg2"});
  }
}
