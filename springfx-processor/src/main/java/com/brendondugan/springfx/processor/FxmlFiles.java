package com.brendondugan.springfx.processor;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Finds FXML files and reads their root element. They are looked up in the folders given by the
 * {@code springfx.resources} processor option, or, without it, in the class output.
 */
class FxmlFiles {

    private static final String FXML_NAMESPACE = "http://javafx.com/fxml";

    private final Filer filer;
    private final List<File> resourceFolders;

    /**
     * @param resourcesOption the {@code springfx.resources} option: folders separated by the
     *        platform path separator, or null to use the class output
     */
    FxmlFiles(Filer filer, String resourcesOption) {
        this.filer = filer;
        this.resourceFolders = resourcesOption == null || resourcesOption.isBlank()
                ? List.of()
                : Arrays.stream(resourcesOption.split(File.pathSeparator))
                        .filter(folder -> !folder.isBlank())
                        .map(String::strip)
                        .map(File::new)
                        .toList();
    }

    /** Where FXML is looked for, for messages. */
    String searched() {
        return resourceFolders.isEmpty()
                ? "the class output"
                : resourceFolders.stream().map(File::getPath).toList().toString();
    }

    /**
     * Reads the root element of the FXML at a class-path resource path ({@code fxml/main.fxml},
     * no leading slash).
     *
     * @return empty if there is no such file
     * @throws IOException if it can't be read or isn't well-formed XML
     */
    Optional<FxmlRoot> read(String resource) throws IOException {
        if (!resourceFolders.isEmpty()) {
            for (File folder : resourceFolders) {
                File file = new File(folder, resource);
                if (file.isFile()) {
                    try (InputStream in = new FileInputStream(file)) {
                        return Optional.of(new FxmlRoot(file.getPath(), fxController(in, file.getPath())));
                    }
                }
            }
            return Optional.empty();
        }
        FileObject file;
        InputStream in;
        try {
            file = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resource);
            in = file.openInputStream();
        } catch (NoSuchFileException | java.io.FileNotFoundException | IllegalArgumentException missing) {
            return Optional.empty();
        }
        try (in) {
            String location = file.toUri().toString();
            return Optional.of(new FxmlRoot(location, fxController(in, location)));
        }
    }

    /** The root element's {@code fx:controller}, reading no further than the root element. */
    private static String fxController(InputStream in, String location) throws IOException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String namespace = reader.getAttributeNamespace(i);
                            if ("controller".equals(reader.getAttributeLocalName(i))
                                    && namespace != null && namespace.startsWith(FXML_NAMESPACE)) {
                                return reader.getAttributeValue(i).strip();
                            }
                        }
                        return null;
                    }
                }
                throw new IOException(location + " has no root element");
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException(location + " is not well-formed XML: " + e.getMessage(), e);
        }
    }
}
