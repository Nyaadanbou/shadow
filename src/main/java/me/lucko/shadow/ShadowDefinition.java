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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Represents a processed {@link Shadow} definition.
 */
final class ShadowDefinition {
    private final @NonNull ShadowFactory shadowFactory;
    private final @NonNull Class<? extends Shadow> shadowClass;
    private final @NonNull Class<?> targetClass;

    // caches
    private final @NonNull LoadingMap<MethodInfo, TargetMethod> methods = LoadingMap.of(this::loadTargetMethod);
    private final @NonNull LoadingMap<FieldInfo, TargetField> fields = LoadingMap.of(this::loadTargetField);
    private final @NonNull LoadingMap<ConstructorInfo, MethodHandle> constructors = LoadingMap.of(this::loadTargetConstructor);

    ShadowDefinition(@NonNull ShadowFactory shadowFactory, @NonNull Class<? extends Shadow> shadowClass, @NonNull Class<?> targetClass) {
        this.shadowFactory = shadowFactory;
        this.shadowClass = shadowClass;
        this.targetClass = targetClass;
    }

    public @NonNull Class<? extends Shadow> getShadowClass() {
        return this.shadowClass;
    }

    public @NonNull Class<?> getTargetClass() {
        return this.targetClass;
    }

    public @NonNull TargetMethod findTargetMethod(@NonNull Method shadowMethod, @NonNull Class<?>[] argumentTypes, @NonNull Class<?> returnType) {
        return this.methods.get(new MethodInfo(shadowMethod, argumentTypes, returnType, shadowMethod.isAnnotationPresent(Static.class)));
    }

    public @NonNull TargetField findTargetField(@NonNull Method shadowMethod) {
        return this.fields.get(new FieldInfo(shadowMethod, shadowMethod.isAnnotationPresent(Static.class)));
    }

    public @NonNull MethodHandle findTargetConstructor(@NonNull Class<?>[] argumentTypes) {
        return this.constructors.get(new ConstructorInfo(argumentTypes));
    }

    private @NonNull TargetMethod loadTargetMethod(@NonNull MethodInfo methodInfo) {
        Method shadowMethod = methodInfo.shadowMethod;
        String methodName = this.shadowFactory.getTargetLookup().lookupMethod(shadowMethod, this.shadowClass, this.targetClass).orElseGet(shadowMethod::getName);
        Method method = BeanUtils.getMatchingMethod(this.targetClass, methodName, methodInfo.argumentTypes, methodInfo.returnType);
        if (method == null) {
            throw new RuntimeException(new NoSuchMethodException(this.targetClass.getName() + "." + methodName));
        }
        if (methodInfo.isStatic && !Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException("Shadow method " + shadowMethod + " is marked as static, but the target method " + method + " is not.");
        }
        if (!methodInfo.isStatic && Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException("Shadow method " + shadowMethod + " is not marked as static, but the target method " + method + " is.");
        }

        Reflection.ensureAccessible(method);

        try {
            return new TargetMethod(method);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private @NonNull TargetField loadTargetField(@NonNull FieldInfo fieldInfo) {
        Method shadowMethod = fieldInfo.shadowMethod;

        String fieldName = this.shadowFactory.getTargetLookup().lookupField(shadowMethod, this.shadowClass, this.targetClass).orElseGet(shadowMethod::getName);
        Field field = Reflection.findField(this.targetClass, fieldName);
        if (field == null) {
            throw new RuntimeException(new NoSuchFieldException(this.targetClass.getName() + "#" + fieldName));
        }
        if (fieldInfo.isStatic && !Modifier.isStatic(field.getModifiers())) {
            throw new RuntimeException("Shadow method " + shadowMethod + " is marked as static, but the target field " + field + " is not.");
        }
        if (!fieldInfo.isStatic && Modifier.isStatic(field.getModifiers())) {
            throw new RuntimeException("Shadow method " + shadowMethod + " is not marked as static, but the target field " + field + " is.");
        }

        Reflection.ensureAccessible(field);

        try {
            return new TargetField(field);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private @NonNull MethodHandle loadTargetConstructor(@NonNull ConstructorInfo constructorInfo) {
        Constructor<?> constructor = BeanUtils.getMatchingConstructor(this.targetClass, constructorInfo.argumentTypes);
        if (constructor == null) {
            throw new RuntimeException(new NoSuchMethodException(this.targetClass.getName() + ".<init>" + " - " + Arrays.toString(constructorInfo.argumentTypes)));
        }

        Reflection.ensureAccessible(constructor);

        try {
            return PrivateMethodHandles.forClass(constructor.getDeclaringClass()).unreflectConstructor(constructor);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class MethodInfo {
        private final Method shadowMethod;
        private final Class<?>[] argumentTypes;
        private final Class<?> returnType;
        private final boolean isStatic;

        MethodInfo(Method shadowMethod, Class<?>[] argumentTypes, Class<?> returnType, boolean isStatic) {
            this.shadowMethod = shadowMethod;
            this.argumentTypes = argumentTypes;
            this.returnType = returnType;
            this.isStatic = isStatic;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodInfo that = (MethodInfo) o;
            return this.shadowMethod.equals(that.shadowMethod);
        }

        @Override
        public int hashCode() {
            return this.shadowMethod.hashCode();
        }
    }

    private static final class FieldInfo {
        private final Method shadowMethod;
        private final boolean isStatic;

        FieldInfo(Method shadowMethod, boolean isStatic) {
            this.shadowMethod = shadowMethod;
            this.isStatic = isStatic;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FieldInfo that = (FieldInfo) o;
            return this.shadowMethod.equals(that.shadowMethod);
        }

        @Override
        public int hashCode() {
            return this.shadowMethod.hashCode();
        }
    }

    private static final class ConstructorInfo {
        private final Class<?>[] argumentTypes;

        ConstructorInfo(Class<?>[] argumentTypes) {
            this.argumentTypes = argumentTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ConstructorInfo that = (ConstructorInfo) o;
            return Arrays.equals(this.argumentTypes, that.argumentTypes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.argumentTypes);
        }
    }

    /**
     * Represents a target field.
     *
     * <h3>Implementation Notes</h3>
     * <p>
     * The {@link #field} is assumed to be accessible, i.e.,
     * {@link AccessibleObject#canAccess(Object)} returns true.
     * <p>
     * For <em>reads</em>, the {@link #varHandle} can (and should) be used for ANY kind
     * of fields, regardless of whether they are public or private, static or non-static,
     * final or non-final.
     * <p>
     * For <em>writes</em>, it becomes a bit of tricky. The {@link #varHandle} can only be
     * used for non-final fields (because it respects the final modifier). For that reason,
     * the {@link #setterHandle} should be used for fields with {@code final} modifier.
     * <p>
     * It should be noted that the writes to {@code static} and {@code final} fields
     * are specifically prohibited due to it being extremely unsafe and unstable. See
     * <a href="https://stackoverflow.com/a/3301720/10275532">this post</a> for details.
     */
    static final class TargetField {
        private final @NonNull Field field;
        private final @NonNull VarHandle varHandle;
        private final @Nullable MethodHandle setterHandle;

        TargetField(@NonNull Field field) throws IllegalAccessException {
            this.field = field;

            MethodHandles.Lookup lookup = PrivateMethodHandles.forClass(field.getDeclaringClass());
            this.varHandle = lookup.unreflectVarHandle(field);

            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                // We can't reflectively set static & final fields in a safe manner,
                // so the setter handle will just be null for static & final fields.
                this.setterHandle = null;
            } else {
                this.setterHandle = lookup.unreflectSetter(field);
            }
        }

        public @NonNull Field underlyingField() {
            return this.field;
        }

        public @NonNull VarHandle varHandle() {
            return this.varHandle;
        }

        public @Nullable MethodHandle setterHandle() {
            return this.setterHandle;
        }
    }

    static final class TargetMethod {
        private final @NonNull Method method;
        private final @NonNull MethodHandle methodHandle;

        TargetMethod(@NonNull Method method) throws IllegalAccessException {
            this.method = method;
            this.methodHandle = PrivateMethodHandles.forClass(method.getDeclaringClass()).unreflect(method);
        }

        public @NonNull Method underlyingMethod() {
            return this.method;
        }

        public @NonNull MethodHandle handle() {
            return this.methodHandle;
        }
    }
}
