package com.brendondugan.springfx.internal;

import com.brendondugan.springfx.SpringFXController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads a View from its Controller's FXML. The Controller is created by Spring, and so is every
 * Controller of a nested {@code <fx:include>}.
 */
class ViewLoader {

    private static final String FXML_NAMESPACE = "http://javafx.com/fxml";

    private final ApplicationContext context;

    ViewLoader(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Loads the View of the given Controller.
     *
     * @throws IllegalStateException if the class isn't a Controller, its FXML is missing, its
     *         {@code fx:controller} names another class, or the loader didn't use the Spring-created
     *         Controller
     */
    LoadedView load(Class<?> controllerType) {
        SpringFXController annotation = AnnotatedElementUtils.findMergedAnnotation(controllerType,
                SpringFXController.class);
        if (annotation == null) {
            throw new IllegalStateException(controllerType.getName() + " is not annotated @SpringFXController");
        }
        String fxml = annotation.value();
        if (fxml.isBlank()) {
            throw new IllegalStateException("Controller " + controllerType.getName()
                    + " does not name its FXML: set @SpringFXController(\"/path/to/view.fxml\")");
        }
        URL url = controllerType.getResource(fxml);
        if (url == null) {
            throw new IllegalStateException("FXML " + fxml + " of Controller " + controllerType.getName()
                    + " was not found on the class path");
        }
        String declared = declaredController(url, fxml);
        if (declared != null && !declared.equals(controllerType.getName())) {
            throw new IllegalStateException("FXML " + fxml + " declares fx:controller=\"" + declared
                    + "\" but is the View of Controller " + controllerType.getName()
                    + "; fx:controller must name " + controllerType.getName() + " or be left out");
        }

        Object controller = context.getBean(controllerType);
        FXMLLoader loader = new FXMLLoader(url);
        AtomicBoolean rootControllerSupplied = new AtomicBoolean(declared == null);
        if (declared == null) {
            loader.setController(controller);
        }
        loader.setControllerFactory(type -> {
            if (type == controllerType && rootControllerSupplied.compareAndSet(false, true)) {
                return controller;
            }
            return nestedController(type);
        });

        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load FXML " + fxml + " of Controller "
                    + controllerType.getName(), e);
        }
        Object used = loader.getController();
        if (used != controller) {
            throw new IllegalStateException("FXML " + fxml + " was bound to "
                    + (used == null ? "no Controller" : "a " + used.getClass().getName() + " SpringFX did not create")
                    + " instead of the Spring-created Controller " + controllerType.getName());
        }
        return new LoadedView(root, controller, annotation.title());
    }

    /** A nested Controller: the Spring bean of that type, or a new autowired instance if there is none. */
    private <T> T nestedController(Class<T> type) {
        return context.getBeanProvider(type)
                .getIfAvailable(() -> context.getAutowireCapableBeanFactory().createBean(type));
    }

    /** The class named by the FXML's root {@code fx:controller} attribute, or null. */
    private static String declaredController(URL url, String fxml) {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try (InputStream in = url.openStream()) {
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
                return null;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read FXML " + fxml, e);
        } catch (XMLStreamException e) {
            throw new IllegalStateException("FXML " + fxml + " is not well-formed XML", e);
        }
    }
}
