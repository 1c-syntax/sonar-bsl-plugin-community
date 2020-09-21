/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2020
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Gryzlov <nixel2007@gmail.com>
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * SonarQube 1C (BSL) Community Plugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * SonarQube 1C (BSL) Community Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with SonarQube 1C (BSL) Community Plugin.
 */
package com.github._1c_syntax.bsl.sonar;

import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.parser.BSLLexer;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.Token;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

import javax.annotation.Nullable;

@RequiredArgsConstructor
public class BSLHighlighter {

  private final SensorContext context;

  public void saveHighlighting(InputFile inputFile, DocumentContext documentContext) {
    NewHighlighting highlighting = context.newHighlighting().onFile(inputFile);

    documentContext.getTokens().forEach((Token token) -> {
      TypeOfText typeOfText = getTypeOfText(token.getType());

      if (typeOfText == null) {
        return;
      }

      int line = token.getLine();
      int charPositionInLine = token.getCharPositionInLine();
      String tokenText = token.getText();

      highlighting.highlight(
        line,
        charPositionInLine,
        line,
        charPositionInLine + tokenText.length(),
        typeOfText
      );
    });

    highlighting.save();
  }

  @Nullable
  private static TypeOfText getTypeOfText(int tokenType) {

    TypeOfText typeOfText = null;

    switch (tokenType) {
      case BSLLexer.PROCEDURE_KEYWORD:
      case BSLLexer.FUNCTION_KEYWORD:
      case BSLLexer.ENDPROCEDURE_KEYWORD:
      case BSLLexer.ENDFUNCTION_KEYWORD:
      case BSLLexer.EXPORT_KEYWORD:
      case BSLLexer.VAL_KEYWORD:
      case BSLLexer.ENDIF_KEYWORD:
      case BSLLexer.ENDDO_KEYWORD:
      case BSLLexer.IF_KEYWORD:
      case BSLLexer.ELSIF_KEYWORD:
      case BSLLexer.ELSE_KEYWORD:
      case BSLLexer.THEN_KEYWORD:
      case BSLLexer.WHILE_KEYWORD:
      case BSLLexer.DO_KEYWORD:
      case BSLLexer.FOR_KEYWORD:
      case BSLLexer.TO_KEYWORD:
      case BSLLexer.EACH_KEYWORD:
      case BSLLexer.IN_KEYWORD:
      case BSLLexer.TRY_KEYWORD:
      case BSLLexer.EXCEPT_KEYWORD:
      case BSLLexer.ENDTRY_KEYWORD:
      case BSLLexer.RETURN_KEYWORD:
      case BSLLexer.CONTINUE_KEYWORD:
      case BSLLexer.RAISE_KEYWORD:
      case BSLLexer.VAR_KEYWORD:
      case BSLLexer.NOT_KEYWORD:
      case BSLLexer.OR_KEYWORD:
      case BSLLexer.AND_KEYWORD:
      case BSLLexer.NEW_KEYWORD:
      case BSLLexer.GOTO_KEYWORD:
      case BSLLexer.BREAK_KEYWORD:
      case BSLLexer.EXECUTE_KEYWORD:
      case BSLLexer.ADDHANDLER_KEYWORD:
      case BSLLexer.REMOVEHANDLER_KEYWORD:
        typeOfText = TypeOfText.KEYWORD;
        break;
      case BSLLexer.SEMICOLON:
      case BSLLexer.QUESTION:
      case BSLLexer.PLUS:
      case BSLLexer.MINUS:
      case BSLLexer.MUL:
      case BSLLexer.QUOTIENT:
      case BSLLexer.MODULO:
      case BSLLexer.ASSIGN:
      case BSLLexer.LESS_OR_EQUAL:
      case BSLLexer.LESS:
      case BSLLexer.NOT_EQUAL:
      case BSLLexer.GREATER_OR_EQUAL:
      case BSLLexer.GREATER:
      case BSLLexer.COMMA:
      case BSLLexer.COLON:
      case BSLLexer.TILDA:
        typeOfText = TypeOfText.KEYWORD_LIGHT;
        break;
      case BSLLexer.TRUE:
      case BSLLexer.FALSE:
      case BSLLexer.UNDEFINED:
      case BSLLexer.NULL:
      case BSLLexer.DATETIME:
      case BSLLexer.DECIMAL:
      case BSLLexer.FLOAT:
        typeOfText = TypeOfText.CONSTANT;
        break;
      case BSLLexer.STRING:
      case BSLLexer.STRINGSTART:
      case BSLLexer.STRINGPART:
      case BSLLexer.STRINGTAIL:
      case BSLLexer.PREPROC_STRING:
      case BSLLexer.PREPROC_STRINGSTART:
      case BSLLexer.PREPROC_STRINGTAIL:
      case BSLLexer.PREPROC_STRINGPART:
        typeOfText = TypeOfText.STRING;
        break;
      case BSLLexer.LINE_COMMENT:
      case BSLLexer.PREPROC_LINE_COMMENT:
        typeOfText = TypeOfText.COMMENT;
        break;
      case BSLLexer.HASH:
      case BSLLexer.PREPROC_USE_KEYWORD:
      case BSLLexer.PREPROC_REGION:
      case BSLLexer.PREPROC_END_REGION:
      case BSLLexer.PREPROC_AND_KEYWORD:
      case BSLLexer.PREPROC_OR_KEYWORD:
      case BSLLexer.PREPROC_NOT_KEYWORD:
      case BSLLexer.PREPROC_IF_KEYWORD:
      case BSLLexer.PREPROC_THEN_KEYWORD:
      case BSLLexer.PREPROC_ELSIF_KEYWORD:
      case BSLLexer.PREPROC_ELSE_KEYWORD:
      case BSLLexer.PREPROC_ENDIF_KEYWORD:
      case BSLLexer.PREPROC_EXCLAMATION_MARK:
      case BSLLexer.PREPROC_LPAREN:
      case BSLLexer.PREPROC_RPAREN:
      case BSLLexer.PREPROC_MOBILEAPPCLIENT_SYMBOL:
      case BSLLexer.PREPROC_MOBILEAPPSERVER_SYMBOL:
      case BSLLexer.PREPROC_MOBILECLIENT_SYMBOL:
      case BSLLexer.PREPROC_THICKCLIENTORDINARYAPPLICATION_SYMBOL:
      case BSLLexer.PREPROC_THICKCLIENTMANAGEDAPPLICATION_SYMBOL:
      case BSLLexer.PREPROC_EXTERNALCONNECTION_SYMBOL:
      case BSLLexer.PREPROC_THINCLIENT_SYMBOL:
      case BSLLexer.PREPROC_WEBCLIENT_SYMBOL:
      case BSLLexer.PREPROC_ATCLIENT_SYMBOL:
      case BSLLexer.PREPROC_CLIENT_SYMBOL:
      case BSLLexer.PREPROC_ATSERVER_SYMBOL:
      case BSLLexer.PREPROC_SERVER_SYMBOL:
      case BSLLexer.PREPROC_INSERT_SYMBOL:
      case BSLLexer.PREPROC_ENDINSERT_SYMBOL:
      case BSLLexer.PREPROC_DELETE_SYMBOL:
      case BSLLexer.PREPROC_ENDDELETE_SYMBOL:
      case BSLLexer.PREPROC_IDENTIFIER:
      case BSLLexer.PREPROC_ANY:
        typeOfText = TypeOfText.PREPROCESS_DIRECTIVE;
        break;
      case BSLLexer.AMPERSAND:
      case BSLLexer.ANNOTATION_AFTER_SYMBOL:
      case BSLLexer.ANNOTATION_AROUND_SYMBOL:
      case BSLLexer.ANNOTATION_ATCLIENT_SYMBOL:
      case BSLLexer.ANNOTATION_ATCLIENTATSERVER_SYMBOL:
      case BSLLexer.ANNOTATION_ATCLIENTATSERVERNOCONTEXT_SYMBOL:
      case BSLLexer.ANNOTATION_ATSERVER_SYMBOL:
      case BSLLexer.ANNOTATION_ATSERVERNOCONTEXT_SYMBOL:
      case BSLLexer.ANNOTATION_BEFORE_SYMBOL:
      case BSLLexer.ANNOTATION_CHANGEANDVALIDATE_SYMBOL:
      case BSLLexer.ANNOTATION_CUSTOM_SYMBOL:
        typeOfText = TypeOfText.ANNOTATION;
        break;
      default:
        // no-op
    }

    return typeOfText;

  }
}
