package com.ioc

import com.ioc.common.*
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

/**
 * Created by sergeygolishnikov on 10/07/2017.
 */

fun targetDependencyModel(element: Element): DependencyModel {
    val dependency = DependencyModel(element, element, element.simpleName.toString(), element.asType(), false, false, false)
    dependency.generatedName = "target"
    return dependency
}

fun DependencyModel.isAllowEmptyConstructorInjection(): Boolean {
    return emptyConstructor != null
        && !emptyConstructor!!.modifiers.contains(Modifier.PRIVATE)
        && dependencies.isEmpty()
        && !isProvider
        && !isLazy
        && !isWeakDependency
        && !isViewModel
}

fun DependencyModel.isAllowModuleMethodProvide(): Boolean {
    val method = provideMethod()
    return method != null
        && method.dependencyModels.isEmpty()
        && !isProvider
        && !isLazy
        && !isWeakDependency
        && !isViewModel
}

fun DependencyModel.returnType(): TypeName {
    return when {
        isLazy -> originalType.asLazyType()
        isProvider -> originalType.asProviderType()
        isWeakDependency -> originalType.asWeakType()
        else -> originalClassName()
    }
}

class DependencyModel constructor(
    var dependency: Element,
    var originalType: Element,
    var fieldName: String,
    var erasuredType: TypeMirror,
    var isProvider: Boolean = false,
    var isLazy: Boolean = false,
    var isWeakDependency: Boolean = false) : SingletonWrapper {

    val typeString: String
        get() = dependency.asType().toString()

    val originalTypeString: String
        get() = originalType.asType().toString()
    val typeElementString: String
        get() = typeElement.asType().toString()
    var argumentsConstructor: ExecutableElement? = null
    var emptyConstructor: ExecutableElement? = null
    var isSingleton: Boolean = false
    var isLocal: Boolean = false
    var isViewModel: Boolean = false
    override val typeElement: TypeElement
        get() = dependency.asType().asTypeElement()
    override val packageName: String
        get() = typeElement.getPackage().toString()
    override var name: String = ""
        get() = erasuredType.toString().split(".").last().decapitalize()
        set(value) {
            field = value
        }
    override val className: ClassName
        get() = ClassName.get(typeElement)
    override var dependencies: List<DependencyModel> = emptyList()
        get() {
            return if (implementations.isEmpty()) field
            else implementations[0].dependencyModels
        }
    var typeArguments = mutableListOf<TypeMirror>()
    override var implementations: List<DependencyProvider> = emptyList()
    fun provideMethod(): DependencyProvider? {
        return implementations.firstOrNull { it.isMethod }
    }

    var sortOrder = Int.MAX_VALUE
    var named: String? = ""
    var setterMethod: ExecutableElement? = null

    var generatedName: String = name

    override fun originalClassName(): TypeName {
        if (typeArguments.isEmpty()) return ClassName.get(originalType.asTypeElement())
        return ParameterizedTypeName.get(ClassName.get(originalType.asTypeElement()), *typeArguments.map { ClassName.get(it) }.toTypedArray())
    }

    fun setterName(): String {
        setterMethod?.let { return it.simpleName.toString() }
        return fieldName
    }

    override fun equals(other: Any?): Boolean {
        return dependency.asType().toString() == (other as? DependencyModel)?.dependency?.asType()?.toString()
    }

    override fun hashCode(): Int {
        return className.hashCode()
    }

    fun setDependency(target: String, value: String): CodeBlock {
        val builder = CodeBlock.builder()
        if (setterMethod != null) builder.addStatement("$target($value)")
        else builder.addStatement("$target = $value")
        return builder.build()
    }

    val simpleName get() = typeElement.simpleName.toString().decapitalize()

    override fun toString(): String {
        return "type: $typeElementString - originalType: $originalTypeString"
    }
}