package com.ioc.common

import com.ioc.DependencyModel
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

object ProviderGeneration {
    fun wrapInProviderClassIfNeed(model: DependencyModel, body: CodeBlock.Builder): CodeBlock.Builder {
        if (!model.isProvider) return body
        return CodeBlock
            .builder()
            .add("\$T \$N = \$L;\n",
                model.originalType.asProviderType(),
                model.generatedName,
                anonymousClass(model.originalType, model.generatedName, body.build()))
    }



    private fun providerMethod(type: Element, name: String, body: CodeBlock): MethodSpec {
        return MethodSpec.methodBuilder("initialize")
            .addModifiers(Modifier.PROTECTED)
            .returns(type.asTypeName())
            .addCode(body)
            .addStatement("return \$N", name)
            .build()
    }

    private fun anonymousClass(type: Element, name: String, body: CodeBlock): TypeSpec {
        return TypeSpec.anonymousClassBuilder("")
            .superclass(type.asProviderType())
            .addMethod(providerMethod(type, name, body))
            .build()
    }

    fun generateLazyClass(body: CodeBlock, name: String, type: Element): TypeSpec {
        return TypeSpec.classBuilder("${type.simpleName}IocProvider")
            .superclass(type.asProviderType())
            .addMethod(providerMethod(type, name, body))
            .build()
    }
}