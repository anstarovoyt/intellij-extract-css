package com.github.anstarovoyt.intellijextractcss

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import extract.css.actions.ExtractState
import extract.css.actions.TargetLanguage
import extract.css.actions.generateContent
import junit.framework.TestCase
import org.junit.Test

@Suppress("JUnitMixedFramework")
class MyPluginTest : BasePlatformTestCase() {

    @Test
    fun testSimple() {
        TestCase.assertEquals(
            """
            .foo {}
            .bar {}
        """.trimIndent(), generateContent(ExtractState(), listOf("foo", "bar"))
        )
    }

    @Test
    fun testNoBEMSCSS() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals(
            """
            .foo {}
            .bar {}
        """.trimIndent(), generateContent(state, listOf("foo", "bar"))
        )
    }

    @Test
    fun testBEMElement() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals(
            """
            .foo {
             &__element {}
            }
        """.trimIndent(), generateContent(state, listOf("foo__element"))
        )
    }

    @Test
    fun testBEMModifier() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals(
            """
            .foo {
             &_modifier {}
            }
        """.trimIndent(), generateContent(state, listOf("foo_modifier"))
        )
    }

    @Test
    fun testBEMElementModifier() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals(
            """
            .foo {
             &__element {
              &_modifier {}
             }
            }
        """.trimIndent(), generateContent(state, listOf("foo__element_modifier"))
        )
    }

    @Test
    fun testBEMElements() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals(
            """
            .foo {
             &__element {}
             &__element2 {}
            }
        """.trimIndent(), generateContent(state, listOf("foo__element", "foo__element2"))
        )
    }

    @Test
    fun testBEMModifiers() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals(
            """
            .foo {
             &_modifier {}
             &_modifier2 {}
            }
        """.trimIndent(), generateContent(state, listOf("foo_modifier", "foo_modifier2"))
        )
    }

    @Test
    fun testBEMElementModifierStylus() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.STYLUS.name
        TestCase.assertEquals(
            """
            .foo
             &__element
              &_modifier
        """.trimIndent(), generateContent(state, listOf("foo__element_modifier"))
        )
    }

    @Test
    fun testBEMElementModifiersStylus() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.STYLUS.name
        TestCase.assertEquals(
            """
            .foo
             &__element
              &_modifier
              &_modifier2
        """.trimIndent(), generateContent(state, listOf("foo__element_modifier", "foo__element_modifier2"))
        )
    }

    @Test
    fun testBEMElementsStylus() {
        val state = ExtractState()
        state.bem = true
        state.language = TargetLanguage.STYLUS.name
        TestCase.assertEquals(
            """
            .foo
             &__element
             &__element2
        """.trimIndent(), generateContent(state, listOf("foo__element", "foo__element2"))
        )
    }

    @Test
    fun testBEMElementModifierComments() {
        val state = ExtractState()
        state.bem = true
        state.bemComments = true
        state.language = TargetLanguage.SCSS.name
        TestCase.assertEquals(
            """
                .foo {
                 // .foo__element
                 &__element {
                  // .foo__element_modifier
                  &_modifier {}
                 }
                }
        """.trimIndent(), generateContent(state, listOf("foo__element_modifier"))
        )
    }

    @Test
    fun testBEMElementModifierStylusComments() {
        val state = ExtractState()
        state.bem = true
        state.bemComments = true
        state.language = TargetLanguage.STYLUS.name
        TestCase.assertEquals(
            """
            .foo
             // .foo__element
             &__element
              // .foo__element_modifier
              &_modifier
        """.trimIndent(), generateContent(state, listOf("foo__element_modifier"))
        )
    }
}
