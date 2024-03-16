package io.moderne.workshop.spring;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.yaml.Assertions.yaml;

public class FindSpringPropertyForVersionsTest implements RewriteTest {

    @Test
    void findSpringPropertyForVersions() {
        rewriteRun(
          (Consumer<RecipeSpec>) spec -> spec.recipe(new FindSpringPropertyForVersions("5.x",
            "spring.application.name", null)),
          srcMainResources(
            //language=yml
            yaml(
              """
                spring.application.name: test
                """,
              """
                spring.application.name: ~~>test
                """,
              spec -> spec.path("application.yml")
            )
          ),
          projectPom("2.5.0")
        );
    }

    @Test
    void dontMatchSpring6() {
        rewriteRun(
          (Consumer<RecipeSpec>) spec -> spec.recipe(new FindSpringPropertyForVersions("5.x",
            "spring.application.name", null)),
          srcMainResources(
            //language=yml
            yaml(
              """
                spring.application.name: test
                """,
              spec -> spec.path("application.yml")
            )
          ),
          projectPom("3.2.0")
        );
    }

    @NotNull
    private static SourceSpecs projectPom(String version) {
        return pomXml(
          //language=xml
          String.format("""
            <project>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>%s</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>0.0.1-SNAPSHOT</version>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter</artifactId>
                </dependency>
              </dependencies>
            </project>
            """, version)
        );
    }
}
