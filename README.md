# Etendo RX
This is the development repository of Etendo RX. <br>

### Etendo RX Instalation

1. Clone the repository
```
git clone git@github.com:etendosoftware/etendo_rx.git
```

2. To compile and deploy an Etendo RX instance you have to setup the configuration variables, to do that you have to create a copy of `gradle.properties.template` file in root.

```bash
cp gradle.properties.template gradle.properties
```

3. You can either edit `gradle.properties` file updating the variables, or use their default values.
> **Info** <br>
> Remember to configure `githubUser` and `githubToken`

4. Run setup tasks to create the configurations files
```
./gradlew setup
```
> **Warning** <br>
> If you change the default **bbdd.url** and/or **bbdd.sid**, you must edit the `rxconfig/das.yaml` file using the new values.

5. Execute the generate.entities task

```
./gradlew generate.entities 
```

6. To execute RX services run:
```
./gradlew rx 
```
By default the following services must be up:
- Config
- Auth
- Edge
- Das
- Async


### Project properties

You can set custom properties when running a project to override the default ones.

Example:

```
./gradlew com.etendorx.das:bootRun --info --args='--spring.datasource.url=jdbc:postgresql://localhost:5470/etendo'
```
