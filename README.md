Uplux
===

Save user-defined scenarios from AnyLogic in S3.

Scope
---

This is an internal library for the [LUX Energy Twin](https://lux.energy) which mostly exists to bundle dependencies.

Installation
---

The library is published to [GitHub Packages](https://github.com/zenmo/uplux/packages).
Each [release](https://github.com/zenmo/uplux/releases) publishes a new version.

Add the GitHub Packages repository (authentication with a token that has
`read:packages` scope is required):

```kotlin
// settings.gradle.kts
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/zenmo/uplux")
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").get()
            password = providers.environmentVariable("GITHUB_TOKEN").get()
        }
    }
}

dependencies {
    implementation("energy.lux:uplux:v6")
}
```

Configuration
---

Set environment variables in a startup script for AnyLogic.

Windows:

```bat
set UPLUX_ACCESS_KEY=xxx
set UPLUX_SECRET_KEY=xxx
Anylogic.exe
```

Linux:

```bash
export UPLUX_ACCESS_KEY=xxx
export UPLUX_SECRET_KEY=xxx
export GTK_THEME=adwaita:light
./anylogic
```

Usage
---

Uplux uses the repository pattern to save and load scenarios.

Intialize repository class:

```java
import energy.lux.uplux.UserScenarioRepository;
import java.util.UUID;

var repository = UserScenarioRepository.builder()
        // p_userIdToken is a model input parameter which is injected by the Lux.energy website
        .userIdToken(p_userIdToken)
        .modelName("Mordor")
        .build();
```

Save scenario:

```java
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper jacksonMapper;
repository.saveUserScenario(
        "Use electric arc furnaces in the forges of Mordor",
        jacksonMapper.writeValueAsBytes(userAnyLogicScenario)
)
```

List saved scenarios:

```java
var scenarioList = repository.listScenarios();
for (var scenario : scenarioList) {
    System.out.println(scenario.getName());
}
```

Load scenario content:

```java
var jsonStream = repository.fetchUserScenarioContent(scenarioList.get(2).getId());
var deserializedScenario = jackson.readValue(jsonStream, MyAnyLogicScenario.class);
```

Delete scenario:

```java
repository.deleteUserScenario(scenarioList.get(2).getId());
```

Security
---

Uplux currently does not verify that the user is authorized to access requested objects. 

It is up to the library consumer to ensure access control and protect against 
user ID spoofing and ID guessing vulnerability.

Temporary download link
---

A secondary functionality of this library is to generate a temporary download link.

Example:

```java
import energy.lux.uplux.TemporaryFileRepository;
import java.nio.file.Path;

String url = TemporaryFileRepository
        .builder()
        .build()
        .createTemporaryDownloadLink(Path.of("export.xlsx"));

// Present it to the user in an AnyLogic agent:
this.getExperimentHost().openWebSite(url);
```

Building
---

Requires JDK 17.

```
./gradlew build
```

Releasing
---

Push a tag (e.g. `v5`) or publish a GitHub release.
The `publish` workflow then publishes the library to GitHub Packages with
`VERSION_TAG` set to the tag name:

```
VERSION_TAG=v5 GITHUB_ACTOR=<user> GITHUB_TOKEN=<token> ./gradlew publish
```
 
