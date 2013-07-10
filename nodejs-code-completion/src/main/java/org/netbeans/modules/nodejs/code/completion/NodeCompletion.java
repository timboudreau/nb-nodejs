/* Copyright (C) 2012 Tim Boudreau

 Permission is hereby granted, free of charge, to any person obtaining a copy 
 of this software and associated documentation files (the "Software"), to 
 deal in the Software without restriction, including without limitation the 
 rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
 sell copies of the Software, and to permit persons to whom the Software is 
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all 
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. */
package org.netbeans.modules.nodejs.code.completion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.LexUtilities;
import org.netbeans.modules.javascript2.editor.spi.CompletionContext;
import org.netbeans.modules.javascript2.editor.spi.CompletionProvider;

/**
 *
 * @author Tim Boudreau
 */
@CompletionProvider.Registration(priority=10)
public class NodeCompletion implements CompletionProvider {
    private static final Logger LOGGER = Logger.getLogger(NodeCompletion.class.getName());
    private int lastTsOffset = 0;

    @Override
    public List<CompletionProposal> complete(CodeCompletionContext ccContext, CompletionContext jsCompletionContext, String prefix) {
        long start = System.currentTimeMillis();
        List<CompletionProposal> result = new ArrayList<>();
        ParserResult parserResult = ccContext.getParserResult();
        int offset = ccContext.getCaretOffset();
        lastTsOffset = ccContext.getParserResult().getSnapshot().getEmbeddedOffset(offset);
        switch (jsCompletionContext) {
            case STRING:
            case GLOBAL:
            case EXPRESSION:
            case OBJECT_PROPERTY:
                if (isInJQuerySelector(parserResult, lastTsOffset)) {
//                    addSelectors(result, parserResult, prefix, lastTsOffset);
                }
                break;
            case OBJECT_PROPERTY_NAME:
//                completeObjectPropertyName(ccContext, result, prefix);
                break;
            default:
                break;
        }
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "Counting jQuery CC took {0}ms ", (end - start));
        return result;
    }

    @Override
    public String getHelpDocumentation(ParserResult pr, ElementHandle eh) {
        return null;
    }
    
    public static boolean isInJQuerySelector(ParserResult parserResult, int offset) {
        if (isJQuery(parserResult, offset)) {
            TokenSequence<? extends JsTokenId> ts = LexUtilities.getTokenSequence(parserResult.getSnapshot().getTokenHierarchy(), offset, JsTokenId.javascriptLanguage());
            if (ts == null) {
                return false;
            }
            ts.move(offset);
            if (!(ts.moveNext() && ts.movePrevious())) {
                return false;
            }
            return ts.token().id() == JsTokenId.STRING || ts.token().id() == JsTokenId.STRING_BEGIN;
//            boolean leftBracket = false;
//            while (!isEndToken(ts.token().id()) && ts.token().id() != JsTokenId.BRACKET_RIGHT_PAREN && ts.movePrevious()) {
//                if (ts.token().id() == JsTokenId.BRACKET_LEFT_PAREN) {
//                    leftBracket = true;
//                    break;
//                }
//            }
//            if (!leftBracket) {
//                return false;
//            } else {
//                ts.move(offset);
//                if (!(ts.moveNext() && ts.movePrevious())) {
//                    return false;
//                }
//            }
//            while (!isEndToken(ts.token().id()) && ts.token().id() != JsTokenId.BRACKET_LEFT_PAREN && ts.moveNext()) {
//                if (ts.token().id() == JsTokenId.BRACKET_RIGHT_PAREN) {
//                    return true;
//                }
//            }
        }
        return false;
    }
    
    public static boolean isJQuery(ParserResult parserResult, int offset) {
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(parserResult.getSnapshot().getTokenHierarchy(), offset);
        if (ts == null) {
            return false;
        }
        ts.move(offset);
        if (!ts.moveNext() && !ts.movePrevious()) {
            return false;
        }
        
        if (ts.token().id() == JsTokenId.EOL || ts.token().id() == JsTokenId.WHITESPACE) {
            ts.movePrevious();
        }
        Token<? extends JsTokenId> lastToken = ts.token();
        Token<? extends JsTokenId> token = lastToken;
        JsTokenId tokenId = token.id();
        while (tokenId != JsTokenId.EOL
                && tokenId != JsTokenId.WHITESPACE
                && ts.movePrevious()) {
            lastToken = token;
            token = ts.token();
            tokenId = token.id();
        }
        return (lastToken.id() == JsTokenId.IDENTIFIER
                && ("$".equals(lastToken.text().toString()) || "jQuery".equals(lastToken.text().toString()))
                || (!ts.movePrevious()
                && ("$".equals(token.text().toString()) || "jQuery".equals(token.text().toString()))));
    }    

}
