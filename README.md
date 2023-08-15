### Gradle properties

    Rename the 'gradle.properties.template' to 'gradle.properties'

### Generation of entities

- To generate the entities run

```./gradlew generate.entities --info```

> The properties will be taken from the 'gradle.properties' file.

### Project properties

You can set custom properties when running a project to override the default ones.

Example:

```
./gradlew com.etendorx.das:bootRun --info --args='--spring.datasource.url=jdbc:postgresql://localhost:5470/etendo'
```
