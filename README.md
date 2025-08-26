Uplux
===

Save user-defined scenarios from AnyLogic.

Scope
---

This is an internal library which mostly exists to bundle dependencies.

Installation
---

Download the [latest release](https://github.com/zenmo/uplux/releases)
and add it to your AnyLogic project dependencies.

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
        .userId(UUID.fromString("6b87f0f9-fdf6-4c05-9e0f-b75e46950113"))
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
