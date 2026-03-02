package com.volcengine.imagegen.model;

/**
 * Model for Word document content
 */
public record WordDocumentContent(
    String text,
    String filename
) {
}
