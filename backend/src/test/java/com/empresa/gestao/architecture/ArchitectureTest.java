package com.empresa.gestao.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Regras da Clean Architecture descritas em
 * {@code docs/REFATORACAO_CLEANARCHITECTURE.md}. Qualquer violação quebra o build.
 */
@AnalyzeClasses(packages = "com.empresa.gestao", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.empresa.gestao";
    private static final String SHARED_MODEL = ROOT + ".shared.domain.model";

    @ArchTest
    static final ArchRule dominioNaoDependeDeFrameworkNemDeOutrasCamadas =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "org.springframework.data..",
                            "jakarta..",
                            "com.fasterxml..",
                            "io.swagger..",
                            "..application..",
                            "..infrastructure..",
                            "..presentation..");

    @ArchTest
    static final ArchRule aplicacaoNaoDependeDeInfraestruturaApresentacaoNemDeFrameworkWeb =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..infrastructure..",
                            "..presentation..",
                            "org.springframework.data..",
                            "org.springframework.http..",
                            "org.springframework.web..",
                            "jakarta.servlet..",
                            "jakarta.persistence..");

    @ArchTest
    static final ArchRule apresentacaoNaoDependeDeInfraestrutura =
            noClasses().that().resideInAPackage("..presentation..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule apresentacaoNaoDependeDeModeloDeDominioDeModulo =
            noClasses().that().resideInAPackage("..presentation..")
                    .should().dependOnClassesThat(JavaClass.Predicates
                            .resideInAPackage("..domain.model..")
                            .and(JavaClass.Predicates.resideOutsideOfPackage(SHARED_MODEL)));

    @ArchTest
    static final ArchRule infraestruturaNaoDependeDeApresentacao =
            noClasses().that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("..presentation..");

    @ArchTest
    static final ArchRule modulosConversamSomentePelaCamadaDeAplicacao =
            classes().that().resideInAPackage("com.empresa.gestao.(*)..")
                    .should(new ArchCondition<>("acessar outros módulos apenas pela camada application") {
                        @Override
                        public void check(JavaClass javaClass, ConditionEvents events) {
                            String module = moduleOf(javaClass.getPackageName());
                            if (module == null || isCompositionModule(module)) {
                                return;
                            }
                            javaClass.getDirectDependenciesFromSelf().forEach(dependency -> {
                                JavaClass target = dependency.getTargetClass();
                                String targetModule = moduleOf(target.getPackageName());
                                if (targetModule == null || targetModule.equals(module)
                                        || isSharedOrComposition(targetModule)
                                        || belongsToApplicationLayer(target)) {
                                    return;
                                }
                                events.add(SimpleConditionEvent.violated(javaClass,
                                        javaClass.getName() + " acessa " + target.getName()
                                                + " em " + target.getPackageName()
                                                + " fora da camada application do módulo "
                                                + targetModule));
                            });
                        }
                    });

    @ArchTest
    static final ArchRule modulosSemCiclos =
            slices().matching("com.empresa.gestao.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule entidadesJpaSomenteNaInfraestruturaDePersistencia =
            classes().that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage("..infrastructure.persistence.entity..");

    @ArchTest
    static final ArchRule controllersSomenteNaApresentacao =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..presentation..");

    private static String moduleOf(String packageName) {
        if (packageName == null || !packageName.startsWith(ROOT + ".")) {
            return null;
        }
        String remainder = packageName.substring(ROOT.length() + 1);
        int separator = remainder.indexOf('.');
        return separator < 0 ? remainder : remainder.substring(0, separator);
    }

    private static boolean isCompositionModule(String module) {
        return "bootstrap".equals(module) || "security".equals(module);
    }

    private static boolean isSharedOrComposition(String module) {
        return "shared".equals(module) || isCompositionModule(module);
    }

    private static boolean belongsToApplicationLayer(JavaClass target) {
        String applicationPackage = ROOT + "." + moduleOf(target.getPackageName()) + ".application";
        String targetPackage = target.getPackageName();
        return targetPackage.equals(applicationPackage)
                || targetPackage.startsWith(applicationPackage + ".");
    }
}
