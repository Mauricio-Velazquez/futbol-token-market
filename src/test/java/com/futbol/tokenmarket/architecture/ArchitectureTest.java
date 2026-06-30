package com.futbol.tokenmarket.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@Tag("arch")
class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.futbol.tokenmarket";

    private static JavaClasses allClasses;

    @BeforeAll
    static void importClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("Controllers no acceden directamente a repositories")
    void controllersDoNotAccessRepositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().accessClassesThat().resideInAPackage("..repository..");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Los models no dependen de services ni controllers")
    void modelsDoNotDependOnUpperLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..model..")
                .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Clases en controller están anotadas con @RestController")
    void controllerClassesHaveRestControllerAnnotation() {
        ArchRule rule = classes()
                .that().resideInAPackage("..controller..")
                .should().beAnnotatedWith(RestController.class);
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Clases en service están anotadas con @Service o @Component")
    void serviceClassesAreManagedBySpring() {
        ArchRule rule = classes()
                .that().resideInAPackage("..service..").and().areTopLevelClasses()
                .should().beAnnotatedWith(Service.class)
                .orShould().beAnnotatedWith(Component.class);
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Clases en repository están anotadas con @Repository")
    void repositoryClassesHaveRepositoryAnnotation() {
        ArchRule rule = classes()
                .that().resideInAPackage("..repository..")
                .should().beAnnotatedWith(Repository.class);
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Clases en controller terminan en 'Controller'")
    void controllerNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..controller..")
                .should().haveSimpleNameEndingWith("Controller");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Clases en service terminan en 'Service'")
    void serviceNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..service..").and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("Service");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Clases en repository terminan en 'Repository'")
    void repositoryNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..repository..")
                .should().haveSimpleNameEndingWith("Repository");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Ninguna clase usa System.out ni System.err (usar SLF4J)")
    void noSystemOutOrErr() {
        ArchRule rule = noClasses()
                .should().accessField(System.class, "out")
                .orShould().accessField(System.class, "err");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("No hay ciclos de dependencias entre paquetes")
    void noCyclicDependencies() {
        slices().matching(BASE_PACKAGE + ".(*)..").should().beFreeOfCycles().check(allClasses);
    }
}
