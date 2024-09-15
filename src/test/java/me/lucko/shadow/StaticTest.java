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

public class StaticTest {

    @Test
    public void testStatic1() {
        TestClassShadow shadow = ShadowFactory.global().shadow(TestClassShadow.class, new TestClass("foo"));
        TestClassShadow staticShadow = ShadowFactory.global().staticShadow(TestClassShadow.class);

        shadow.isString("bar");
        staticShadow.isString("bar");

        assertThrows(IllegalStateException.class, staticShadow::getString, "Cannot call non-static method from a static shadow instance.");
    }

    @Test
    public void testStatic2() {
        TestClassShadow staticShadow = ShadowFactory.global().staticShadow(TestClassShadow.class);
        assertDoesNotThrow(staticShadow::getRandomInt, "No exception should be thrown if we read value from a private-static-final field.");
        assertEquals(1, staticShadow.getRandomInt());
    }

    @Test
    public void testStatic3() {
        TestClassShadow staticShadow = ShadowFactory.global().staticShadow(TestClassShadow.class);
        assertThrows(UnsupportedOperationException.class, () -> staticShadow.setRandomInt(2), "An exception should be throw if we modify value for a private-static-final field.");
        assertEquals(1, staticShadow.getRandomInt());
    }

    @Test
    public void testDefault() {
        TestClassShadow shadow = ShadowFactory.global().shadow(TestClassShadow.class, new TestClass("foo"));
        assertEquals("foobar", shadow.getStringWith("bar"));
    }

    @ClassTarget(TestClass.class)
    private interface TestClassShadow extends Shadow {
        @Field
        String getString();

        @Field
        @Static
        @Target("RANDOM_INT")
        Integer getRandomInt();

        // Shadow at static & final fields is prohibited!
        @Field
        @Static
        @Target("RANDOM_INT")
        void setRandomInt(int i);

        @Static
        boolean isString(Object o);

        default String getStringWith(String other) {
            return getString() + other;
        }
    }

    private static final class TestClass {
        private final String string;

        private TestClass(String string) {
            this.string = string;
        }

        private static final Integer RANDOM_INT = 1;

        public static boolean isString(Object obj) {
            return obj instanceof String;
        }
    }
}
