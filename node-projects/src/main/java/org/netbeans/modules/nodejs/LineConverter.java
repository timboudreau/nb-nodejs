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
package org.netbeans.modules.nodejs;

import java.awt.Toolkit;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.extexecution.ExecutionDescriptor.LineConvertorFactory;
import org.netbeans.api.extexecution.print.ConvertedLine;
import org.netbeans.api.extexecution.print.LineConvertor;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.text.Line.ShowOpenType;
import org.openide.text.Line.ShowVisibilityType;
import org.openide.util.Exceptions;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

/**
 *
 * @author Tim Boudreau
 */
final class LineConverter implements LineConvertorFactory {

    @Override
    public LineConvertor newLineConvertor () {
        return new LineConvertor() {
            @Override
            public List<ConvertedLine> convert ( String line ) {
                Matcher m = ERR_PATTERN.matcher( line );
                OutputListener ol = null;
                try {
                    if (m.find()) {
                        String clazz = m.group( 1 );
                        String path = m.group( 2 );
                        int lineNumber = Integer.parseInt( m.group( 3 ) );
                        int charPos = Integer.parseInt( m.group( 4 ) );
                        ol = new Link( clazz, path, lineNumber, charPos );
                    } else {
                        m = SYNTAX_ERR_PATTERN.matcher( line );
                        if (m.find()) {
                            String clazz = null;
                            String path = m.group( 1 );
                            int lineNumber = Integer.parseInt( m.group( 2 ) );
                            int charPos = 0;
                            ol = new Link( clazz, path, lineNumber, charPos );
                        }
                    }
                } catch ( NumberFormatException nfe ) {
                    //do nothing - some output looked like a stack element by accident
                }
                return Collections.singletonList( ConvertedLine.forText( line, ol ) );
            }
        };
    }
    private static final Pattern ERR_PATTERN =
            Pattern.compile( "at\\s(.*?)\\s\\((.*?.js):(\\d+):(\\d+)\\)" );
    //e.g. at Server.<anonymous> (/home/tim/Fooger/src/Fooger.js:7:5)
    private static final Pattern SYNTAX_ERR_PATTERN =
            Pattern.compile( "(\\/.*?\\.js):(\\d+)" );
    //e.g. /home/tim/work/personal/captcha/captcha.js:38

    private static class Link implements OutputListener {
        private final String path;
        private final String clazz;
        private final int line;
        private final int charPos;

        Link ( String clazz, String path, int line, int charPos ) {
            this.clazz = clazz;
            this.path = path;
            this.line = line;
            this.charPos = charPos;
        }

        @Override
        public void outputLineSelected ( OutputEvent ev ) {
            //do nothing
        }

        @Override
        public void outputLineAction ( OutputEvent ev ) {
            String pathLocal = this.path;
            if (pathLocal.indexOf( '/' ) < 0) { //NOI18N
                String sourcePath = DefaultExecutable.get().getSourcesLocation();
                if (sourcePath != null) {
                    File f = new File( sourcePath );
                    f = new File( f, pathLocal );
                    if (!f.exists() && new File( f, "lib" ).exists()) { //NOI18N
                        f = new File( f, "lib" );
                        f = new File( f, pathLocal );
                    }
                    pathLocal = f.getAbsolutePath();
                }
            }
            File f = new File( pathLocal );
            if (f.exists()) {
                FileObject fo = FileUtil.toFileObject( f );
                try {
                    DataObject dob = DataObject.find( fo );
                    EditorCookie ck = dob.getLookup().lookup( EditorCookie.class );
                    if (ck != null) {
                        LineCookie l = dob.getLookup().lookup( LineCookie.class );
                        if (l != null) {
                            Line goTo = l.getLineSet().getCurrent( Math.max (0, line -1) );
                            if (goTo == null) {
                                goTo =  l.getLineSet().getOriginal(line -1);
                            }
                            if (goTo != null) {
                                String txt = goTo.getText();
                                int length = txt == null ? -1 : txt.length();
                                int position = charPos >= length && txt != null ? 0 : charPos;
                                goTo.show( ShowOpenType.REUSE_NEW, 
                                    ShowVisibilityType.FOCUS, position );
                            } else {
                                Logger.getLogger(LineConvertor.class.getName()).log(
                                        Level.WARNING, 
                                        "Could not go to line {0} of {1}", 
                                        new Object[]{line - 1, fo.getPath()});
                            }
                        }
                    }
                } catch ( DataObjectNotFoundException ex ) {
                    Exceptions.printStackTrace( ex );
                }
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        @Override
        public void outputLineCleared ( OutputEvent ev ) {
            //do nothing
        }

        public String toString () {
            return path + " line " + line + " pos " + charPos;
        }
    }
}
