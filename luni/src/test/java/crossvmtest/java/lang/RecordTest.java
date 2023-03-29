/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package crossvmtest.java.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

public class RecordTest {

    record RecordInteger(int x) {}

    private static class NonRecordInteger {

        NonRecordInteger(int x) {
            this.x = x;
        }
        private final int x;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
    public @interface CustomAnnotation {
        String value();
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
    public @interface CustomAnnotation2 {

        CustomAnnotation[] customAnnotations();
    }
    record RecordInteger2(@CustomAnnotation2(customAnnotations = {@CustomAnnotation("a")})
                          @CustomAnnotation("b") int x) {}

    @Test
    public void testHashCode() {
        RecordInteger a = new RecordInteger(9);
        RecordInteger b = new RecordInteger(9);
        RecordInteger c = new RecordInteger(0);

        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
    }

    @Test
    public void testEquals() {
        RecordInteger a = new RecordInteger(9);
        RecordInteger b = new RecordInteger(9);
        RecordInteger c = new RecordInteger(0);

        assertTrue(a.equals(b));
        assertEquals(a, b);
        assertFalse(a.equals(c));
        assertNotEquals(a, c);
    }

    @Test
    public void testToString() {
        RecordInteger a = new RecordInteger(9);
        RecordInteger b = new RecordInteger(9);
        RecordInteger c = new RecordInteger(0);

        assertEquals(a.toString(), b.toString());
        assertNotEquals(a.toString(), c.toString());
    }

    @Test
    public void testIsRecord() throws Exception {
        RecordInteger a = new RecordInteger(9);
        assertTrue(a.getClass().isRecord());
    }

    @Test
    public void testReflectedConstructor() throws ReflectiveOperationException {
        RecordInteger a = new RecordInteger(9);

        Constructor<?> c = RecordInteger.class.getDeclaredConstructors()[0];
        assertEquals(Arrays.deepToString(c.getParameters()), 1, c.getParameters().length);
        assertEquals(c.getParameters()[0].toString(), "x", c.getParameters()[0].getName());
        RecordInteger b = (RecordInteger) c.newInstance(9);
        assertEquals(a.x, b.x);
        assertEquals(a.x(), b.x());
        assertEquals(a, b);
    }

    @Test
    public void testReadField() throws ReflectiveOperationException {
        RecordInteger a = new RecordInteger(9);
        assertEquals(9, a.x);
        assertEquals(9, a.x());

        Field[] fields = RecordInteger.class.getDeclaredFields();
        assertEquals(Arrays.deepToString(fields), 1, fields.length);
        Field field = fields[0];
        field.setAccessible(true);
        assertEquals(field.toString(), "x", field.getName());
        assertEquals(9, field.get(a));
    }

    @Test
    public void testWriteField() throws ReflectiveOperationException {
        NonRecordInteger a = new NonRecordInteger(8);
        Field fieldA = NonRecordInteger.class.getDeclaredField("x");
        fieldA.setAccessible(true);
        fieldA.set(a, 7);
        assertEquals(7, a.x);

        RecordInteger b = new RecordInteger(8);

        Field fieldB = RecordInteger.class.getDeclaredField("x");
        fieldB.setAccessible(true);
        assertThrows(IllegalAccessException.class, () -> fieldB.set(b, 7));
        assertEquals(8, b.x);
    }

    @Test
    public void testVarHandleWrite() throws ReflectiveOperationException {
        NonRecordInteger a = new NonRecordInteger(8);

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(NonRecordInteger.class,
                MethodHandles.lookup());
        VarHandle varHandle = lookup.findVarHandle(NonRecordInteger.class, "x", int.class);
        assertEquals(8, varHandle.get(a));
        assertThrows(UnsupportedOperationException.class, () -> varHandle.set(a, 6));
        assertEquals(8, a.x);

        RecordInteger b = new RecordInteger(8);

        lookup = MethodHandles.privateLookupIn(RecordInteger.class, MethodHandles.lookup());
        VarHandle varHandleB = lookup.findVarHandle(RecordInteger.class, "x", int.class);
        assertThrows(UnsupportedOperationException.class, () -> varHandleB.set(b, 7));
        assertEquals(8, b.x);
    }
}