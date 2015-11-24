package com.bendb.thrifty.schema;

/**
 * Utility that determines whether an instance of one of several types
 * has non-empty Javadoc.
 *
 * If only we had traits - or JDK 8, at the very least.
 */
final class JavadocUtil {
    private JavadocUtil() {
        // no instances
    }

    static boolean hasJavadoc(Named named) {
        return hasJavadoc(named.documentation());
    }

    static boolean hasJavadoc(Field field) {
        return hasJavadoc(field.documentation());
    }

    static boolean hasJavadoc(EnumType.Member enumMember) {
        return hasJavadoc(enumMember.documentation());
    }

    static boolean hasJavadoc(ServiceMethod method) {
        return hasJavadoc(method.documentation());
    }

    private static boolean hasJavadoc(String doc) {
        if (doc == null) return false;
        if (doc.isEmpty()) return false;

        for (int i = 0; i < doc.length(); ++i) {
            char c = doc.charAt(i);
            if (!Character.isWhitespace(c)) {
                return true;
            }
        }

        return false;
    }
}
