package com.brendondugan.springfx.processor;

/**
 * What the processor needs from an FXML file: where it was found and its root element's
 * {@code fx:controller}.
 *
 * @param location where the file was read from, for messages
 * @param fxController the class named by the root's {@code fx:controller}, or null if absent
 */
record FxmlRoot(String location, String fxController) {
}
