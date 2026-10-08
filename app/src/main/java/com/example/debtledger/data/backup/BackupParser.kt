package com.example.debtledger.data.backup

import kotlinx.serialization.json.*

enum class BackupFileType { PLAIN_JSON, ENCRYPTED_ENVELOPE }

object BackupParser {

    private const val MAX_FILE_SIZE_CHARS = 15 * 1024 * 1024
    private const val MAX_DEPTH = 5
    private const val MAX_RECORDS = 50000

    fun detectFileTypeAndValidateStructure(jsonString: String): BackupFileType {
        if (jsonString.length > MAX_FILE_SIZE_CHARS) {
            throw IllegalArgumentException("حجم ملف النسخة الاحتياطية يتجاوز الحد الأقصى المسموح (15 ميجابايت)")
        }

        // 1. Strict streaming validation for duplicate keys, depth, field lengths, and total records
        validateJsonStreaming(jsonString)

        // 2. Parse JsonElement
        val root = try {
            Json.parseToJsonElement(jsonString)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("صيغة JSON غير صالحة")
        }

        return if (root is JsonObject && root.containsKey("header") && root.containsKey("ciphertext")) {
            BackupFileType.ENCRYPTED_ENVELOPE
        } else {
            BackupFileType.PLAIN_JSON
        }
    }

    private fun getFieldLengthLimit(key: String?): Int {
        return when (key) {
            "name" -> 200
            "phone" -> 30
            "description" -> 500
            "notes", "reason" -> 2000
            "beforeJson", "afterJson" -> 5000
            "ciphertext" -> MAX_FILE_SIZE_CHARS
            else -> 2000
        }
    }

    fun decodeJsonStringEscape(str: String): String {
        if (!str.contains('\\')) return str
        val sb = StringBuilder()
        var i = 0
        val len = str.length
        while (i < len) {
            val c = str[i]
            if (c == '\\' && i + 1 < len) {
                when (str[i + 1]) {
                    'u' -> {
                        if (i + 6 <= len) {
                            val hex = str.substring(i + 2, i + 6)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 6
                                continue
                            }
                        }
                        sb.append('\\')
                        i++
                    }
                    '"' -> { sb.append('"'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '/' -> { sb.append('/'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000C'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    else -> { sb.append(c); i++ }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun validateJsonStreaming(jsonString: String) {
        var depth = 0
        val keysStack = ArrayDeque<MutableSet<String>>()
        var i = 0
        val len = jsonString.length

        var currentKey: String? = null
        val arrayKeyStack = ArrayDeque<String?>()
        var totalRecords = 0

        val recordArrayNames = setOf("persons", "debts", "payments", "auditEvents")

        while (i < len) {
            val c = jsonString[i]
            when (c) {
                '{' -> {
                    depth++
                    if (depth > MAX_DEPTH) {
                        throw IllegalArgumentException("عمق هيكل JSON يتجاوز الحد الأقصى المسموح (5 مستويات)")
                    }
                    keysStack.addLast(HashSet())

                    // If we are inside one of the record arrays, each object element represents a record!
                    if (arrayKeyStack.lastOrNull() in recordArrayNames) {
                        totalRecords++
                        if (totalRecords > MAX_RECORDS) {
                            throw IllegalArgumentException("عدد السجلات الكلي ($totalRecords) يتجاوز الحد الأقصى المسموح ($MAX_RECORDS)")
                        }
                    }

                    currentKey = null
                    i++
                }
                '[' -> {
                    depth++
                    if (depth > MAX_DEPTH) {
                        throw IllegalArgumentException("عمق هيكل JSON يتجاوز الحد الأقصى المسموح (5 مستويات)")
                    }
                    arrayKeyStack.addLast(currentKey)
                    currentKey = null
                    i++
                }
                '}' -> {
                    if (keysStack.isNotEmpty()) keysStack.removeLast()
                    depth--
                    currentKey = null
                    i++
                }
                ']' -> {
                    if (arrayKeyStack.isNotEmpty()) arrayKeyStack.removeLast()
                    depth--
                    currentKey = null
                    i++
                }
                '"' -> {
                    // Read string literal
                    val startPos = i + 1
                    var p = startPos
                    var unescapedLen = 0
                    var isEscaped = false

                    while (p < len) {
                        val ch = jsonString[p]
                        if (isEscaped) {
                            if (ch == 'u') {
                                p += 4 // Skip 4 hex digits
                            }
                            unescapedLen++
                            isEscaped = false
                        } else if (ch == '\\') {
                            isEscaped = true
                        } else if (ch == '"') {
                            break
                        } else {
                            unescapedLen++
                        }
                        p++
                    }

                    if (p >= len) {
                        throw IllegalArgumentException("صيغة JSON غير صالحة")
                    }

                    // Check if string is a key or a value
                    var nextIndex = p + 1
                    while (nextIndex < len && jsonString[nextIndex].isWhitespace()) {
                        nextIndex++
                    }

                    val isKey = nextIndex < len && jsonString[nextIndex] == ':'

                    if (isKey) {
                        val rawKeyStr = jsonString.substring(startPos, p)
                        val decodedKey = decodeJsonStringEscape(rawKeyStr)
                        val currentScopeKeys = keysStack.lastOrNull()
                        if (currentScopeKeys != null) {
                            if (!currentScopeKeys.add(decodedKey)) {
                                throw IllegalArgumentException("مفتاح مكرر في JSON: $decodedKey")
                            }
                        }
                        currentKey = decodedKey
                        i = nextIndex + 1 // Skip closing quote and ':'
                    } else {
                        // It's a string value! Validate string length
                        val limit = getFieldLengthLimit(currentKey)
                        if (unescapedLen > limit) {
                            throw IllegalArgumentException("طول الحقل $currentKey يتجاوز الحد الأقصى المسموح ($limit حرف)")
                        }
                        currentKey = null
                        i = p + 1 // Skip closing quote
                    }
                }
                ',' -> {
                    currentKey = null
                    i++
                }
                else -> {
                    i++
                }
            }
        }
    }
}
