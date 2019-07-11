package com.github._1c_syntax.sonar.bsl.language;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BSLLanguageTest {

    @Test
    public void test_create() {

        BSLLanguage language = new BSLLanguage();

        assertThat(language.getKey()).containsIgnoringCase("bsl");
        assertThat(language.getName()).containsIgnoringCase("1C (BSL)");
        assertThat(language.getFileSuffixes()).contains(new String[]{"bsl", "os"});

    }

}
