package com.bendb.thrifty.schema;

/**
 * Controls the style of names generated for fields.
 */
abstract class FieldNamingPolicy {
    public abstract String apply(String name);

    protected FieldNamingPolicy() {}

    /**
     * The default policy is to leave names unaltered from their definition in Thrift IDL.
     */
    public static final FieldNamingPolicy DEFAULT = new FieldNamingPolicy() {
        @Override
        public String apply(String name) {
            return name;
        }
    };

    /**
     * The Java policy generates camelCase names, unless the initial part of the field name
     * appears to be an acronym, in which case the casing is preserved.
     *
     * <p>"Acronym" here is defined to be two or more consecutive upper-case characters
     * at the beginning of the name.  Thus, this policy will preserve `.SSLFlag` over
     * `.sSLFlag`.
     */
    public static final FieldNamingPolicy JAVA = new FieldNamingPolicy() {
        @Override
        public String apply(String name) {
            if (Character.isUpperCase(name.charAt(0))) {
                if (name.length() == 1 || !Character.isUpperCase(name.charAt(1))) {
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                }
            }
            return name;
        }
    };
}
