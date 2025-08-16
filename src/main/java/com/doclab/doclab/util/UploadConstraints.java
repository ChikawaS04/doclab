package com.doclab.doclab.util;

import java.util.Set;

public final class UploadConstraints {
    private UploadConstraints() {}
    public static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );
}