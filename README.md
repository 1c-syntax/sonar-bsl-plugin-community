# SonarQube 1C (BSL) Community Plugin

[![Actions Status](https://github.com/1c-syntax/sonar-bsl-plugin-community/workflows/Java%20CI/badge.svg)](https://github.com/1c-syntax/sonar-bsl-plugin-community/actions)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=1c-syntax_sonar-bsl-plugin-community&metric=alert_status)](https://sonarcloud.io/dashboard?id=1c-syntax_sonar-bsl-plugin-community)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=1c-syntax_sonar-bsl-plugin-community&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=1c-syntax_sonar-bsl-plugin-community)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=1c-syntax_sonar-bsl-plugin-community&metric=coverage)](https://sonarcloud.io/dashboard?id=1c-syntax_sonar-bsl-plugin-community)

[![Download](https://img.shields.io/github/release/1c-syntax/sonar-bsl-plugin-community.svg?label=download&style=flat)](https://github.com/1c-syntax/sonar-bsl-plugin-community/releases/latest)
[![Download night build](https://img.shields.io/github/workflow/status/1c-syntax/sonar-bsl-plugin-community/Night%20build/nigth_build?label=night%20build)](https://github.com/1c-syntax/sonar-bsl-plugin-community/releases/v999.99.99)

[![telegram](https://img.shields.io/badge/telegram-chat-green.svg)](https://t.me/bsl_language_server)

Поддержка языка 1С:Предприятие 8 и OneScript для [SonarQube](http://sonarqube.org).

Сайт проекта -> https://1c-syntax.github.io/sonar-bsl-plugin-community/

English version -> https://1c-syntax.github.io/sonar-bsl-plugin-community/en/

## Возможности

* Project "Overview" dashboard;
* Подсветка исходного кода 1С:Предприятие;
* Расчет базовых метрик, расчет количества строк кода;
* Регистрация диагностик, предоставляемых [BSL Language Server](https://1c-syntax.github.io/bsl-language-server) как внутренних правил;
* Встроенный анализатор - BSL Language Server Diagnostic provider 
* Импорт результатов внешних анализаторов во внутреннем формате [json](https://1c-syntax.github.io/bsl-language-server/reporters/json.html);

## Установка и обновление

* Скачать jar-файл со страницы [релизов](https://github.com/1c-syntax/sonar-bsl-plugin-community/releases)
* Разместить jar-файл согласно разделу Manual Installation [официальной документации](https://docs.sonarqube.org/latest/setup/install-plugin/) (по умолчанию - каталог `$SONARQUBE_HOME/extensions/plugins`)
* Перезапустить сервер

## Требования

Версия SonarQube | Версия плагина
-----------------|----------------
7.9+             | 0.7.0+
7.4 - 7.8        | 0.1.0...0.6.0

Версия плагина | Версия JAVA
---------------|------------------
1.0+           | 11
0.1.0 - 0.6.0  | 8

## Запуск анализа

### Настройка окружения

Для анализа исходных кодов 1С используется утилита [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner).

Утилите неоходимо указать параметры анализа одним из нижеперечисленных способов:
* в качестве аргументов командной строки, используя синтаксис -DимяПараметра=значениеПараметра
* используя файл `sonar-project.properties`

Пример файла `sonar-project.properties`:

```properties
# Ключ проекта. Уникальный в пределах сервера SonarQube
sonar.projectKey=my_project
# Имя проекта, отображаемое в интерфейсе SonarQube. Значение по умолчанию - ключ проекта.
sonar.projectName=My project
# Версия проекта
sonar.projectVersion=1.0
 
# Путь к исходным кодам. Относительные пути разрешаются от файла sonar-project.properties
# В качестве разделителя пути используется прямой слэш - /. Можно указать несколько каталогов через запятую.
sonar.sources=src
 
# Кодировка файлов исходных кодов.
sonar.sourceEncoding=UTF-8

# Фильтры на включение в анализ. В примере ниже - только bsl и os файлы.
sonar.inclusions=**/*.bsl, **/*.os
```

Способы передачи параметров можно комбинировать.

Если на сервере SonarQube включено требование принудительной авторизации и/или запрет анонимного анализа проектов, утилите sonar-scanner дополнительно нужно передавать токен авторизации, который можно получить согласно инструкции [User guide/User token](https://docs.sonarqube.org/latest/user-guide/user-token/)

### Пример строки запуска

```sh
sonar-scanner -Dsonar.host.url=http://sonar.company.com -Dsonar.login=SONAR_AUTH_TOKEN
```

## Настройки плагина

* `sonar.bsl.languageserver.diagnosticLanguage` - язык имен правил и текстов сообщений сработавших правил от BSL Language Server. По умолчанию - `ru` - русский;
* `sonar.bsl.languageserver.enabled` - использование встроенного анализатора BSL Language Server Diagnostic provider при запуске анализа через `sonar-scanner`. По умолчанию - `true` - включен;
* `sonar.bsl.languageserver.reportPaths` - путь к файлам отчетов во внутреннем формате BSL Language Server - `json`. По умолчанию - `""` - не заполнено.

## Интеграция с BSL Language Server

По умолчанию в качестве анализатора используется встроенный провайдер диагностик из BSL Language Server.

Выполнение анализа встроенным анализатором можно отключить, установив параметру `sonar.bsl.languageserver.enabled` значение `false` через командную строку или файл настроек.

```sh
sonar-scanner -Dsonar.bsl.languageserver.enabled=false
```

> Отключение анализатора не отключает процесс парсинга файлов. Расчет метрик и подсветка синтаксиса будут работать вне зависимости от значения настройки.

### Импорт результатов из внешнего файла

[BSL Language Server](https://github.com/1c-syntax/bsl-language-server) может запускать анализ исходного кода и выдавать список обнаруженых диагностик в виде json-файла. Инструкция по запуску BSL Language Server в режиме анализа расположена на странице проекта.

Для импорта результата при запуске утилиты sonar-scanner нужно передать параметр `sonar.bsl.languageserver.reportPaths` через аргументы командной строки или через файл `sonar-project.properties`, в котором указать путь к файлу (или файлам, через запятую) с результатами анализа.

```sh
sonar-scanner -Dsonar.bsl.languageserver.reportPaths=./bsl-json.json
```
