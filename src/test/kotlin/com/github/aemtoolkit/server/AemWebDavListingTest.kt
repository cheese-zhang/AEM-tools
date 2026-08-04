package com.github.aemtoolkit.server

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AemWebDavListingTest : BasePlatformTestCase() {
    fun testParsesEncodedFilesAndCollections() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/crx/repository/crx.default/apps/example/components/card/</d:href>
                    <d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop></d:propstat>
                </d:response>
                <d:response>
                    <d:href>/crx/repository/crx.default/apps/example/components/card/.content.xml</d:href>
                    <d:propstat><d:prop><d:resourcetype/></d:prop></d:propstat>
                </d:response>
                <d:response>
                    <d:href>/crx/repository/crx.default/apps/example/components/card/my%20file.html</d:href>
                    <d:propstat><d:prop><d:resourcetype/></d:prop></d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        val entries = AemContentSyncService(project).parseListing(xml.toByteArray())

        assertEquals(
            listOf(
                WebDavEntry("/apps/example/components/card", true),
                WebDavEntry("/apps/example/components/card/.content.xml", false),
                WebDavEntry("/apps/example/components/card/my file.html", false),
            ),
            entries,
        )
    }
}
