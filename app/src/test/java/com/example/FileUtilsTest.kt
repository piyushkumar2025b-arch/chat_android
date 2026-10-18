package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.FileUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FileUtilsTest {

    @Test
    fun testFormatCsvContent() {
        val tempFile = File.createTempFile("test_csv", ".csv")
        tempFile.writeText("Name,Department,Salary\nAlice,Engineering,120000\nBob,Design,95000")

        val result = FileUtils.formatCsvContent(tempFile)
        assertTrue(result.contains("CSV Spreadsheet"))
        assertTrue(result.contains("HEADER: Name,Department,Salary"))
        assertTrue(result.contains("Alice,Engineering,120000"))
        assertTrue(result.contains("Bob,Design,95000"))
        tempFile.delete()
    }

    @Test
    fun testFormatTsvContent() {
        val tempFile = File.createTempFile("test_tsv", ".tsv")
        tempFile.writeText("Metric\tQ1\tQ2\nRevenue\t1000\t1500")

        val result = FileUtils.formatTsvContent(tempFile)
        assertTrue(result.contains("TSV Data"))
        assertTrue(result.contains("Revenue | 1000 | 1500"))
        tempFile.delete()
    }

    @Test
    fun testFormatJsonContent() {
        val tempFile = File.createTempFile("test_json", ".json")
        tempFile.writeText("{\"status\":\"ok\",\"code\":200,\"data\":[1,2,3]}")

        val result = FileUtils.formatJsonContent(tempFile)
        assertTrue(result.contains("JSON Object"))
        assertTrue(result.contains("\"status\": \"ok\""))
        assertTrue(result.contains("\"code\": 200"))
        tempFile.delete()
    }

    @Test
    fun testExtractTextFromXmlOrHtml() {
        val tempFile = File.createTempFile("test_html", ".html")
        tempFile.writeText("<html><body><h1>Welcome to OmniChat</h1><p>AI with &amp; multi-provider &lt;support&gt;</p></body></html>")

        val result = FileUtils.extractTextFromXmlOrHtml(tempFile)
        assertTrue(result.contains("Markup Document"))
        assertTrue(result.contains("Welcome to OmniChat"))
        assertTrue(result.contains("AI with & multi-provider <support>"))
        assertFalse(result.contains("<h1>"))
        assertFalse(result.contains("<body>"))
        tempFile.delete()
    }

    @Test
    fun testExtractTextFromRtf() {
        val tempFile = File.createTempFile("test_rtf", ".rtf")
        tempFile.writeText("{\\rtf1\\ansi\\b Project Plan\\b0\\par Milestone 1 completed.}")

        val result = FileUtils.extractTextFromRtf(tempFile)
        assertTrue(result.contains("RTF Document"))
        assertTrue(result.contains("Project Plan"))
        assertTrue(result.contains("Milestone 1 completed"))
        tempFile.delete()
    }

    @Test
    fun testExtractTextFromDocxZip() {
        val tempFile = File.createTempFile("test_doc", ".docx")
        val zos = ZipOutputStream(FileOutputStream(tempFile))
        zos.putNextEntry(ZipEntry("word/document.xml"))
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p><w:r><w:t>Quarterly Financial Results for OmniChat</w:t></w:r></w:p>
                    <w:p><w:r><w:t>All objectives were achieved successfully.</w:t></w:r></w:p>
                </w:body>
            </w:document>
        """.trimIndent()
        zos.write(xmlContent.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val result = FileUtils.extractTextFromDocx(tempFile)
        assertTrue(result.contains("Word Document"))
        assertTrue(result.contains("Quarterly Financial Results for OmniChat"))
        assertTrue(result.contains("All objectives were achieved successfully."))
        tempFile.delete()
    }

    @Test
    fun testExtractTextFromXlsxZip() {
        val tempFile = File.createTempFile("test_sheet", ".xlsx")
        val zos = ZipOutputStream(FileOutputStream(tempFile))

        zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
        val sharedXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <sst count="2" uniqueCount="2">
                <si><t>Revenue</t></si>
                <si><t>Profit</t></si>
            </sst>
        """.trimIndent()
        zos.write(sharedXml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <worksheet>
                <sheetData>
                    <row r="1">
                        <c r="A1" t="s"><v>0</v></c>
                        <c r="B1"><v>500000</v></c>
                    </row>
                    <row r="2">
                        <c r="A2" t="s"><v>1</v></c>
                        <c r="B2"><v>150000</v></c>
                    </row>
                </sheetData>
            </worksheet>
        """.trimIndent()
        zos.write(sheetXml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val result = FileUtils.extractTextFromXlsx(tempFile)
        assertTrue(result.contains("Excel Spreadsheet"))
        assertTrue(result.contains("Revenue"))
        assertTrue(result.contains("500000"))
        assertTrue(result.contains("Profit"))
        assertTrue(result.contains("150000"))
        tempFile.delete()
    }

    @Test
    fun testInspectZipArchive() {
        val tempFile = File.createTempFile("test_archive", ".zip")
        val zos = ZipOutputStream(FileOutputStream(tempFile))
        zos.putNextEntry(ZipEntry("main.py"))
        zos.write("print('Hello from archive')".toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.putNextEntry(ZipEntry("data.json"))
        zos.write("{\"key\":\"value\"}".toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val result = FileUtils.inspectZipArchive(tempFile)
        assertTrue(result.contains("ZIP Archive"))
        assertTrue(result.contains("main.py"))
        assertTrue(result.contains("data.json"))
        tempFile.delete()
    }

    @Test
    fun testReadFullTextContentGeneral() {
        runBlocking {
            val tempFile = File.createTempFile("code_sample", ".kt")
            tempFile.writeText("package com.example\n\nfun main() {\n    println(\"OmniChat AI\")\n}\n")

            val result = FileUtils.readFullTextContent(tempFile)
            assertTrue(result.contains("println(\"OmniChat AI\")"))
            tempFile.delete()
        }
    }

    @Test
    fun testDeviceStorageStats() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stats = FileUtils.getDeviceStorageStats(context)
        assertTrue(stats.totalBytes > 0)
        assertTrue(stats.availableBytes > 0)
        assertTrue(stats.totalFormatted.isNotBlank())
        assertTrue(stats.availableFormatted.isNotBlank())
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("500 B", FileUtils.formatFileSize(500))
        assertEquals("1.5 KB", FileUtils.formatFileSize(1536))
        assertEquals("2.0 MB", FileUtils.formatFileSize(2 * 1024 * 1024))
    }
}
