# SonarQube 1C (BSL) Community Plugin

Support languages 1C:Enterprise and OneScript for [SonarQube](http://sonarqube.org).

## Возможности

- Project "Overview" dashboard;
- Highlighting the source code of 1C: Enterprise;
- Расчет базовых метрик, расчет количества строк кода;
- Registration of diagnostics provided by [BSL Language Server](https://1c-syntax.github.io/bsl-language-server) as internal rules; 
- Встроенный анализатор - BSL Language Server Diagnostic provider
- Import results from external analyzers in internal [json](https://1c-syntax.github.io/bsl-language-server/reporters/json.html) format;

## Install and Update

- Download jar file from [releases](https://github.com/1c-syntax/sonar-bsl-plugin-community/releases) page
- Put the jar file according to Manual Installation [section of official documentation](https://docs.sonarqube.org/latest/setup/install-plugin/) (default - `$SONARQUBE_HOME/extensions/plugins`)
- Перезапустить сервер

Версия SonarQube | Версия плагина
--- | ---
7.9+ | 0.6.0+
7.4 - 7.8 | 0.1.0...0.5.1

## Запуск анализа

### Настройка окружения

Analisis of the source code 1C is used by the utility [ sonar-scanner ](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner).

You must specify the analysis parameters for the utility in one of the following ways:

- as command line arguments using the syntax -D nameParameter = valueParameter
- using the file ` sonar-project.properties file `

An example of the ` sonar-project.properties file `:

```properties
# The key of the project. Unique within the SonarQube server
sonar.projectKey=my_project
# The project name displayed in the SonarQube interface. The default value is the project key.
sonar.projectName=My project
# The version of the project
sonar.projectVersion=1.0
 
# The path to the source code. Relative paths are resolved from the sonar-project file.properties
# A slash ("/") is used as a path separator. You can specify multiple directories separated by commas.
sonar.sources=src
 
# Encoding of source files. 
sonar.sourceEncoding=UTF-8

# Filters for inclusion in the analysis. In the example below - only bsl and os files.
sonar.inclusions=**/*.bsl, **/*.os
```

Design parameters can be combined.

If the SonarQube server has enabled the requirement of forced authorization and/or the prohibition of anonymous project analysis, the sonar-scanner utility additionally needs to pass an authorization token, which can be obtained according to the instruction [User guide/User token](https://docs.sonarqube.org/latest/user-guide/user-token/)

### For example

```sh
sonar-scanner -Dsonar.host.url=http://sonar.company.com -Dsonar.login=SONAR_AUTH_TOKEN
```

## Настройки плагина

- `sonar.bsl.languageserver.diagnosticLanguage` - the language of the rule names and message text of the triggered rules from the BSL Language Server. Default - `ru` - Russian;
- `sonar.bsl.languageserver.enabled` - use the built-in BSL Language Server Diagnostic provider analyzer when running analysis via `sonar-scanner`. Default - `true` - enabled;
- `sonar.bsl.languageserver.reportPaths` - the path to the report files in the internal format to the BSL Language Server - {code 1}json. By default - `" " ` - not filled.

## Интеграция с BSL Language Server

By default, the built-in diagnostics provider from BSL Language Server is used as the analyzer.

The built-in analyzer can disable the analysis by setting the `sonar parameter.bsl.language server.enabled` value `false` via command line or settings file.

```sh
sonar-scanner -Dsonar.bsl.languageserver.enabled=false
```

> Disabling the analyzer does not disable the file parsing process. Metrics calculation and syntax highlighting will work regardless of the setting value.

### Импорт результатов из внешнего файла

[BSL Language Server](https://github.com/1c-syntax/bsl-language-server) can run source code analysis and output a list of detected diagnostics as a JSON file. Instructions for running BSL Language Server in analysis mode are available on the project page.

To import the result when you run the sonar-scanner utility, pass the parameter `sonar.bsl.languageserver.reportPaths` via command-line arguments or via the `sonar-project file.properties`, which specifies the path to the file (or files, separated by commas) with the analysis results.

```sh
sonar-scanner -Dsonar.bsl.languageserver.reportPaths=./bsl-json.json
```
