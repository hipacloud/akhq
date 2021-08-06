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
import java.nio.file.Paths;

public class PickleDeserializer {

    private static PickleDeserializer INSTANCE;
    private static final String VENV_HOME = System.getenv("VENV_HOME");

    private final Value pySerde;

    public PickleDeserializer() {
        String PYTHON_EXE = Paths.get(VENV_HOME, "bin", "python").toAbsolutePath().toString();

        Context context = Context.newBuilder("python").
            allowAllAccess(true).
            option("python.ForceImportSite", "true").
            option("python.Executable", PYTHON_EXE).
            build();

        context.eval(getSerdeSource());
        pySerde = context.getPolyglotBindings().getMember("deserialize");
    }

    private Source getSerdeSource() {
        InputStream codeInputStream = getClass().getClassLoader().getResourceAsStream("serde.py");
        assert codeInputStream != null;

        try {
            try (InputStreamReader codeReader = new InputStreamReader(codeInputStream)) {
                return Source.newBuilder("python", codeReader, "serde.py").build();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
