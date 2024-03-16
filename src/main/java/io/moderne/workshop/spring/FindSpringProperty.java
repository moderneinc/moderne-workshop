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
