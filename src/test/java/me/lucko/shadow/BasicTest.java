/*
 * This file is part of shadow, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.shadow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BasicTest {

    @Test
    public void testShadow() {
        DataClass data = new DataClass("foo", 5, false);
        DataClassShadow shadow = ShadowFactory.global().shadow(DataClassShadow.class, data);

        assertEquals("foo", shadow.getTheString());
        assertEquals(5, shadow.getTheInteger());
        assertFalse(shadow.isTheBoolean());

        shadow.setTheString("bar");
        assertEquals("bar", shadow.getTheString());
        assertEquals("bar", data.theString);
        shadow.incrementTheInteger();
        assertEquals(6, shadow.getTheInteger());
        assertEquals(1, shadow.getOne());
        assertEquals(2, shadow.getTwo());
    }

    @Test
    public void testConstruction() {
        DataClassShadow shadow = ShadowFactory.global().constructShadow(DataClassShadow.class, "baz", 42, true);
        assertEquals("baz", shadow.getTheString());

        Object target = shadow.getShadowTarget();
        assertNotNull(target);
        assertInstanceOf(DataClass.class, target);

        DataClass casted = (DataClass) target;
        assertTrue(casted.theBoolean);
        assertEquals(42, casted.i);
    }

    @Test
    public void testEqualityHashcode() {
        DataClass data1 = new DataClass("foo", 5, false);
        DataClass data2 = new DataClass("foo", 5, false);
        DataClassShadow shadow1 = ShadowFactory.global().shadow(DataClassShadow.class, data1);
        DataClassShadow shadow1_2 = ShadowFactory.global().shadow(DataClassShadow.class, data1);
        DataClassShadow shadow2 = ShadowFactory.global().shadow(DataClassShadow.class, data2);

        // ensure underlying properties are correct
        assertNotEquals(data1, data2);
        assertNotEquals(data1.hashCode(), data2.hashCode());

        // test shadow equals/hashcode
        assertEquals(shadow1, shadow1_2);
        assertNotEquals(shadow1, shadow2);
        assertEquals(shadow1.hashCode(), shadow1_2.hashCode());
        assertNotEquals(shadow1.hashCode(), shadow2.hashCode());

        // test different instances, even though equal
        assertNotSame(shadow1, shadow1_2);
    }

    @Test
    public void testShadowInspectionMethods() {
        DataClass data = new DataClass("foo", 5, false);
        DataClassShadow shadow = ShadowFactory.global().shadow(DataClassShadow.class, data);

        assertEquals(DataClassShadow.class, shadow.getShadowClass());
        assertEquals(data, shadow.getShadowTarget());
        assertNotNull(shadow.getShadowTarget());
        assertEquals(DataClass.class, shadow.getShadowTarget().getClass());
    }

    @Test
    public void testTransitiveShadow() {
        DataClass data = new DataClass("foo", 5, false);
        DataClassShadow shadow = ShadowFactory.global().shadow(DataClassShadow.class, data);

        assertEquals(-1, shadow.getTheHiddenClass().getTheInteger());
        assertSame(data, shadow.getTheSelf());
    }

    @ClassTarget(DataClass.class)
    private interface DataClassShadow extends Shadow, InterfaceA, InterfaceB {
        @Field
        String getTheString();

        @Field
        @Target("i")
        int getTheInteger();

        @Field
        boolean isTheBoolean();

        @Field
        void setTheString(String value);

        void incrementTheInteger();

        @Field
        @Target("transitiveClass")
        HiddenClassShadow getTheHiddenClass();

        @Target("getSelf")
        DataClass getTheSelf();
    }

    @ClassTarget(TransitiveClass.class)
    private interface HiddenClassShadow extends Shadow {
        @Field
        @Target("j")
        int getTheInteger();
    }

    private static final class DataClass extends AbstractClassA implements InterfaceA {
        private final String theString;
        private int i;
        private final boolean theBoolean;
        final TransitiveClass transitiveClass;

        private DataClass(String theString, int theInteger, boolean theBoolean) {
            this.theString = theString;
            this.i = theInteger;
            this.theBoolean = theBoolean;
            this.transitiveClass = new TransitiveClass(-1);
        }

        private void incrementTheInteger() {
            this.i++;
        }

        private InterfaceA getSelf() {
            return this;
        }
    }

    private interface InterfaceA {
        default int getOne() {
            return 1;
        }
    }

    private interface InterfaceB {
        int getTwo();
    }

    private static abstract class AbstractClassA implements InterfaceB {
        public int getTwo() {
            return 2;
        }
    }

    private static class TransitiveClass {
        final int j;
        private TransitiveClass(int j) {this.j = j;}
    }

}
