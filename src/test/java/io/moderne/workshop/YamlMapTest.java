package io.moderne.workshop;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.yaml.Assertions.yaml;

public class YamlMapTest implements RewriteTest {

    @Test
    void yamlMap() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new YamlIsoVisitor<>() {
              @Override
              public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                  YamlMap map = YamlMap.build(document);
                  YamlMap name = map.get("spring").get("application").get("name");
                  assertThat(name.getValue()).contains("test");
                  name.setValue("changed");
                  return map.getYaml();
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
          //language=yml
          yaml(
            """
              spring:
                application:
                  name: test
              """,
            """
              spring:
                application:
                  name: changed
              """
          )
        );
    }
}
