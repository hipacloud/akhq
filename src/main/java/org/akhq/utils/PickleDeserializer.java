package org.akhq.utils;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public class PickleDeserializer {

    private static PickleDeserializer INSTANCE;

    private static String PYTHON_EXECUTABLE = PickleDeserializer.class.getClassLoader()
        .getResource("venv/bin/python").getPath();

    private Value pySerde;

    public PickleDeserializer() {
        Context context = Context.newBuilder("python").
            allowAllAccess(true).
            option("python.ForceImportSite", "true").
            option("python.Executable", PYTHON_EXECUTABLE).
            build();

        Source source;
        try {
            InputStream codeInputStream = PickleDeserializer.class.getClassLoader().getResourceAsStream("serde.py");
            InputStreamReader codeReader = new InputStreamReader(codeInputStream);
            source = Source.newBuilder("python", codeReader, "serde.py").build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.eval(source);
        pySerde = context.getPolyglotBindings().getMember("deserialize");
    }

    public String deserialize(byte[] payload) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        try {
            return decoder.decode(ByteBuffer.wrap(payload)).toString();
        } catch (CharacterCodingException ex) {
            // ignore
        }

        try {
            return pySerde.execute((Object) payload).asString();
        } catch (PolyglotException e) {
            return new String(payload);
        }
    }

    public static PickleDeserializer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PickleDeserializer();
        }
        return INSTANCE;
    }
}
