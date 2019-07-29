package com.ioc

import com.ioc.common.*
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier
import javax.lang.model.util.Types

/**
 * Created by sergeygolishnikov on 10/07/2017.
 */

internal fun targetParameter(className: ClassName): ParameterSpec {
    return ParameterSpec.builder(className, "target", Modifier.FINAL)
        .addAnnotation(nonNullAnnotation)
        .build()
}

class ImplementationsSpec constructor(
    private val target: TargetType,
    private val methods: List<MethodSpec>) {

    init {
        target.parentsDependencies()
    }

    @Throws(Throwable::class)
    fun inject(): TypeSpec {

        val builder = TypeSpec.classBuilder("${target.name}Injector")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(keepAnnotation)

        generateMethods().forEach { builder.addMethod(it) }

        methods.forEach { builder.addMethod(it) }

        return builder.build()
    }

    private fun generateMethods(): List<MethodSpec> {
        val methods = mutableListOf<MethodSpec>()

        val builder = MethodSpec.methodBuilder("inject")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(keepAnnotation)
            .addParameter(targetParameter(target.className))

        // Call super for parent inject
        target.firstParentWithDependencies()?.let {
            val parentType = it.className
            val injectorType = ClassName.get(parentType.packageName(), "${parentType.simpleName()}Injector")
            builder.addStatement("new \$T().inject(target)", injectorType)
        }

        this.methods.forEach {
            builder.addStatement("\$N(target)", it.name)
        }

        val postInitialization = target.postInitialization
        if (postInitialization != null) {
            builder.addStatement("target.\$N()", postInitialization.simpleName)
        }

        methods.add(builder.build())

        //methods.addAll(cachedMethods)


        return methods
    }

    companion object {

        fun dependencyInjectionMethod(
            target: ClassName,
            model: DependencyModel,
            codeBlock: CodeBlock): MethodSpec.Builder {

            val body = codeBlock.toBuilder()
            if (model.isSingleton) {
                body.add("\$T \$N = \$N;\n", model.className, model.generatedName, singletonProvider(model))
            }

            val methodName = "inject${model.name.capitalize()}In${model.fieldName.capitalize()}"
            return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .addParameter(targetParameter(target))
                .addCode(body.build())
        }

        // Root scope
        //  scope
        fun dependencyInjectionCode(
            dependency: DependencyModel,
            typeUtils: Types,
            target: TargetType,
            usedSingletons: Map<String, DependencyModel>): CodeBlock.Builder {

            val packageName = dependency.originalType.asTypeElement().getPackage()
            val isAllowedPackage = excludedPackages.any { packageName.toString().startsWith(it) }
            if (dependency.provideMethod() == null && isAllowedPackage) {
                throw ProcessorException("Can't find implementations of `${dependency.dependency.asType()} ${dependency.dependency}` maybe you forgot add correct @Named, @Qualifier or @Scope annotations or add @Dependency on provides method, `${target.element}`").setElement(target.element)
            }

            val builder = CodeBlock.builder()
            for (usedSingleton in usedSingletons) {
                //builder.add(singleton(usedSingleton.value.simpleName, usedSingleton.value))
            }

            val code = DependencyTree.generateWithLocalScope(listOf(dependency), typeUtils, usedSingletons, target)
            builder.add(code)
            //applyIsLoadIfNeed(listOf(dependency), target, usedSingletons)
            return builder

//            if (dependency.isViewModel) {
//                val code = CodeBlock.builder()
//                code.add(builder.build())
//                DependencyTree.get(dependency.dependencies, typeUtils, usedSingletons, target).also { code.add(it) }
//                applyIsLoadIfNeed(dependency.dependencies, target, usedSingletons)
//                val names = dependency.dependencyNames()
//                code.addStatement("return (T) new \$T($names)", dependency.originalClassName())
//                val originalGeneratedName = dependency.generatedName
//                val factoryName = "factory_${dependency.generatedName}"
//                val viewModelBuilder = viewModelFactoryCode(originalGeneratedName, code)
//
//                viewModelBuilder.addStatement("\$T \$N = \$T.of(target, \$N).get(\$T.class)",
//                    dependency.originalClassName(),
//                    dependency.generatedName,
//                    viewModelProvidersType,
//                    factoryName,
//                    dependency.originalType)
//                return viewModelBuilder
//            }
//
//            dependency.implementations.firstOrNull { it.isMethod }?.let {
//                if (it.isSingleton) {
//                    return singleton(dependency).toBuilder()
//                }
//
//
//                DependencyTree.get(dependency.dependencies, typeUtils, usedSingletons, target).also { builder.add(it) }
//                applyIsLoadIfNeed(dependency.dependencies, target, usedSingletons)
//                val names = dependency.dependencyNames()
//                var statementString = "\$T \$N = \$T.\$N(\$L)"
//                if (it.isKotlinModule) statementString = "\$T \$N = \$T.INSTANCE.\$N(\$L)"
//                return builder.addStatement(statementString,
//                    dependency.originalClassName(),
//                    dependency.generatedName,
//                    it.module,
//                    it.name,
//                    names)
//            }
//
//            dependency.implementations.firstOrNull { !it.isMethod }?.let {
//                if (it.isSingleton) {
//                    return singleton(dependency).toBuilder()
//                }
//
//                DependencyTree.get(it.dependencyModels, typeUtils, usedSingletons, target).also { builder.add(it) }
//                applyIsLoadIfNeed(dependency.dependencies, target, usedSingletons)
//                val names = it.dependencyNames()
//                builder.addStatement("\$T \$N = new \$T(\$L)", dependency.originalClassName(), dependency.generatedName, it.returnType(), names)
//                return builder
//            }
//
//            if (dependency.isSingleton)
//                return singleton(dependency).toBuilder()
//
//            // Inject with arguments constructor
//            if (dependency.argumentsConstructor != null) {
//                return argumentsConstructor(dependency, typeUtils, target, usedSingletons)
//                    .add(builder)
//            }
//
//            // Inject with no arguments constructor
//            if (dependency.emptyConstructor != null) {
//                return CodeBlock.builder().emptyConstructor(dependency).add(builder)
//            }
//
//            return builder
        }

        fun injectInTarget(builder: MethodSpec.Builder, dependency: DependencyModel): MethodSpec {
            builder.addCode(dependency.setDependency("target.${dependency.setterName()}", dependency.generatedName))
            return builder.build()
        }
    }

}