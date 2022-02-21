package com.github._1c_syntax.bsl.sonar.extissues;

import com.github._1c_syntax.bsl.languageserver.configuration.Language;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;
import java.util.List;

import static com.github._1c_syntax.bsl.sonar.BSLCommunityProperties.BSL_CATEGORY;

/**
 * Общий интерфейс для настроек импортеров
 */
public interface Reporter {

  /**
   * Пользовательское имя
   */
  String name();

  /**
   * Подкатегория настроек
   */
  String subcategory();

  /**
   * Ключ настроек для активизации импортера
   */
  String enabledKey();

  /**
   * Значение ключа для активизации импортера по умолчанию
   */
  boolean enableDefaultValue();

  /**
   * Ключ настроек для создания "внешние замечаний"
   */
  String createExternalIssuesKey();

  /**
   * Значение по умолчанию для создания "внешние замечаний"
   */
  boolean createExternalIssuesDefaultValue();

  /**
   * Ключ настройки путей файлов с описанием диагностик
   */
  String rulesPathsKey();

  /**
   * Путь к поставляемому файлу описания диагностик
   */
  String rulesDefaultPath();

  /**
   * Ключ репозитория диагностик
   */
  String repositoryKey();

  /**
   * Ключ-идентификатор репортера
   */
  String source();

  /**
   * Пользовательское представление репозитория диагностик
   */
  String repositoryName();

  /**
   * Имя тега, указываемого диагностикам при загрузке
   */
  String ruleTag();

  /**
   * Стартовый индекс расположения настроек в UI SQ
   */
  int startIndex();

  /**
   * Возвращает список параметров импортера для отображения в UI SQ
   */
  default List<PropertyDefinition> getProperties() {
    var index = startIndex();
    return Arrays.asList(
      PropertyDefinition.builder(enabledKey())
        .name(String.format("Enable %s rules", name()))
        .description(
          String.format("Enable %s rules. Need restart server", name())
        )
        .defaultValue(Boolean.toString(enableDefaultValue()))
        .type(PropertyType.BOOLEAN)
        .options(Language.RU.getLanguageCode(), Language.EN.getLanguageCode())
        .category(BSL_CATEGORY)
        .subCategory(subcategory())
        .onQualifiers(Qualifiers.APP)
        .index(++index)
        .build(),
      PropertyDefinition.builder(createExternalIssuesKey())
        .name(String.format("Create external issues with %s sources", source()))
        .description(
          String.format("Create external issue if no active %s rule was found", source())
        )
        .defaultValue(Boolean.toString(createExternalIssuesDefaultValue()))
        .type(PropertyType.BOOLEAN)
        .options(Language.RU.getLanguageCode(), Language.EN.getLanguageCode())
        .category(BSL_CATEGORY)
        .subCategory(subcategory())
        .onQualifiers(Qualifiers.APP, Qualifiers.PROJECT)
        .index(++index)
        .build(),
      PropertyDefinition.builder(rulesPathsKey())
        .name(String.format("%s rules path", name()))
        .description(
          String.format("Path (absolute or relative) to json file with %s rules", name())
        )
        .defaultValue("")
        .type(PropertyType.STRING)
        .category(BSL_CATEGORY)
        .subCategory(subcategory())
        .onQualifiers(Qualifiers.APP)
        .multiValues(true)
        .index(++index)
        .build()
    );
  }
}
