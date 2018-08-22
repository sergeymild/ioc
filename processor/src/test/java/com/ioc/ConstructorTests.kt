package com.ioc

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by sergeygolishnikov on 20/11/2017.
 */
@RunWith(JUnit4::class)
class ConstructorTests: BaseTest {

    @Test
    @Throws(Exception::class)
    fun constructorWithArgumentsWithoutInjectAnnotation() {

        val activityFile = JavaFileObjects.forSourceLines("test.Activity",
                "package test;",
                "",
                Inject::class.java.import(),
                "",
                "public class Activity {",
                "",
                "   @Inject",
                "   public ParentDependency dependency;",
                "}")

        val dependencyFile = JavaFileObjects.forSourceLines("test.DependencyModel",
                "package test;",
                "",
                "class DependencyModel {",
                "   public DependencyModel() {};",
                "}")

        val parentDependencyFile = JavaFileObjects.forSourceLines("test.ParentDependency",
                "package test;",
                "",
                "public class ParentDependency {",
                "   public ParentDependency(DependencyModel argument) {}",
                "}")

        val injectedFile = JavaFileObjects.forSourceLines("test.ActivityInjector",
                "package test;",
                "",
                keepAnnotation,
                nonNullAnnotation,
                "",
                "@Keep",
                "public final class ActivityInjector {",
                "   @Keep",
                "   public final void inject(@NonNull final Activity target) {",
                "       injectParentDependencyInDependency(target);",
                "   }",
                "",
                "   private final void injectParentDependencyInDependency(@NonNull final Activity target) {",
                "       DependencyModel dependencyModel = new DependencyModel();",
                "       ParentDependency parentDependency = new ParentDependency(dependencyModel);",
                "       target.dependency = parentDependency;",
                "   }",
                "}")

        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(activityFile, dependencyFile, parentDependencyFile))
                .processedWith(IProcessor())
                .compilesWithoutError()
                .and().generatesSources(injectedFile)
    }


    @Test
    @Throws(Exception::class)
    fun unsupportedConstructor() {

        val activityFile = JavaFileObjects.forSourceLines("test.Activity",
                "package test;",
                "",
                Inject::class.java.import(),
                "",
                "public class Activity {",
                "",
                "   @Inject",
                "   public DependencyModel dependency;",
                "}")

        val dependencyFile = JavaFileObjects.forSourceLines("test.DependencyModel",
                "package test;",
                "",
                Inject::class.java.import(),
                "",
                "class DependencyModel {",
                "   @Inject",
                "   public DependencyModel(String string) {};",
                "}")

        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(activityFile, dependencyFile))
                .processedWith(IProcessor())
                .failsToCompile()
                .withErrorContaining("@Inject annotation placed on constructor in test.DependencyModel which have unsupported parameters.")
                .`in`(dependencyFile)
                .onLine(7)
    }


    @Test
    @Throws(Exception::class)
    fun privatePostInitialization() {

        val activityFile = JavaFileObjects.forSourceLines("test.Activity",
                "package test;",
                "",
                Inject::class.java.import(),
                PostInitialization::class.java.import(),
                "",
                "public class Activity {",
                "",
                "   @Inject",
                "   public DependencyModel dependency;",
                "   @PostInitialization",
                "   private void postInitialization() {}",
                "}")

        val dependencyFile = JavaFileObjects.forSourceLines("test.DependencyModel",
                "package test;",
                "",
                Inject::class.java.import(),
                "",
                "class DependencyModel {",
                "   @Inject",
                "   public DependencyModel(String string) {};",
                "}")

        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(activityFile, dependencyFile))
                .processedWith(IProcessor())
                .failsToCompile()
                .withErrorContaining("@PostInitialization placed on `postInitialization` in test.Activity with private access")
                .`in`(activityFile)
                .onLine(11)
    }

    @Test
    @Throws(Exception::class)
    fun postInitializationWithParameters() {

        val activityFile = JavaFileObjects.forSourceLines("test.Activity",
                "package test;",
                "",
                Inject::class.java.import(),
                PostInitialization::class.java.import(),
                "",
                "public class Activity {",
                "",
                "   @Inject",
                "   public DependencyModel dependency;",
                "   @PostInitialization",
                "   public void postInitialization(String string) {}",
                "}")

        val dependencyFile = JavaFileObjects.forSourceLines("test.DependencyModel",
                "package test;",
                "",
                Inject::class.java.import(),
                "",
                "class DependencyModel {",
                "   @Inject",
                "   public DependencyModel(String string) {};",
                "}")

        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(activityFile, dependencyFile))
                .processedWith(IProcessor())
                .failsToCompile()
                .withErrorContaining("@PostInitialization placed on `postInitialization` in test.Activity must not have parameters")
                .`in`(activityFile)
                .onLine(11)
    }

    @Test
    @Throws(Exception::class)
    fun postInitializationWithReturnType() {

        val activityFile = JavaFileObjects.forSourceLines("test.Activity",
                "package test;",
                "",
                Inject::class.java.import(),
                PostInitialization::class.java.import(),
                "",
                "public class Activity {",
                "",
                "   @Inject",
                "   public DependencyModel dependency;",
                "   @PostInitialization",
                "   public String postInitialization() {return null;}",
                "}")

        val dependencyFile = JavaFileObjects.forSourceLines("test.DependencyModel",
                "package test;",
                "",
                Inject::class.java.import(),
                "",
                "class DependencyModel {",
                "   @Inject",
                "   public DependencyModel() {};",
                "}")

        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(activityFile, dependencyFile))
                .processedWith(IProcessor())
                .failsToCompile()
                .withErrorContaining("@PostInitialization placed on `postInitialization` in test.Activity must not have return type")
                .`in`(activityFile)
                .onLine(11)
    }

    @Test
    @Throws(Exception::class)
    fun successPostInitialization() {

        val activityFile = JavaFileObjects.forSourceLines("test.Activity",
                "package test;",
                "",
                Inject::class.java.import(),
                PostInitialization::class.java.import(),
                "",
                "public class Activity {",
                "",
                "   @Inject",
                "   public DependencyModel dependency;",
                "   @PostInitialization",
                "   public void postInitialization() {}",
                "}")

        val dependencyFile = JavaFileObjects.forSourceLines("test.DependencyModel",
                "package test;",
                "",
                "class DependencyModel {}")

        val injectedFile = JavaFileObjects.forSourceLines("test.ActivityInjector",
                "package test;",
                "",
                keepAnnotation,
                nonNullAnnotation,
                "",
                "@Keep",
                "public final class ActivityInjector {",
                "   @Keep",
                "   public final void inject(@NonNull final Activity target) {",
                "       injectDependencyModelInDependency(target);",
                "       target.postInitialization();",
                "   }",
                "",
                "   private final void injectDependencyModelInDependency(@NonNull final Activity target) {",
                "       DependencyModel dependencyModel = new DependencyModel();",
                "       target.dependency = dependencyModel;",
                "   }",
                "}")

        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(activityFile, dependencyFile))
                .processedWith(IProcessor())
                .compilesWithoutError()
                .and().generatesSources(injectedFile)
    }


    @Test
    @Throws(Exception::class)
    fun successNameCollision() {

        val dbMapper = JavaFileObjects.forSourceLines("test.DbMapper",
                "package test;",
                "",
                "public interface DbMapper {",
                "}")

        val dbMapperImpl = JavaFileObjects.forSourceLines("test.DbMapperImpl",
                "package test;",
                "",
                Dependency::class.java.import(),
                "",
                "@Dependency",
                "public class DbMapperImpl implements DbMapper {",
                "}")

        val dbRepository = JavaFileObjects.forSourceLines("test.DbRepository",
                "package test;",
                "",
                Inject::class.java.import(),
                "",
                "public class DbRepository {",
                "   @Inject",
                "   public DbRepository(DbMapper mapper) {}",
                "}")

        val managerFile = JavaFileObjects.forSourceLines("test.Manager",
                "package test;",
                "",
                Inject::class.java.import(),
                Singleton::class.java.import(),
                "",
                "@Singleton",
                "class Manager {",
                "   @Inject",
                "   public Manager(DbMapper mapper, DbRepository repository) {}",
                "}")

        val activityFile = JavaFileObjects.forSourceLines("test.Activity",
                "package test;",
                "",
                Inject::class.java.import(),
                PostInitialization::class.java.import(),
                "",
                "public class Activity {",
                "",
                "   @Inject",
                "   public Manager manager;",
                "}")

        val injectedFile = JavaFileObjects.forSourceLines("test.ManagerSingleton",
                "package test;",
                "",
                keepAnnotation,
                nonNullAnnotation,
                "",
                "@Keep",
                "public final class ManagerSingleton {",
                "   private static Manager singleton;",
                "",
                "   private static final ManagerSingleton instance = new ManagerSingleton();",
                "",
                "   @Keep",
                "   @NonNull",
                "   public static final Manager get() {",
                "       if (singleton != null) return singleton;",
                "       DbMapperImpl dbMapper = new DbMapperImpl();",
                "       DbMapperImpl dbMapper2 = new DbMapperImpl();",
                "       DbRepository dbRepository = new DbRepository(dbMapper2);",
                "       singleton = new Manager(dbMapper, dbRepository);",
                "       return singleton;",
                "   }",
                "}")

        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(dbMapper, dbMapperImpl, dbRepository, managerFile, activityFile))
                .processedWith(IProcessor())
                .compilesWithoutError()
                .and().generatesSources(injectedFile)
    }
}