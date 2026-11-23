package com.brendondugan.springfx.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks SpringFX Controllers at compile time (ADR-0003) and generates the {@code Views} enum of
 * their View Names (ADR-0004). Compilation fails when a Controller's FXML is missing, its
 * {@code fx:controller} names another class, its View Name is malformed or taken, {@code Views}
 * couldn't refer to it, more than one Controller is primary, or the module has a
 * {@code SpringFXApplication} but no primary Controller.
 *
 * <p>FXML is read from the folders in the {@code springfx.resources} option (point it at
 * {@code src/main/resources}), else from the class output. {@code Views} goes in the package of the
 * module's SpringFX Application, or the one in the {@code springfx.viewsPackage} option. From JDK 23
 * the processor must be listed on the compiler's processor path.
 */
public class SpringFXProcessor extends AbstractProcessor {

    /** The processor option naming the folders FXML is read from. */
    public static final String RESOURCES_OPTION = "springfx.resources";

    /** The processor option naming the package {@code Views} is generated in. */
    public static final String VIEWS_PACKAGE_OPTION = "springfx.viewsPackage";

    private static final String CONTROLLER_ANNOTATION = "com.brendondugan.springfx.SpringFXController";
    private static final String APPLICATION_CLASS = "com.brendondugan.springfx.SpringFXApplication";
    private static final String GENERATED_ANNOTATION = "javax.annotation.processing.Generated";
    private static final Pattern VIEW_NAME = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");

    private Messager messager;
    private Elements elements;
    private Types types;
    private Filer filer;
    private FxmlFiles fxmlFiles;
    private ModuleIndex index;
    private String viewsPackageOption;

    /** Every Controller compiled in this compilation, across rounds, by class name. */
    private final Map<String, ControllerDeclaration> compiled = new LinkedHashMap<>();
    /** Every SpringFX Application class compiled in this compilation, across rounds. */
    private final List<TypeElement> applications = new ArrayList<>();
    private boolean viewsHandled;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        // Every round, not only those with Controllers: the application class carries no
        // SpringFX annotation but still needs a primary Controller.
        return Set.of("*");
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(RESOURCES_OPTION, VIEWS_PACKAGE_OPTION);
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
        filer = processingEnv.getFiler();
        fxmlFiles = new FxmlFiles(filer, processingEnv.getOptions().get(RESOURCES_OPTION));
        index = new ModuleIndex(filer);
        String option = processingEnv.getOptions().get(VIEWS_PACKAGE_OPTION);
        viewsPackageOption = option == null || option.isBlank() ? null : option.strip();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        TypeElement controllerAnnotation = elements.getTypeElement(CONTROLLER_ANNOTATION);
        if (controllerAnnotation == null) {
            // springfx isn't on the class path: nothing here can be a Controller.
            return false;
        }
        if (round.processingOver()) {
            checkPrimaryControllers();
            writeIndex();
            return false;
        }
        for (TypeElement type : ElementFilter.typesIn(round.getElementsAnnotatedWith(controllerAnnotation))) {
            ControllerDeclaration controller = declaration(type).orElseThrow();
            compiled.put(controller.name(), controller);
            checkFxml(controller);
        }
        TypeElement applicationClass = elements.getTypeElement(APPLICATION_CLASS);
        if (applicationClass != null) {
            for (TypeElement type : ElementFilter.typesIn(round.getRootElements())) {
                if (isApplication(type)) {
                    applications.add(type);
                }
            }
        }
        if (!viewsHandled) {
            // The first round sees every hand-written source. Generating Views now, not in the
            // last round, lets javac compile it without a warning.
            viewsHandled = true;
            generateViews();
        }
        return false;
    }

    // --- FXML (ADR-0003) ---

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

    // --- View Names and Views (ADR-0004) ---

    private void generateViews() {
        Map<String, ControllerDeclaration> module = moduleControllers(false);
        if (module.isEmpty()) {
            return;
        }
        boolean valid = true;

        for (ControllerDeclaration controller : compiled.values()) {
            if (!VIEW_NAME.matcher(controller.viewName()).matches()) {
                error(controller, "View Name \"" + controller.viewName() + "\" of Controller " + controller.name()
                        + " must be UPPER_SNAKE_CASE, such as LOGIN_PAGE: capital letters and digits in words"
                        + " joined by single underscores, starting with a letter");
                valid = false;
            }
        }

        Map<String, List<ControllerDeclaration>> byViewName = module.values().stream()
                .filter(controller -> VIEW_NAME.matcher(controller.viewName()).matches())
                .collect(Collectors.groupingBy(ControllerDeclaration::viewName, TreeMap::new, Collectors.toList()));
        for (Map.Entry<String, List<ControllerDeclaration>> entry : byViewName.entrySet()) {
            if (entry.getValue().size() > 1) {
                valid = false;
                List<String> names = entry.getValue().stream().map(ControllerDeclaration::name).toList();
                entry.getValue().stream()
                        .filter(controller -> compiled.containsKey(controller.name()))
                        .forEach(controller -> error(controller, "View Name " + entry.getKey()
                                + " is used by more than one Controller: " + names
                                + "; each View Name must be unique within the module"));
            }
        }

        Optional<String> pkg = viewsPackage();
        if (pkg.isEmpty()) {
            return;
        }
        for (ControllerDeclaration controller : compiled.values()) {
            if (!reachableFrom(controller.type(), pkg.get())) {
                error(controller, "Controller " + controller.name() + " can't be referred to from "
                        + qualified(pkg.get(), ViewsSource.SIMPLE_NAME) + ": make it (and any class it is nested in)"
                        + " public, or generate Views in its package with -A" + VIEWS_PACKAGE_OPTION + "="
                        + elements.getPackageOf(controller.type()).getQualifiedName());
                valid = false;
            }
        }

        if (!valid || elements.getTypeElement(ViewsSource.VIEW_NAME_INTERFACE) == null) {
            // Don't generate a Views that wouldn't compile, or for a springfx without ViewName.
            return;
        }
        List<ControllerDeclaration> views = module.values().stream()
                .filter(controller -> VIEW_NAME.matcher(controller.viewName()).matches())
                .toList();
        Element[] originating = views.stream().map(ControllerDeclaration::type).toArray(Element[]::new);
        try {
            JavaFileObject file = filer.createSourceFile(qualified(pkg.get(), ViewsSource.SIMPLE_NAME), originating);
            try (Writer writer = file.openWriter()) {
                writer.write(ViewsSource.of(pkg.get(), views, elements.getTypeElement(GENERATED_ANNOTATION) != null));
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "SpringFX could not generate "
                    + qualified(pkg.get(), ViewsSource.SIMPLE_NAME) + ": " + e.getMessage());
        }
    }

    /** The package Views goes in, or empty after reporting why there is none. */
    private Optional<String> viewsPackage() {
        if (viewsPackageOption != null) {
            return Optional.of(viewsPackageOption);
        }
        Set<String> packages = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();
        for (TypeElement application : moduleApplications()) {
            packages.add(elements.getPackageOf(application).getQualifiedName().toString());
            names.add(application.getQualifiedName().toString());
        }
        if (packages.size() == 1) {
            return Optional.of(packages.iterator().next());
        }
        String problem = packages.isEmpty()
                ? "This module has Controllers but no SpringFX Application, so SpringFX doesn't know which"
                        + " package to generate Views in"
                : "This module's SpringFX Applications " + names + " are in different packages, so SpringFX"
                        + " doesn't know which one to generate Views in";
        String message = problem + ": set -A" + VIEWS_PACKAGE_OPTION + "=<package>";
        compiled.values().stream().findFirst().ifPresentOrElse(
                controller -> error(controller, message),
                () -> messager.printMessage(Diagnostic.Kind.ERROR, message));
        return Optional.empty();
    }

    /** Whether code in the given package can name the type, as Views must. */
    private boolean reachableFrom(TypeElement type, String pkg) {
        boolean samePackage = elements.getPackageOf(type).getQualifiedName().contentEquals(pkg);
        for (Element element = type; element instanceof TypeElement enclosing; element = enclosing.getEnclosingElement()) {
            Set<Modifier> modifiers = enclosing.getModifiers();
            if (modifiers.contains(Modifier.PRIVATE) || (!modifiers.contains(Modifier.PUBLIC) && !samePackage)) {
                return false;
            }
        }
        return true;
    }

    // --- Primary Controller ---

    private void checkPrimaryControllers() {
        List<String> primaries = moduleControllers(true).values().stream()
                .filter(ControllerDeclaration::primary)
                .map(ControllerDeclaration::name)
                .toList();
        if (primaries.size() > 1) {
            for (String primary : primaries) {
                ControllerDeclaration controller = compiled.get(primary);
                if (controller != null) {
                    error(current(controller), "More than one primary Controller: " + primaries
                            + "; mark exactly one @SpringFXController with primary = true");
                }
            }
        }
        if (primaries.isEmpty()) {
            for (TypeElement application : applications) {
                messager.printMessage(Diagnostic.Kind.ERROR, "SpringFX Application "
                        + application.getQualifiedName() + " has no primary Controller: mark exactly one"
                        + " @SpringFXController with primary = true to choose the Primary View", application);
            }
        }
    }

    // --- The module across incremental builds ---

    /**
     * The module's Controllers: those compiled now, then those recorded by an earlier compilation
     * whose classes weren't recompiled and are still Controllers.
     *
     * @param fresh look compiled Controllers up again, as the last round needs
     */
    private Map<String, ControllerDeclaration> moduleControllers(boolean fresh) {
        Map<String, ControllerDeclaration> module = new LinkedHashMap<>();
        compiled.values().forEach(controller -> module.put(controller.name(), fresh ? current(controller) : controller));
        for (String name : index.controllers()) {
            if (!module.containsKey(name)) {
                TypeElement type = elements.getTypeElement(name);
                if (type != null) {
                    declaration(type).ifPresent(controller -> module.put(name, controller));
                }
            }
        }
        return module;
    }

    /** The module's SpringFX Application classes, compiled now or recorded earlier. */
    private List<TypeElement> moduleApplications() {
        Map<String, TypeElement> module = new LinkedHashMap<>();
        applications.forEach(application -> module.put(application.getQualifiedName().toString(), application));
        for (String name : index.applications()) {
            TypeElement type = elements.getTypeElement(name);
            if (!module.containsKey(name) && type != null && isApplication(type)) {
                module.put(name, type);
            }
        }
        return List.copyOf(module.values());
    }

    private void writeIndex() {
        Set<String> controllers = moduleControllers(true).keySet();
        List<String> applicationNames = moduleApplications().stream()
                .map(application -> application.getQualifiedName().toString())
                .toList();
        if (controllers.isEmpty() && applicationNames.isEmpty() && index.isEmpty()) {
            return;
        }
        try {
            index.write(controllers, applicationNames);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.NOTE, "SpringFX could not record this module's Controllers in "
                    + ModuleIndex.RESOURCE + ": " + e.getMessage());
        }
    }

    // --- Helpers ---

    /**
     * The same Controller as seen in this round. Elements and annotations kept from an earlier
     * round can't be traced back to their source position, so javac would report on the class
     * instead of the annotation.
     */
    private ControllerDeclaration current(ControllerDeclaration controller) {
        TypeElement type = elements.getTypeElement(controller.name());
        return type == null ? controller : declaration(type).orElse(controller);
    }

    private Optional<ControllerDeclaration> declaration(TypeElement type) {
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
            if (annotationType.getQualifiedName().contentEquals(CONTROLLER_ANNOTATION)) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                        elements.getElementValuesWithDefaults(mirror);
                String value = "";
                String fxml = "";
                String viewName = "";
                boolean primary = false;
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                    Object attributeValue = entry.getValue().getValue();
                    switch (entry.getKey().getSimpleName().toString()) {
                        case "value" -> value = (String) attributeValue;
                        case "fxml" -> fxml = (String) attributeValue;
                        case "viewName" -> viewName = String.valueOf(attributeValue);
                        case "primary" -> primary = (Boolean) attributeValue;
                        default -> { }
                    }
                }
                return Optional.of(new ControllerDeclaration(type, mirror, value.isBlank() ? fxml : value,
                        viewName, primary));
            }
        }
        return Optional.empty();
    }

    private boolean isApplication(TypeElement type) {
        TypeElement applicationClass = elements.getTypeElement(APPLICATION_CLASS);
        return applicationClass != null
                && type.getKind() == ElementKind.CLASS
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

    private static String qualified(String pkg, String simpleName) {
        return pkg.isEmpty() ? simpleName : pkg + "." + simpleName;
    }

    private void error(ControllerDeclaration controller, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, controller.type(), controller.annotation());
    }
}
