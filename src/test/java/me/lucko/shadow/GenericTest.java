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
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericTest {

    @Test
    public void testUnwrapper() {
        Tag tag = new Tag();
        TagShadow tagShadow = ShadowFactory.global().shadow(TagShadow.class, tag);
        Map<String, Integer> tags = tagShadow.getTags();

        assertEquals(1, tags.get("sum"));

        CraftMetaItem craftMetaItem = new CraftMetaItem();
        CraftMetaItemShadow craftMetaItemShadow = ShadowFactory.global().shadow(CraftMetaItemShadow.class, craftMetaItem);
        Map<String, TagShadow> unhandledTags = craftMetaItemShadow.getUnhandledTags();

        assertEquals(1, unhandledTags.get("k").getTags().get("sum"));
    }

    @Test
    public void testWrapper() {
        CraftMetaItem craftMetaItem = new CraftMetaItem();
        CraftMetaItemShadow craftMetaItemShadow = ShadowFactory.global().shadow(CraftMetaItemShadow.class, craftMetaItem);

        TagShadow tagShadow = ShadowFactory.global().constructShadow(TagShadow.class);
        tagShadow.getTags().put("diff", -1);

        TreeMap<String, TagShadow> newUnhandledTags = new TreeMap<>();
        newUnhandledTags.put("k", tagShadow);
        craftMetaItemShadow.setUnhandledTags(newUnhandledTags);

        assertEquals(-1, craftMetaItemShadow.getUnhandledTags().get("k").getTags().get("diff"));
    }

    @ClassTarget(CraftMetaItem.class)
    private interface CraftMetaItemShadow extends Shadow {
        @Field
        @ShadowingStrategy(wrapper = UnhandledTagsStrategy.class)
        Map<String, TagShadow> getUnhandledTags();

        @Field
        @ShadowingStrategy(unwrapper = UnhandledTagsStrategy.class)
        void setUnhandledTags(TreeMap<String, TagShadow> tags);

        @SuppressWarnings("unchecked")
        class UnhandledTagsStrategy implements ShadowingStrategy.Wrapper, ShadowingStrategy.Unwrapper {

            @Override public @Nullable Object wrap(@Nullable final Object unwrapped, @NonNull final Class<?> expectedType, @NonNull final ShadowFactory shadowFactory) {
                Objects.requireNonNull(unwrapped);
                TreeMap<String, Object /* Tag */> unwrappedMap = (TreeMap<String, Object>) unwrapped;
                TreeMap<String, TagShadow> wrappedMap = new TreeMap<>();
                unwrappedMap.forEach((k, v) -> {
                    wrappedMap.put(k, shadowFactory.shadow(TagShadow.class, v));
                });
                return wrappedMap;
            }
            @Override public @Nullable Object unwrap(@Nullable final Object wrapped, @NonNull final Class<?> expectedType, @NonNull final ShadowFactory shadowFactory) {
                Objects.requireNonNull(wrapped);
                TreeMap<String, TagShadow> wrappedMap = (TreeMap<String, TagShadow>) wrapped;
                TreeMap<String, Object /* Tag */> unwrappedMap = new TreeMap<>();
                wrappedMap.forEach((k, v) -> {
                    unwrappedMap.put(k, v.getShadowTarget());
                });
                return unwrappedMap;
            }
            @Override public @NonNull Class<?> unwrap(final Class<?> wrappedClass, @NonNull final ShadowFactory shadowFactory) {
                return shadowFactory.getTargetClass(wrappedClass);
            }
        }
    }

    @ClassTarget(Tag.class)
    private interface TagShadow extends Shadow {
        @Field
        Map<String, Integer> getTags();
    }

    private static final class CraftMetaItem {
        final Map<String, Tag> unhandledTags = new TreeMap<>();

        public CraftMetaItem() {
            // create a Tag
            Tag tag = new Tag();

            // put it into the unhandledTags
            unhandledTags.put("k", tag);
        }
    }

    private static final class Tag {
        final Map<String, Integer> tags = new HashMap<>();

        public Tag() {
            // by default, there is an entry
            tags.put("sum", 1);
        }
    }

}
