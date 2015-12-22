package com.bendb.thrifty.schema;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FieldNamingPolicyTest {
    @Test
    public void defaultNamesAreUnaltered() {
        FieldNamingPolicy policy = FieldNamingPolicy.DEFAULT;

        assertThat(policy.apply("SSLFlag"), is("SSLFlag"));
        assertThat(policy.apply("MyField"), is("MyField"));
    }

    @Test
    public void javaPolicyCamelCasesNames() {
        FieldNamingPolicy policy = FieldNamingPolicy.JAVA;

        assertThat(policy.apply("MyField"), is("myField"));
        assertThat(policy.apply("X"), is("x"));
        assertThat(policy.apply("abcde"), is("abcde"));
    }

    @Test
    public void javaPolicyPreservesAcronyms() {
        FieldNamingPolicy policy = FieldNamingPolicy.JAVA;

        assertThat(policy.apply("OAuthToken"), is("OAuthToken"));
        assertThat(policy.apply("SSLFlag"), is("SSLFlag"));
    }
}