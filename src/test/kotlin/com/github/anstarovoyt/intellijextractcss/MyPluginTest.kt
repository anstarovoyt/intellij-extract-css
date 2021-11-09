package com.github.anstarovoyt.intellijextractcss

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import extract.css.actions.ExtractState
import extract.css.actions.TargetLanguage
import extract.css.actions.generateContent
import junit.framework.TestCase

class MyPluginTest : BasePlatformTestCase() {
    fun testSimple() {
        TestCase.assertEquals("""
            .foo {}
            .bar {}
        """.trimIndent(), generateContent(ExtractState(), listOf("foo", "bar")))
    }

    fun testNoBEMSCSS() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals("""
            .foo {}
            .bar {}
        """.trimIndent(), generateContent(state, listOf("foo", "bar")))
    }
    
    fun testBEMElement() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals("""
            .foo {
             &__element {}
            }
        """.trimIndent(), generateContent(state, listOf("foo__element")))
    }

    fun testBEMModifier() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals("""
            .foo {
             &_modifier {}
            }
        """.trimIndent(), generateContent(state, listOf("foo_modifier")))
    }

    fun testBEMElementModifier() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals("""
            .foo {
             &__element {
              &_modifier {}
             }
            }
        """.trimIndent(), generateContent(state, listOf("foo__element_modifier")))
    }
    
    fun testBEMElements() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals("""
            .foo {
             &__element {}
             &__element2 {}
            }
        """.trimIndent(), generateContent(state, listOf("foo__element", "foo__element2")))
    }
    
    fun testBEMModifiers() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals("""
            .foo {
             &_modifier {}
             &_modifier2 {}
            }
        """.trimIndent(), generateContent(state, listOf("foo_modifier", "foo_modifier2")))
    }

    fun testBEMElementModifierStylus() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.STYLUS.name
        TestCase.assertEquals("""
            .foo
             &__element
              &_modifier
        """.trimIndent(), generateContent(state, listOf("foo__element_modifier")))
    }
    
    fun testBEMElementModifiersStylus() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.STYLUS.name
        TestCase.assertEquals("""
            .foo
             &__element
              &_modifier
              &_modifier2
        """.trimIndent(), generateContent(state, listOf("foo__element_modifier", "foo__element_modifier2")))
    }
    
    fun testBEMElementsStylus() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.STYLUS.name
        TestCase.assertEquals("""
            .foo
             &__element
             &__element2
        """.trimIndent(), generateContent(state, listOf("foo__element", "foo__element2")))
    }

}
