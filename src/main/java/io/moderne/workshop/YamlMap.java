/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.moderne.workshop;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;

import static java.util.Collections.emptyIterator;
import static java.util.Objects.requireNonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class YamlMap extends AbstractMap<String, YamlMap> {
    private static final YamlMap EMPTY = new YamlMap(null, null);

    @Nullable
    private final YamlMap parent;

    @Nullable
    private Yaml yaml;

    public <Y extends Yaml> Y getYaml() {
        //noinspection unchecked
        return (Y) requireNonNull(yaml);
    }

    public String getPathAsProperty() {
        return "";
    }

    public static YamlMap build(Yaml.Document document) {
        return new YamlMap(null, document);
    }

    public static YamlMap build(Yaml.Mapping mapping) {
        return new YamlMap(null, mapping);
    }

    @Override
    public Set<Entry<String, YamlMap>> entrySet() {
        return new EntrySet();
    }

    @Override
    public YamlMap get(Object key) {
        if (yaml instanceof Yaml.Document) {
            if (((Yaml.Document) yaml).getBlock() instanceof Yaml.Mapping) {
                return new YamlMap(this, ((Yaml.Document) yaml).getBlock()).get(key);
            }
        }
        if (yaml instanceof Yaml.Mapping) {
            for (Yaml.Mapping.Entry entry : ((Yaml.Mapping) yaml).getEntries()) {
                if (entry.getKey().getValue().equals(key)) {
                    YamlMap parent = new YamlMap(this, entry);
                    if (entry.getValue() instanceof Yaml.Mapping) {
                        return new YamlMap(parent, entry.getValue());
                    }
                    return new YamlMap(parent, entry.getValue());
                }
            }
        }
        return EMPTY;
    }

    public Optional<String> getValue() {
        if (yaml instanceof Yaml.Scalar) {
            return Optional.of(((Yaml.Scalar) yaml).getValue());
        }
        return Optional.empty();
    }

    public YamlMap setValue(Object value) {
        if (yaml instanceof Yaml.Scalar) {
            return update(((Yaml.Scalar) yaml).withValue(value.toString()));
        }
        return this;
    }

    public YamlMap insert(Map<String, Object> values) {
        if (!(yaml instanceof Yaml.Mapping)) {
            throw new IllegalArgumentException("Values can only be inserted at Yaml.Mapping instances");
        }
        throw new UnsupportedOperationException("implement me!");
    }

    private class EntrySet extends AbstractSet<Entry<String, YamlMap>> {
        @Override
        public Iterator<Entry<String, YamlMap>> iterator() {
            if (yaml instanceof Yaml.Document) {
                if (((Yaml.Document) yaml).getBlock() instanceof Yaml.Mapping) {
                    return new YamlMap(YamlMap.this, ((Yaml.Document) yaml).getBlock())
                            .entrySet().iterator();
                }
            }
            if (!(yaml instanceof Yaml.Mapping)) {
                return emptyIterator();
            }
            return ((Yaml.Mapping) yaml).getEntries()
                    .stream()
                    .filter(entry -> entry.getValue() instanceof Yaml.Mapping)
                    .map(entry -> (Entry<String, YamlMap>) new SimpleEntry<>(entry.getKey().getValue(),
                            new YamlMap(YamlMap.this, entry.getValue())))
                    .iterator();
        }

        @Override
        public int size() {
            if (yaml instanceof Yaml.Mapping) {
                return (int) ((Yaml.Mapping) yaml).getEntries()
                        .stream()
                        .filter(entry -> entry.getValue() instanceof Yaml.Mapping)
                        .count();
            }
            return 0;
        }
    }

    private YamlMap update(Yaml after) {
        if (parent != null) {
            Yaml parentAfter = new YamlVisitor<Integer>() {
                @Override
                public @Nullable Yaml visit(@Nullable Tree tree, Integer integer) {
                    if (tree == parent.yaml) {
                        return super.visit(tree, integer);
                    } else if (tree == yaml) {
                        return after;
                    }
                    return (Yaml) tree;
                }
            }.visitNonNull(requireNonNull(parent.yaml), 0);
            parent.update(parentAfter);
        }
        yaml = after;
        return this;
    }
}
