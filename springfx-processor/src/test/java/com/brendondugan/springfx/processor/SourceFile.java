package com.brendondugan.springfx.processor;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/** A Java source file held in memory. */
class SourceFile extends SimpleJavaFileObject {

    private final String code;

    SourceFile(String className, String code) {
        super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
