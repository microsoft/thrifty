package com.microsoft.thrifty.util;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ObfuscationUtilTest {
    @Test
    public void summarizeStringList() {
        List<String> strings = Arrays.asList("one", "two", "three");
        String summary = ObfuscationUtil.summarizeCollection(strings, "List", "String");
        assertThat(summary, is("List<String>(size=3)"));
    }

    @Test
    public void summarizeObjectSet() {
        Set<Object> set = Sets.newHashSet(new Object(), new Object());
        String summary = ObfuscationUtil.summarizeCollection(set, "Set", "Object");
        assertThat(summary, is("Set<Object>(size=2)"));
    }

    @Test
    public void summarizeNullList() {
        List<Integer> list = null;
        String summary = ObfuscationUtil.summarizeCollection(list, "List", "Integer");
        assertThat(summary, is("null"));
    }

    @Test
    public void summarizeMap() {
        Map<String, Integer> map = Collections.emptyMap();
        assertThat(ObfuscationUtil.summarizeMap(map, "String", "Integer"), is("Map<String, Integer>(size=0)"));
    }
}