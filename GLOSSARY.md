# SpringFX

A library that lets a Spring Boot application drive a JavaFX user interface: Spring owns the objects, JavaFX owns the screen, and SpringFX connects the two so an app needs one application class and no JavaFX plumbing of its own.

## Language

**SpringFX Application**:
The consuming app's single entry-point class. It starts Spring and JavaFX together and opens the Primary View.
_Avoid_: launcher, FX application, bootstrap class

**View**:
One piece of user interface defined by an FXML file together with its Controller.
_Avoid_: screen, page, scene, form

**View Name**:
The short name, chosen by the developer, that code uses to refer to a View, such as `LOGIN_PAGE`. Every View has exactly one, unique within the app.
_Avoid_: friendly name, view key, view id, bean name

**Controller**:
The Spring-managed object behind a View. A Controller names its View's FXML file, not the other way round, and a fresh Controller is made each time its View is shown.
_Avoid_: view controller, presenter, handler

**Primary View**:
The one View shown when the application starts. Exactly one Controller in an app is marked primary.
_Avoid_: main view, root view, home screen

**Window**:
A top-level window on screen showing one View. Closing the Primary Window ends the application.
_Avoid_: stage (that's the JavaFX class behind a Window), dialog, frame

**Primary Window**:
The Window that shows the Primary View.
_Avoid_: main window, root window

**Window Settings**:
The configured starting state of Windows: title, size, resizability, maximised and full-screen state, stylesheets and icons. Size and state apply to the Primary Window only; stylesheets and icons apply to every Window.
_Avoid_: stage config, window properties, initial state

**Window Manager**:
The one place a Controller goes to work with Windows outside its FXML: reach the Primary Window, find its own Window, open a View in a new Window, or close one.
_Avoid_: stage manager, view manager, navigator
