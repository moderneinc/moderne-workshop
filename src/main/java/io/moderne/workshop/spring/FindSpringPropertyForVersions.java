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
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.yaml.search.FindProperty;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindSpringPropertyForVersions extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "Version range",
            description = "Limit the property search to a " +
                          "matching Spring Framework version range.",
            example = "management.metrics.binders.files.enabled")
    String versionRange;

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

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (versionRange != null) {
            validated = validated.and(Semver.validate(versionRange, null));
        }
        return validated;
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean inRange) {
        VersionComparator versionComparator = requireNonNull(Semver.validate(versionRange, null).getValue());
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree != null) {
                    tree.getMarkers().findFirst(MavenResolutionResult.class)
                            .flatMap(mrr -> mrr.findDependencies("org.springframework", "spring-core", Scope.Runtime).stream()
                                    .filter(dep -> {
                                        return versionComparator.isValid(null, dep.getVersion());
                                    })
                                    .findFirst())
                            .ifPresent(dep -> inRange.set(true));
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean inRange) {
        TreeVisitor<?, ExecutionContext> mainSourceSetApplicationConfig = Preconditions.and(
                new HasSourceSet("main").getVisitor(),
                new FindSourceFiles("**/application*").getVisitor()
        );

        return Preconditions.check(inRange.get(), Preconditions.check(mainSourceSetApplicationConfig, new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t = tree;
                t = new FindProperties(propertyKey, relaxedBinding).getVisitor().visit(t, ctx);
                t = new FindProperty(propertyKey, relaxedBinding).getVisitor().visit(t, ctx);
                return t;
            }
        }));
    }
}
