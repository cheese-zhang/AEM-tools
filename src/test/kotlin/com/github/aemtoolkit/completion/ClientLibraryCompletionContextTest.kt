package com.github.aemtoolkit.completion

import junit.framework.TestCase

class ClientLibraryCompletionContextTest : TestCase() {
    fun testExtractsHtlCategoryPrefix() {
        val text =
            """<sly data-sly-call="${'$'}{clientlib.js @ categories=['example.si']}"/>"""
        val offset = text.indexOf("']}") 

        assertEquals(
            "example.si",
            ClientLibraryCompletionContext.htlCategoryPrefix(text, offset),
        )
    }

    fun testExtractsCurrentFileVaultArrayItem() {
        val text = """categories="[example.base, example.si]""""
        val offset = text.indexOf("]\"")

        assertEquals(
            "example.si",
            ClientLibraryCompletionContext.xmlArrayPrefix(text, offset),
        )
    }

    fun testExtractsLaterHtlCategoryAndScalarXmlValue() {
        val htl = """clientlib.css @ categories=['example.base', 'example.si']"""
        assertEquals(
            "example.si",
            ClientLibraryCompletionContext.htlCategoryPrefix(
                htl,
                htl.indexOf("']"),
            ),
        )
        val xml = """dependencies="example.co""""
        assertEquals(
            "example.co",
            ClientLibraryCompletionContext.xmlArrayPrefix(xml, xml.length - 1),
        )
    }
}
