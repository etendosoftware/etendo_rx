### Gradle properties
    Rename the 'gradle.properties.template' to 'gradle.properties'

### Generation of entities

- To generate the entities run

```./gradlew generate.entities --info```

> The properties will be taken from the 'gradle.properties' file.

- Command line parameters
    - ```-g <true-false>``` Specifies if the generation of the 'metadata.json' files should be generated automatically (Requires the 'com.etendoerp.etendorx' module installed).
    - ```-e <modules>``` A separated comma list of modules to exclude from the 'metadata.json' generation
    - ```-i <modules>``` A separated comma list of modules to include in the generation of the 'metadata.json'

```
Examples:
Omiting the generation of the 'metadata.json'
> ./gradlew generate.entities --info --args="-g false"

Excluding modules in the generation of the 'metadata.json'
> ./gradlew generate.entities --info --args="-g true -e com.etendoerp.module1,com.etendoerp.module2"

Specifing the only modules to generate the 'metadata.json'
> ./gradlew generate.entities --info --args="-g true -i com.etendoerp.module1,com.etendoerp.module2"
```

- Debug mode

```./gradlew generate.entities --info -Pdebug=true```

   Optional command line parameters

   ```'-Pport=<port number>' Specifies the port```

    To debug the generate-entities JAR copy the path location of the file obtained
    from the console output after the '-cp'

    Using intellij go to File -> Project Structure -> Libraries
    Press '+' -> New project library -> Java -> Paste the path location of the JAR file
    
    In the Intellij External Libraries you would see the 'generate-entities" JAR file.
    Go to 'generate.entities.jar' -> com -> etendorx.gen -> GenerateEntitiesApplication
    and set a breakpoint.

    In the 'Intellij configurations' create a new 'Remote JVM Debug' configuration
    using the specified port and press 'debug'.

### Project properties

You can set custom properties when running a project to override the default ones.

Example:
```
./gradlew com.etendorx.das:bootRun --info --args='--spring.datasource.url=jdbc:postgresql://localhost:5470/etendo'
```
