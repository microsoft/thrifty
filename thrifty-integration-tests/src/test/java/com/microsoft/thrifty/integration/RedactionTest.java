package com.microsoft.thrifty.integration;

import com.microsoft.thrifty.integration.gen.HasCommentBasedRedaction;
import com.microsoft.thrifty.integration.gen.HasRedaction;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class RedactionTest {
    @Test
    public void redaction() {
        HasRedaction hr = new HasRedaction.Builder()
                .one("value-one")
                .two("should-not-appear")
                .three("value-three")
                .build();

        assertThat(hr.toString(), containsString("one=value-one"));
        assertThat(hr.toString(), not(containsString("should-not-appear")));
        assertThat(hr.toString(), containsString("three=value-three"));
        assertThat(hr.two, is("should-not-appear"));
    }

    @Test
    public void commentBasedRedaction() {
        HasCommentBasedRedaction hcbr = new HasCommentBasedRedaction.Builder()
                .foo("bar")
                .build();

        assertThat(hcbr.toString(), is("HasCommentBasedRedaction{foo=<REDACTED>}"));
    }
}
