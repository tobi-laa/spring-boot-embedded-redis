package io.github.tobi.laa.spring.boot.embedded.redis

import org.springframework.core.annotation.AnnotationUtils.findAnnotation
import org.springframework.test.context.TestContextAnnotationUtils.searchEnclosingClass

internal inline fun <reified T : Annotation> findTestClassAnnotation(
    testClass: Class<*>,
    annotationType: Class<T>
): T? {
    var clazz = testClass
    var annotation = findAnnotation(clazz, annotationType)
    while (annotation == null && searchEnclosingClass(clazz) && clazz.enclosingClass != null) {
        clazz = clazz.enclosingClass
        annotation = findAnnotation(clazz, annotationType)
    }
    return annotation
}