package com.github._1c_syntax.sonar.bsl.language;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class BSLLanguageServerRuleDefinitionTest {

    @Test
    public void test_init() {
        Configuration config = new MapSettings().asConfig();
        BSLLanguageServerRuleDefinition ruleDefinition = new BSLLanguageServerRuleDefinition(config);
        RulesDefinition.Context context = new RulesDefinition.Context();
        ruleDefinition.define(context);
        // TODO: проверку
    }

}
