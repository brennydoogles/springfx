package com.brendondugan.springfx.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Checks SpringFX Controllers at compile time (ADR-0003). Compilation fails when a Controller's
 * FXML is missing, its {@code fx:controller} names another class, more than one Controller is
 * primary, or the module has a {@code SpringFXApplication} but no primary Controller.
 *
 * <p>FXML is read from the folders in the {@code springfx.resources} option (point it at
 * {@code src/main/resources}), else from the class output. From JDK 23 the processor must be
 * listed on the compiler's processor path.
 */
public class SpringFXProcessor extends AbstractProcessor {

    /** The processor option naming the folders FXML is read from. */
    public static final String RESOURCES_OPTION = "springfx.resources";

    private static final String CONTROLLER_ANNOTATION = "com.brendondugan.springfx.SpringFXController";
    private static final String APPLICATION_CLASS = "com.brendondugan.springfx.SpringFXApplication";

    private Messager messager;
    private Elements elements;
    private Types types;
    private FxmlFiles fxmlFiles;
    private PrimaryControllerIndex index;

    /** Every Controller compiled in this compilation, across rounds. */
    private final List<ControllerDeclaration> controllers = new ArrayList<>();
    /** Every SpringFXApplication subclass compiled in this compilation, across rounds. */
    private final List<TypeElement> applications = new ArrayList<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        // Every round, not only those with Controllers: the application class carries no
        // SpringFX annotation but still needs a primary Controller.
        return Set.of("*");
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(RESOURCES_OPTION);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        fxmlFiles = new FxmlFiles(processingEnv.getFiler(), processingEnv.getOptions().get(RESOURCES_OPTION));
        index = new PrimaryControllerIndex(processingEnv.getFiler());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        TypeElement controllerAnnotation = elements.getTypeElement(CONTROLLER_ANNOTATION);
        TypeElement applicationClass = elements.getTypeElement(APPLICATION_CLASS);
        if (controllerAnnotation == null) {
            // springfx isn't on the class path: nothing here can be a Controller.
            return false;
        }
        if (round.processingOver()) {
            checkPrimaryControllers();
            return false;
        }
        for (TypeElement type : ElementFilter.typesIn(round.getElementsAnnotatedWith(controllerAnnotation))) {
            ControllerDeclaration controller = declaration(type).orElseThrow();
            controllers.add(controller);
            checkFxml(controller);
        }
        if (applicationClass != null) {
            for (TypeElement type : ElementFilter.typesIn(round.getRootElements())) {
                if (isApplication(type, applicationClass)) {
                    applications.add(type);
                }
            }
        }
        return false;
    }

    private void checkFxml(ControllerDeclaration controller) {
        if (controller.fxml().isBlank()) {
            error(controller, "Controller " + controller.name() + " does not name its FXML:"
                    + " set @SpringFXController(\"/path/to/view.fxml\")");
            return;
        }
        String resource = resourcePath(controller);
        Optional<FxmlRoot> fxml;
        try {
            fxml = fxmlFiles.read(resource);
        } catch (IOException e) {
            error(controller, "Could not read FXML " + controller.fxml() + " of Controller " + controller.name()
                    + ": " + e.getMessage());
            return;
        }
        if (fxml.isEmpty()) {
            error(controller, "FXML " + controller.fxml() + " of Controller " + controller.name()
                    + " was not found: looked for " + resource + " in " + fxmlFiles.searched()
                    + (fxmlFiles.searched().equals("the class output")
                            ? "; set -A" + RESOURCES_OPTION + "=<your resources folder> to read it from source"
                            : ""));
            return;
        }
        String declared = fxml.get().fxController();
        if (declared != null && !declared.equals(binaryName(controller.type()))) {
            error(controller, "FXML " + fxml.get().location() + " declares fx:controller=\"" + declared
                    + "\" but is the View of Controller " + controller.name() + "; fx:controller must name "
                    + binaryName(controller.type()) + " or be left out");
        }
    }

    private void checkPrimaryControllers() {
        Set<String> compiled = new LinkedHashSet<>();
        controllers.forEach(controller -> compiled.add(controller.name()));
        List<ControllerDeclaration> primaries = controllers.stream().filter(ControllerDeclaration::primary).toList();

        // Primaries from earlier compilations whose classes weren't recompiled this time.
        List<String> recorded = index.read();
        List<String> earlierPrimaries = recorded.stream()
                .filter(name -> !compiled.contains(name))
                .filter(this::isStillPrimary)
                .toList();

        Set<String> allPrimaries = new LinkedHashSet<>();
        primaries.forEach(primary -> allPrimaries.add(primary.name()));
        allPrimaries.addAll(earlierPrimaries);

        if (allPrimaries.size() > 1) {
            for (ControllerDeclaration primary : primaries) {
                error(current(primary), "More than one primary Controller: " + List.copyOf(allPrimaries)
                        + "; mark exactly one @SpringFXController with primary = true");
            }
        }
        if (allPrimaries.isEmpty()) {
            for (TypeElement application : applications) {
                messager.printMessage(Diagnostic.Kind.ERROR, "SpringFX Application "
                        + application.getQualifiedName() + " has no primary Controller: mark exactly one"
                        + " @SpringFXController with primary = true to choose the Primary View", application);
            }
        }
        if (!allPrimaries.isEmpty() || !recorded.isEmpty()) {
            try {
                index.write(allPrimaries);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.NOTE, "SpringFX could not record the primary Controller in "
                        + PrimaryControllerIndex.RESOURCE + ": " + e.getMessage());
            }
        }
    }

    /**
     * The same Controller as seen in this round. Elements and annotations kept from an earlier
     * round can't be traced back to their source position, so javac would report on the class
     * instead of the annotation.
     */
    private ControllerDeclaration current(ControllerDeclaration controller) {
        TypeElement type = elements.getTypeElement(controller.name());
        return type == null ? controller : declaration(type).orElse(controller);
    }

    /** Whether an already compiled class is still a primary Controller. */
    private boolean isStillPrimary(String name) {
        TypeElement type = elements.getTypeElement(name);
        return type != null && declaration(type).map(ControllerDeclaration::primary).orElse(false);
    }

    private Optional<ControllerDeclaration> declaration(TypeElement type) {
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
            if (annotationType.getQualifiedName().contentEquals(CONTROLLER_ANNOTATION)) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                        elements.getElementValuesWithDefaults(mirror);
                String value = "";
                String fxml = "";
                boolean primary = false;
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                    String attribute = entry.getKey().getSimpleName().toString();
                    Object attributeValue = entry.getValue().getValue();
                    switch (attribute) {
                        case "value" -> value = (String) attributeValue;
                        case "fxml" -> fxml = (String) attributeValue;
                        case "primary" -> primary = (Boolean) attributeValue;
                        default -> { }
                    }
                }
                return Optional.of(new ControllerDeclaration(type, mirror, value.isBlank() ? fxml : value, primary));
            }
        }
        return Optional.empty();
    }

    private boolean isApplication(TypeElement type, TypeElement applicationClass) {
        return type.getKind() == ElementKind.CLASS
                && !type.getModifiers().contains(Modifier.ABSTRACT)
                && !type.equals(applicationClass)
                && types.isSubtype(types.erasure(type.asType()), types.erasure(applicationClass.asType()));
    }

    /** The class-path resource path of a Controller's FXML: absolute, or relative to its package. */
    private String resourcePath(ControllerDeclaration controller) {
        String fxml = controller.fxml().strip();
        if (fxml.startsWith("/")) {
            return fxml.substring(1);
        }
        PackageElement pkg = elements.getPackageOf(controller.type());
        String base = pkg.isUnnamed() ? "/" : "/" + pkg.getQualifiedName().toString().replace('.', '/') + "/";
        try {
            return URI.create(base).resolve(fxml).getPath().substring(1);
        } catch (IllegalArgumentException e) {
            return base.substring(1) + fxml;
        }
    }

    /** The name FXMLLoader expects in fx:controller: the binary name, with {@code $} for nested classes. */
    private String binaryName(TypeElement type) {
        return elements.getBinaryName(type).toString();
    }

    private void error(ControllerDeclaration controller, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, controller.type(), controller.annotation());
    }
}
