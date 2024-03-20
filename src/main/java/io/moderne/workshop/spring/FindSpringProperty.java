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
package io.moderne.workshop.spring;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.HasSourceSet;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.yaml.search.FindProperty;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindSpringProperty extends Recipe {

    @Option(displayName = "Property key",
            description = "The property key to look for.",
            example = "management.metrics.binders.files.enabled")
    String propertyKey;

    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                          "rules. Default is `true`. Set to `false`  to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Override
    public String getDisplayName() {
        return "Find Spring properties";
    }

    @Override
    public String getDescription() {
        return "Find Spring properties in YAML or properties files, " +
               "and only in the main source set.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> mainSourceSetApplicationConfig = Preconditions.and(
                new HasSourceSet("main").getVisitor(),
                new FindSourceFiles("**/application*").getVisitor()
        );

        return Preconditions.check(mainSourceSetApplicationConfig, new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t = tree;
                t = new FindProperties(propertyKey, relaxedBinding).getVisitor().visit(t, ctx);
                t = new FindProperty(propertyKey, relaxedBinding).getVisitor().visit(t, ctx);
                return t;
            }
        });
    }
}
