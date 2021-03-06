
/*
 * Copyright (C) 2020 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.core

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputContentInfo
import androidx.annotation.RequiresApi
import dev.patrickgold.florisboard.ime.text.key.KeyCode

// Constants for detectLastUnicodeCharacterLengthBeforeCursor method
private const val LIGHT_SKIN_TONE           = 0x1F3FB
private const val MEDIUM_LIGHT_SKIN_TONE    = 0x1F3FC
private const val MEDIUM_SKIN_TONE          = 0x1F3FD
private const val MEDIUM_DARK_SKIN_TONE     = 0x1F3FE
private const val DARK_SKIN_TONE            = 0x1F3FF
private const val RED_HAIR                  = 0x1F9B0
private const val CURLY_HAIR                = 0x1F9B1
private const val WHITE_HAIR                = 0x1F9B2
private const val BALD                      = 0x1F9B3
private const val ZERO_WIDTH_JOINER         =  0x200D
private const val VARIATION_SELECTOR        =  0xFE0F

// Array which holds all variations for easier checking (convenience only)
private val emojiVariationArray: Array<Int> = arrayOf(
        LIGHT_SKIN_TONE,
        MEDIUM_LIGHT_SKIN_TONE,
        MEDIUM_SKIN_TONE,
        MEDIUM_DARK_SKIN_TONE,
        DARK_SKIN_TONE,
        RED_HAIR,
        CURLY_HAIR,
        WHITE_HAIR,
        BALD
)

// VinaKey characters
private val baseCharMap = mapOf(
    "Á" to "A",
    "À" to "A",
    "Ả" to "A",
    "Ã" to "A",
    "Ạ" to "A",

    "á" to "a",
    "à" to "a",
    "ả" to "a",
    "ã" to "a",
    "ạ" to "a",

    "Ắ" to "Ă",
    "Ằ" to "Ă",
    "Ẳ" to "Ă",
    "Ẵ" to "Ă",
    "Ặ" to "Ă",

    "ắ" to "ă",
    "ằ" to "ă",
    "ẳ" to "ă",
    "ẵ" to "ă",
    "ặ" to "ă",

    "Ấ" to "Â",
    "Ầ" to "Â",
    "Ẩ" to "Â",
    "Ẫ" to "Â",
    "Ậ" to "Â",

    "ấ" to "â",
    "ầ" to "â",
    "ẩ" to "â",
    "ẫ" to "â",
    "ậ" to "â",

    "É" to "E",
    "È" to "E",
    "Ẻ" to "E",
    "Ẽ" to "E",
    "Ẹ" to "E",

    "é" to "e",
    "è" to "e",
    "ẻ" to "e",
    "ẽ" to "e",
    "ẹ" to "e",

    "Ế" to "Ê",
    "Ề" to "Ê",
    "Ể" to "Ê",
    "Ễ" to "Ê",
    "Ệ" to "Ê",

    "ế" to "ê",
    "ề" to "ê",
    "ể" to "ê",
    "ễ" to "ê",
    "ệ" to "ê",

    "Í" to "I",
    "Ì" to "I",
    "Ỉ" to "I",
    "Ĩ" to "I",
    "Ị" to "I",

    "í" to "i",
    "ì" to "i",
    "ỉ" to "i",
    "ĩ" to "i",
    "ị" to "i",

    "Ó" to "O",
    "Ò" to "O",
    "Ỏ" to "O",
    "Õ" to "O",
    "Ọ" to "O",

    "ó" to "o",
    "ò" to "o",
    "ỏ" to "o",
    "õ" to "o",
    "ọ" to "o",

    "Ố" to "Ô",
    "Ồ" to "Ô",
    "Ổ" to "Ô",
    "Ỗ" to "Ô",
    "Ộ" to "Ô",

    "ố" to "ô",
    "ồ" to "ô",
    "ổ" to "ô",
    "ỗ" to "ô",
    "ộ" to "ô",

    "Ớ" to "Ơ",
    "Ờ" to "Ơ",
    "Ở" to "Ơ",
    "Ỡ" to "Ơ",
    "Ợ" to "Ơ",

    "ớ" to "ơ",
    "ờ" to "ơ",
    "ở" to "ơ",
    "ỡ" to "ơ",
    "ợ" to "ơ",

    "Ú" to "U",
    "Ù" to "U",
    "Ủ" to "U",
    "Ũ" to "U",
    "Ụ" to "U",

    "ú" to "u",
    "ù" to "u",
    "ủ" to "u",
    "ũ" to "u",
    "ụ" to "u",

    "Ứ" to "Ư",
    "Ừ" to "Ư",
    "Ử" to "Ư",
    "Ữ" to "Ư",
    "Ự" to "Ư",

    "ứ" to "ư",
    "ừ" to "ư",
    "ử" to "ư",
    "ữ" to "ư",
    "ự" to "ư",

    "Ý" to "Y",
    "Ỳ" to "Y",
    "Ỷ" to "Y",
    "Ỹ" to "Y",
    "Ỵ" to "Y",

    "ý" to "y",
    "ỳ" to "y",
    "ỷ" to "y",
    "ỹ" to "y",
    "ỵ" to "y"
)

private val priorityChars = arrayOf(
    arrayOf(
        "A",
        "a",
        "Ă",
        "ă",
        "Â",
        "â"
    ),
    arrayOf(
        "E",
        "e",
        "Ê",
        "ê"
    ),
    arrayOf(
        "O",
        "o",
        "Ô",
        "ô",
        "Ơ",
        "ơ"
    ),
    arrayOf(
        "Y",
        "y"
    ),
    arrayOf(
        "U",
        "u",
        "Ư",
        "ư"
    ),
    arrayOf(
        "I",
        "i",
    )
)
private val combineChar1Map = mapOf(
    "A" to "Á",
    "a" to "á",
    "Ă" to "Ắ",
    "ă" to "ắ",
    "Â" to "Ấ",
    "â" to "ấ",
    "E" to "É",
    "e" to "é",
    "Ê" to "Ế",
    "ê" to "ế",
    "I" to "Í",
    "i" to "í",
    "O" to "Ó",
    "o" to "ó",
    "Ô" to "Ố",
    "ô" to "ố",
    "Ơ" to "Ớ",
    "ơ" to "ớ",
    "U" to "Ú",
    "u" to "ú",
    "Ư" to "Ứ",
    "ư" to "ứ",
    "Y" to "Ý",
    "y" to "ý"
)
private val combineChar2Map = mapOf(
    "A" to "À",
    "a" to "à",
    "Ă" to "Ằ",
    "ă" to "ằ",
    "Â" to "Ầ",
    "â" to "ầ",
    "E" to "È",
    "e" to "è",
    "Ê" to "Ề",
    "ê" to "ề",
    "I" to "Ì",
    "i" to "ì",
    "O" to "Ò",
    "o" to "ò",
    "Ô" to "Ồ",
    "ô" to "ồ",
    "Ơ" to "Ờ",
    "ơ" to "ờ",
    "U" to "Ù",
    "u" to "ù",
    "Ư" to "Ừ",
    "ư" to "ừ",
    "Y" to "Ỳ",
    "y" to "ỳ"
)
private val combineChar3Map = mapOf(
    "A" to "Ả",
    "a" to "ả",
    "Ă" to "Ẳ",
    "ă" to "ẳ",
    "Â" to "Ẩ",
    "â" to "ẩ",
    "E" to "Ẻ",
    "e" to "ẻ",
    "Ê" to "Ể",
    "ê" to "ể",
    "I" to "Ỉ",
    "i" to "ỉ",
    "O" to "Ỏ",
    "o" to "ỏ",
    "Ô" to "Ổ",
    "ô" to "ổ",
    "Ơ" to "Ở",
    "ơ" to "ở",
    "U" to "Ủ",
    "u" to "ủ",
    "Ư" to "Ử",
    "ư" to "ử",
    "Y" to "Ỷ",
    "y" to "ỷ"
)
private val combineChar4Map = mapOf(
    "A" to "Ã",
    "a" to "ã",
    "Ă" to "Ẵ",
    "ă" to "ẵ",
    "Â" to "Ẫ",
    "â" to "ẫ",
    "E" to "Ẽ",
    "e" to "ẽ",
    "Ê" to "Ễ",
    "ê" to "ễ",
    "I" to "Ĩ",
    "i" to "ĩ",
    "O" to "Õ",
    "o" to "õ",
    "Ô" to "Ỗ",
    "ô" to "ỗ",
    "Ơ" to "Ỡ",
    "ơ" to "ỡ",
    "U" to "Ũ",
    "u" to "ũ",
    "Ư" to "Ữ",
    "ư" to "ữ",
    "Y" to "Ỹ",
    "y" to "ỹ"
)
private val combineChar5Map = mapOf(
    "A" to "Ạ",
    "a" to "ạ",
    "Ă" to "Ặ",
    "ă" to "ặ",
    "Â" to "Ậ",
    "â" to "ậ",
    "E" to "Ẹ",
    "e" to "ẹ",
    "Ê" to "Ệ",
    "ê" to "ệ",
    "I" to "Ị",
    "i" to "ị",
    "O" to "Ọ",
    "o" to "ọ",
    "Ô" to "Ộ",
    "ô" to "ộ",
    "Ơ" to "Ợ",
    "ơ" to "ợ",
    "U" to "Ụ",
    "u" to "ụ",
    "Ư" to "Ự",
    "ư" to "ự",
    "Y" to "Ỵ",
    "y" to "ỵ"
)
private val combineChar6Map = mapOf(
    "A" to "Â",
    "a" to "â",
    "Á" to "Ấ",
    "á" to "ấ",
    "À" to "Ầ",
    "à" to "ầ",
    "Ả" to "Ẩ",
    "ả" to "ẩ",
    "Ã" to "Ẫ",
    "ã" to "ẫ",
    "Ạ" to "Ậ",
    "ạ" to "ậ",
    "E" to "Ê",
    "e" to "ê",
    "É" to "Ế",
    "é" to "ế",
    "È" to "Ề",
    "è" to "ề",
    "Ẻ" to "Ể",
    "ẻ" to "ể",
    "Ẽ" to "Ễ",
    "ẽ" to "ễ",
    "Ẹ" to "Ệ",
    "ẹ" to "ệ",
    "O" to "Ô",
    "o" to "ô",
    "Ó" to "Ố",
    "ó" to "ố",
    "Ò" to "Ồ",
    "ò" to "ồ",
    "Ỏ" to "Ổ",
    "ỏ" to "ổ",
    "Õ" to "Ỗ",
    "õ" to "ỗ",
    "Ọ" to "Ộ",
    "ọ" to "Ộ"
)
private val combineChar7Map = mapOf(
    "UO" to "ƯƠ",
    "uo" to "ươ",
    "UÓ" to "ƯỚ",
    "uó" to "ướ",
    "UÒ" to "ƯỜ",
    "uò" to "ườ",
    "UỎ" to "ƯỞ",
    "uỏ" to "ưở",
    "UÕ" to "ƯỠ",
    "uõ" to "ưỡ",
    "UỌ" to "ƯỢ",
    "uọ" to "ượ",
    "O" to "Ơ",
    "o" to "ơ",
    "Ó" to "Ớ",
    "ó" to "ớ",
    "Ò" to "Ờ",
    "ò" to "ờ",
    "Ỏ" to "Ở",
    "ỏ" to "ở",
    "Õ" to "Ỡ",
    "õ" to "ỡ",
    "Ọ" to "Ợ",
    "ọ" to "ợ",
    "U" to "Ư",
    "u" to "ư",
    "Ú" to "Ứ",
    "ú" to "ứ",
    "Ù" to "Ừ",
    "ù" to "ừ",
    "Ủ" to "Ử",
    "ủ" to "ử",
    "Ũ" to "Ữ",
    "ũ" to "ữ",
    "Ụ" to "Ự",
    "ụ" to "ự"
)
private val combineChar8Map = mapOf(
    "A" to "Ă",
    "a" to "ă",
    "Á" to "Ắ",
    "á" to "ắ",
    "À" to "Ằ",
    "à" to "ằ",
    "Ả" to "Ẳ",
    "ả" to "ẳ",
    "Ã" to "Ẵ",
    "ã" to "ẵ",
    "Ạ" to "Ặ",
    "ạ" to "ặ",
)
private val combineChar9Map = mapOf(
    "D" to "Đ",
    "d" to "đ"
)
private val combineCharGMap = mapOf(
    "NG" to "Ŋ",
    "Ng" to "Ŋ",
    "ng" to "ŋ"
)
private val combineCharHMap = mapOf(
    "NH" to "Ñ",
    "Nh" to "Ñ",
    "nh" to "ñ",
    "ŊH" to "Ŋ",
    "Ŋh" to "Ŋ",
    "ŋh" to "ŋ"
)
private val combineCharIMap = mapOf(
    "GI" to "J",
    "Gi" to "J",
    "gi" to "j"
)
/**
 * Class which holds information relevant to an editor instance like the input [cachedText], [selection],
 * [inputAttributes], [imeOptions], etc. This class is thought to be an improved [EditorInfo]
 * object which also holds the state of the currently focused input editor.
 */
class EditorInstance private constructor(private val ims: InputMethodService?) {
    var contentMimeTypes: Array<out String?>? = null
    val cursorCapsMode: InputAttributes.CapsMode
        get() {
            val ic = ims?.currentInputConnection ?: return InputAttributes.CapsMode.NONE
            return InputAttributes.CapsMode.fromFlags(
                    ic.getCursorCapsMode(inputAttributes.capsMode.toFlags())
            )
        }
    var currentWord: Region = Region(this)
        private set
    var imeOptions: ImeOptions = ImeOptions.fromImeOptionsInt(EditorInfo.IME_NULL)
        private set
    var inputAttributes: InputAttributes = InputAttributes.fromInputTypeInt(InputType.TYPE_NULL)
        private set
    var isComposingEnabled: Boolean = false
        set(v) {
            field = v
            reevaluateCurrentWord()
            if (v && !isRawInputEditor) {
                markComposingRegion(currentWord)
            } else {
                markComposingRegion(null)
            }
        }
    var isNewSelectionInBoundsOfOld: Boolean = false
        private set
    var isPrivateMode: Boolean = false
    var isRawInputEditor: Boolean = true
        private set
    var packageName: String = "undefined"
        private set
    var selection: Selection = Selection(this)
        private set
    var cachedText: String = ""

    private var clipboardManager: ClipboardManager? = null

    init {
        val tmpClipboardManager = ims?.getSystemService(Context.CLIPBOARD_SERVICE)
        if (tmpClipboardManager != null && tmpClipboardManager is ClipboardManager) {
            clipboardManager = tmpClipboardManager
        }
    }

    companion object {
        fun default(): EditorInstance {
            return EditorInstance(null)
        }

        fun from(editorInfo: EditorInfo?, ims: InputMethodService?): EditorInstance {
            return if (editorInfo == null) { default() } else {
                EditorInstance(ims).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        contentMimeTypes = editorInfo.contentMimeTypes
                    }
                    imeOptions = ImeOptions.fromImeOptionsInt(editorInfo.imeOptions)
                    inputAttributes = InputAttributes.fromInputTypeInt(editorInfo.inputType)
                    packageName = editorInfo.packageName
                    /*selection = Selection(this).apply {
                        start = editorInfo.initialSelStart
                        end = editorInfo.initialSelEnd
                    }*/
                }
            }
        }
    }

    init {
        updateEditorState()
        reevaluateCurrentWord()
    }

    /**
     * Event handler which reacts to selection updates coming from the target app's editor.
     */
    fun onUpdateSelection(
            oldSelStart: Int, oldSelEnd: Int,
            newSelStart: Int, newSelEnd: Int
    ) {
        updateEditorState()
        isNewSelectionInBoundsOfOld =
            newSelStart >= (oldSelStart - 1) &&
            newSelStart <= (oldSelStart + 1) &&
            newSelEnd >= (oldSelEnd - 1) &&
            newSelEnd <= (oldSelEnd + 1)
        selection.apply {
            start = newSelStart
            end = newSelEnd
        }
        reevaluateCurrentWord()
        if (selection.isCursorMode && isComposingEnabled && !isRawInputEditor) {
            markComposingRegion(currentWord)
        } else {
            markComposingRegion(null)
        }
    }

    /**
     * Completes the given [text] in the current composing region. Does nothing if the current
     * composing region is of zero length or null.
     *
     * @param text The text to complete in this editor's composing region.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun commitCompletion(text: String): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return if (isRawInputEditor) {
            false
        } else {
            ic.beginBatchEdit()
            ic.setComposingText(text, 1)
            markComposingRegion(null)
            updateEditorState()
            reevaluateCurrentWord()
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Commits the given [content] to this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @param content The content to commit.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun commitContent(content: Uri, description: ClipDescription): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        val contentMimeTypes = contentMimeTypes
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 || contentMimeTypes == null || contentMimeTypes.isEmpty()) {
            commitText(content.toString())
        } else {
            var mimeTypesDoMatch = false
            for (contentMimeType in contentMimeTypes) {
                if (description.hasMimeType(contentMimeType)) {
                    mimeTypesDoMatch = true
                    break
                }
            }
            if (mimeTypesDoMatch) {
                ic.beginBatchEdit()
                markComposingRegion(null)
                val ret = ic.commitContent(InputContentInfo(content, description), 0, null)
                ic.endBatchEdit()
                ret
            } else {
                commitText(content.toString())
            }
        }
    }

    /**
     * Commits the given [text] to this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * This method overwrites any selected text and replaces it with given [text]. If there is no
     * text selected (selection is in cursor mode), then this method will insert the [text] after
     * the cursor, then set the cursor position to the first character after the inserted text.
     *
     * @param text The text to commit.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun commitText(text: String): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return if (isRawInputEditor) {
            ic.commitText(text, 1)
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)
            ic.commitText(text, 1)
            updateEditorState()
            reevaluateCurrentWord()
            if (isComposingEnabled) {
                markComposingRegion(currentWord)
                vinaKey(text)
            }
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Executes a backward delete on this editor's text. If a text selection is active, all
     * characters inside this selection will be removed, else only the left-most character from
     * the cursor's position.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun deleteBackwards(): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return if (isRawInputEditor) {
            sendSystemKeyEvent(KeyEvent.KEYCODE_DEL)
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)
            sendSystemKeyEvent(KeyEvent.KEYCODE_DEL)
            updateEditorState()
            reevaluateCurrentWord()
            if (isComposingEnabled) {
                markComposingRegion(currentWord)
            }
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Deletes [n] words before the current cursor's position.
     * NOTE: this implementation does currently only delete currentWord. This is due to change in
     * future versions.
     *
     * @param n The number of words to delete before the cursor. Must be greater than 0 or this
     *  method will fail.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun deleteWordsBeforeCursor(n: Int): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return if (n < 1 || isRawInputEditor || !selection.isValid || !selection.isCursorMode) {
            false
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)

            getWordsInString(cachedText.substring(0, selection.start)).run {
                get(size - n.coerceAtLeast(0)).range
            }.run {
                ic.setSelection(first, selection.start)
            }

            ic.commitText("", 1)

            updateEditorState()
            reevaluateCurrentWord()
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Finds all words in the given string with the correct regex for current subtype.
     * TODO: currently only supports en-US
     *
     * @param string String to select words from
     * @return Words in [string] as a List of [MatchResult]
     */
    private fun getWordsInString(string: String):List<MatchResult>{
        val wordRegexPattern = "[\\p{L}]+".toRegex()
        return wordRegexPattern.findAll(
                string
        ).toList()
    }

    /**
     * Undoes the last action
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performUndo(): Boolean{
        val ic = ims?.currentInputConnection ?: return false
        return if (isRawInputEditor) {
            sendSystemKeyEventCtrl(KeyEvent.KEYCODE_Z)
            true
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)
            sendSystemKeyEventCtrl(KeyEvent.KEYCODE_Z)
            updateEditorState()
            reevaluateCurrentWord()
            if (isComposingEnabled) {
                markComposingRegion(currentWord)
            }
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Redoes the last Undo action
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performRedo(): Boolean{
        val ic = ims?.currentInputConnection ?: return false
        return if (isRawInputEditor) {
            sendSystemKeyEventCtrlShift(KeyEvent.KEYCODE_Z)
            true
        } else {
            ic.beginBatchEdit()
            markComposingRegion(null)
            sendSystemKeyEventCtrlShift(KeyEvent.KEYCODE_Z)
            updateEditorState()
            reevaluateCurrentWord()
            if (isComposingEnabled) {
                markComposingRegion(currentWord)
            }
            ic.endBatchEdit()
            true
        }
    }

    /**
     * Gets [n] characters after the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get after the cursor. Must be greater than 0 or this
     *  method will fail.
     *
     * @return [n] or less characters after the cursor.
     */
    fun getTextAfterCursor(n: Int): String {
        if (!selection.isValid || n < 1 || isRawInputEditor) {
            return ""
        }
        val from = selection.end.coerceIn(0, cachedText.length)
        val to = (selection.end + n).coerceIn(0, cachedText.length)
        return cachedText.substring(from, to)
    }

    /**
     * Gets [n] characters before the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get before the cursor. Must be greater than 0 or this
     *  method will fail.
     *
     * @return [n] or less characters after the cursor.
     */
    fun getTextBeforeCursor(n: Int): String {
        if (!selection.isValid || n < 1 || isRawInputEditor) {
            return ""
        }
        val from = (selection.start - n).coerceIn(0, cachedText.length)
        val to = selection.start.coerceIn(0, cachedText.length)
        return cachedText.substring(from, to)
    }

    /**
     * Performs a cut command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCut(): Boolean {
        return if (selection.isSelectionMode) {
            val clipData: ClipData = ClipData.newPlainText(selection.text, selection.text)
            clipboardManager?.setPrimaryClip(clipData)
            deleteBackwards()
            true
        } else {
            false
        }
    }

    /**
     * Performs a copy command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCopy(): Boolean {
        return if (selection.isSelectionMode) {
            val clipData: ClipData = ClipData.newPlainText(selection.text, selection.text)
            clipboardManager?.setPrimaryClip(clipData)
            setSelection(selection.end, selection.end)
            true
        } else {
            false
        }
    }

    /**
     * Performs a paste command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardPaste(): Boolean {
//        val clipData: ClipData? = clipboardManager?.primaryClip
//        val item: ClipData.Item? = clipData?.getItemAt(0)
//        return when {
//            item?.text != null -> {
//                commitText(item.text.toString())
//            }
//            item?.uri != null -> {
//                commitContent(item.uri, clipData.description)
//            }
//            else -> {
//                false
//            }
//        }
        sendSystemKeyEventCtrl(KeyEvent.KEYCODE_V)
        return true
    }

    /**
     * Performs an enter key press on the current input editor.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performEnter(): Boolean {
        return if (isRawInputEditor) {
            sendSystemKeyEvent(KeyEvent.KEYCODE_ENTER)
        } else {
            commitText("\n")
        }
    }

    /**
     * Performs a given [action] on the current input editor.
     *
     * @param action The action to be performed on this editor instance.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performEnterAction(action: ImeOptions.Action): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return ic.performEditorAction(action.toInt())
    }

    /**
     * Sends a given [keyCode] as a [KeyEvent.ACTION_DOWN].
     *
     * @param keyCode The key code to send, use a key code defined in Android's [KeyEvent], not in
     *  [KeyCode] or this call may send a weird character, as this key codes do not match!!
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun sendSystemKeyEvent(keyCode: Int): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }

    /**
     * Sends a given [keyCode] as a [KeyEvent.ACTION_DOWN] with ALT pressed.
     *
     * @param keyCode The key code to send, use a key code defined in Android's [KeyEvent], not in
     *  [KeyCode] or this call may send a weird character, as this key codes do not match!!
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun sendSystemKeyEventAlt(keyCode: Int): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return ic.sendKeyEvent(
                KeyEvent(
                        0,
                        1,
                        KeyEvent.ACTION_DOWN, keyCode,
                        0,
                        KeyEvent.META_ALT_LEFT_ON
                )
        )
    }

    /**
     * Sends a given [keyCode] with Ctrl pressed with [sendDownUpKeyEvents]
     *
     * @param keyCode The key code to send, use a key code defined in Android's [KeyEvent], not in
     *  [KeyCode] or this call may send a weird character, as this key codes do not match!!
     */
    fun sendSystemKeyEventCtrl(keyCode: Int) {
        val ic = ims?.currentInputConnection ?: return
        sendDownUpKeyEvents(keyCode, KeyEvent.META_CTRL_ON)
    }


    /**
     * Sends a given [keyCode] with Ctrl and Shift pressed with [sendDownUpKeyEvents]
     *
     * @param keyCode The key code to send, use a key code defined in Android's [KeyEvent], not in
     *  [KeyCode] or this call may send a weird character, as this key codes do not match!!
     */
    fun sendSystemKeyEventCtrlShift(keyCode: Int) {
        val ic = ims?.currentInputConnection ?: return
        sendDownUpKeyEvents(keyCode, KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON)
    }

    /**
     * Same as [InputMethodService.sendDownUpKeyEvents] but also allows to set metaStae
     *
     * @param keyEventCode The key code to send, use a key code defined in Android's [KeyEvent]
     * @param metaState Flags indicating which meta keys are currently pressed.
     */
    fun sendDownUpKeyEvents(keyEventCode: Int, metaState: Int) {
        val ic = ims?.currentInputConnection ?: return
        val eventTime = SystemClock.uptimeMillis()
        ic.sendKeyEvent(
                KeyEvent(
                        eventTime,
                        eventTime,
                        KeyEvent.ACTION_DOWN,
                        keyEventCode,
                        0,
                        metaState,
                        KeyCharacterMap.VIRTUAL_KEYBOARD,
                        0,
                        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
                )
        )
        ic.sendKeyEvent(
                KeyEvent(
                        eventTime,
                        SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_UP,
                        keyEventCode,
                        0,
                        metaState,
                        KeyCharacterMap.VIRTUAL_KEYBOARD,
                        0,
                        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
                )
        )
    }

    /**
     * Sets the selection region of this instance and notifies the input connection.
     *
     * @param from The start index of the selection in characters (inclusive).
     * @param to The end index of the selection in characters (exclusive).
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun setSelection(from: Int, to: Int): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return if (isRawInputEditor) {
            selection.apply {
                start = -1
                end = -1
            }
            false
        } else {
            selection.apply {
                start = from
                end = to
            }
            ic.setSelection(from, to)
        }
    }

    /**
     *Adds one word on the left of selection to it
     *
     * @return True on success, false if no new words are selected
     */
    fun leftAppendWordToSelection(): Boolean{
        // no words left to select
        if (selection.start <= 0)
            return false
        val stringBeforeSelection = cachedText.substring(
                0,
                selection.start
        )
        getWordsInString(stringBeforeSelection).last().range.apply {
            setSelection(first, selection.end)
        }
        return true
    }

    /**
     *Removes one word on the left from the selection
     *
     * @return True on success, false if no new words are deselected
     */
    fun leftPopWordFromSelection(): Boolean{
        // no words left to pop
        if (selection.start >= selection.end)
            return false
        val stringInsideSelection = cachedText.substring(
                selection.start,
                selection.end
        )
        getWordsInString(stringInsideSelection).first().range.apply {
            setSelection(selection.start + last + 1, selection.end)
        }
        return true
    }

    /**
     * Detects the length of the character before the cursor, as many Unicode characters nowadays
     * are longer than 1 Java char and thus the length has to be calculated in order to avoid
     * deleting only half of an emoji...
     * Is used primarily in [deleteBackwards].
     *
     * @return The length of the last Unicode character, in Java characters or 0 if the current
     *  selection is invalid.
     */
    private fun detectLastUnicodeCharacterLengthBeforeCursor(): Int {
        if (!selection.isValid) {
            return 0
        }
        var charIndex = 0
        var charLength = 0
        var charShouldGlue = false
        val textToSearch = cachedText.substring(0, selection.start.coerceAtMost(cachedText.length))
        var i = 0
        while (i < textToSearch.length) {
            val cp = textToSearch.codePointAt(i)
            val cpLength = Character.charCount(cp)
            when {
                charShouldGlue || cp == VARIATION_SELECTOR || emojiVariationArray.contains(cp) -> {
                    charLength += cpLength
                    charShouldGlue = false
                }
                cp == ZERO_WIDTH_JOINER -> {
                    charLength += cpLength
                    charShouldGlue = true
                }
                else -> {
                    charIndex = i
                    charLength = 0
                    charShouldGlue = false
                }
            }
            i += cpLength
        }
        return textToSearch.length - charIndex
    }

    /**
     * Marks a given [region] as composing and notifies the input connection.
     *
     * @param region The region which should be marked as composing.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    private fun markComposingRegion(region: Region?): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        return when (region) {
            null -> ic.finishComposingText()
            else -> if (region.isValid) {
                ic.setComposingRegion(region.start, region.end)
            } else {
                ic.finishComposingText()
            }
        }
    }

    /**
     * Evaluates the current word in this editor instance based on the current cursor position and
     * given delimiter [regex].
     *
     * @param regex The delimiter regex which should be used to split up the content text and find
     *  words. May differ from locale to locale.
     *
     * @return True on success, false if no current word could be found.
     */
    private fun reevaluateCurrentWord(regex: Regex): Boolean {
        var foundValidWord = false
        if (selection.isValid && selection.isCursorMode) {
            val words = cachedText.split("((?<=$regex)|(?=$regex))".toRegex())
            var pos = 0
            for (word in words) {
                if (selection.start >= pos && selection.start <= pos + word.length &&
                    word.isNotEmpty() && !word.matches(regex)) {
                    currentWord.apply {
                        start = pos
                        end = pos + word.length
                    }
                    foundValidWord = true
                    break
                } else {
                    pos += word.length
                }
            }
        }
        if (!foundValidWord) {
            currentWord.apply {
                start = -1
                end = -1
            }
        }
        return foundValidWord
    }

    /**
     * Evaluates the current word with the correct delimiter regex for current subtype.
     * TODO: currently only supports en-US
     */
    private fun reevaluateCurrentWord() {
        val regex = "[^\\p{L}\\p{N}]".toRegex()
        reevaluateCurrentWord(regex)
    }

    /**
     * Gets the current text from the app's editor view.
     *
     * @return The target editor's content string.
     */
    private fun updateEditorState() {
        val ic = ims?.currentInputConnection
        val et = ic?.getExtractedText(
                ExtractedTextRequest(), 0
        )
        val text = et?.text
        if (ic == null || et == null || text == null) {
            isRawInputEditor = true
            cachedText = ""
            selection.apply {
                start = -1
                end = -1
            }
        } else {
            isRawInputEditor = false
            cachedText = text.toString()
            selection.apply {
                start = et.selectionStart.coerceAtMost(cachedText.length)
                end = et.selectionEnd.coerceAtMost(cachedText.length)
            }
        }
        reevaluateCurrentWord()
    }

    private fun vinaKeyVniReplace(key: String, input: String, combineCharMap: Map<String, String>): String {
        if ("\\d+${key}\\s*$".toRegex().containsMatchIn(input) || "${key}${key}$".toRegex().containsMatchIn(input)) {
            return  input
        }

        var output = input
        var fnKeyNum = (key.single().toByte().toInt() - 48)

        loop@ for ((k, v) in combineCharMap) {
            if ("${v}${key}$".toRegex().containsMatchIn(output)) {
                output = output.replace("${v}${key}$".toRegex(), "${k}${key}")
                break@loop
            }
        }

        if (output == input) {
            if (fnKeyNum > 0 && fnKeyNum < 6) {
                loop@ for (char in input) {
                    val search = char.toString()
                    val replacement = baseCharMap[search].toString()
                    if (replacement != "null") {
                        output = output.replace(search, replacement)
                    }
                }

                loop@ for (priorityChar in priorityChars) {
                    var isFound = false
                    loop1@ for (char in priorityChar) {
                        val start: Int = output.lastIndexOf(char.toString())
                        if (start > -1) {
                            val replacement = combineCharMap[char.toString()].toString()
                            val builder = StringBuilder()
                            builder.append(output.substring(0, start))
                            builder.append(replacement)
                            builder.append(output.substring(start + char.length))
                            output = builder.toString()
                            isFound = true
                            break@loop1
                        }
                    }

                    if (isFound) {
                        break@loop
                    }
                }
            } else {
                loop@ for ((k, v) in combineCharMap) {
                    val start: Int = output.lastIndexOf(k.toString())
                    if (start > -1) {
                        val builder = StringBuilder()
                        builder.append(output.substring(0, start))
                        builder.append(v.toString())
                        builder.append(output.substring(start + k.length))
                        output = builder.toString()
                        break@loop
                    }
                }
            }

            if (output != input) {
                output = output.replace("${key}$".toRegex(), "")
            }
        }

        return output
    }

    /**
     * Vni input method, get key and original string
     *
     * @return corrected string of original string
     */
    private fun vinaKeyVni(key: String, input: String): String {
        var output = input

        when (key) {
            "1" -> {
                output = vinaKeyVniReplace(key, input, combineChar1Map)
            }
            "2" -> {
                output = vinaKeyVniReplace(key, input, combineChar2Map)
            }
            "3" -> {
                output = vinaKeyVniReplace(key, input, combineChar3Map)
            }
            "4" -> {
                output = vinaKeyVniReplace(key, input, combineChar4Map)
            }
            "5" -> {
                output = vinaKeyVniReplace(key, input, combineChar5Map)
            }
            "6" -> {
                output = vinaKeyVniReplace(key, input, combineChar6Map)
            }
            "7" -> {
                output = vinaKeyVniReplace(key, input, combineChar7Map)
            }
            "8" -> {
                output = vinaKeyVniReplace(key, input, combineChar8Map)
            }
            "9" -> {
                output = vinaKeyVniReplace(key, input, combineChar9Map)
            }
            "g" -> {
                output = vinaKeyVniReplace(key, input, combineCharGMap)
            }
            "G" -> {
                output = vinaKeyVniReplace(key, input, combineCharGMap)
            }
            "h" -> {
                output = vinaKeyVniReplace(key, input, combineCharHMap)
            }
            "H" -> {
                output = vinaKeyVniReplace(key, input, combineCharHMap)
            }
            "i" -> {
                output = vinaKeyVniReplace(key, input, combineCharIMap)
            }
            "I" -> {
                output = vinaKeyVniReplace(key, input, combineCharIMap)
            }
        }

        return  output
    }

    /**
     * Gets the current text from the app's editor view.
     *
     * @return The target editor's content string.
     */
    private fun vinaKey(input: String): Boolean {
        val ic = ims?.currentInputConnection ?: return false
        val originalText = currentWord.text.toString()
        val correctedText = vinaKeyVni(input, originalText)

        if (correctedText != originalText) {
            ic.setComposingText(correctedText, 1)
            markComposingRegion(currentWord)
        }
        return true;
    }
}

/**
 * Class which holds the same information as an [EditorInfo.imeOptions] int but more accessible and
 * readable.
 */
class ImeOptions private constructor(imeOptions: Int) {
    val action: Action = Action.fromInt(imeOptions)
    val flagForceAscii: Boolean = imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII > 0
    val flagNavigateNext: Boolean = imeOptions and EditorInfo.IME_FLAG_NAVIGATE_NEXT > 0
    val flagNavigatePrevious: Boolean = imeOptions and EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS > 0
    val flagNoAccessoryAction: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION > 0
    val flagNoEnterAction: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0
    val flagNoExtractUi: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_EXTRACT_UI > 0
    val flagNoFullscreen: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_FULLSCREEN > 0
    @RequiresApi(Build.VERSION_CODES.O)
    val flagNoPersonalizedLearning: Boolean = imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING > 0

    companion object {
        fun default(): ImeOptions {
            return fromImeOptionsInt(EditorInfo.IME_NULL)
        }

        fun fromImeOptionsInt(imeOptions: Int): ImeOptions {
            return ImeOptions(imeOptions)
        }
    }

    enum class Action {
        DONE,
        GO,
        NEXT,
        NONE,
        PREVIOUS,
        SEARCH,
        SEND,
        UNSPECIFIED;

        companion object {
            fun fromInt(raw: Int): Action {
                return when (raw and EditorInfo.IME_MASK_ACTION) {
                    EditorInfo.IME_ACTION_DONE -> DONE
                    EditorInfo.IME_ACTION_GO -> GO
                    EditorInfo.IME_ACTION_NEXT -> NEXT
                    EditorInfo.IME_ACTION_NONE -> NONE
                    EditorInfo.IME_ACTION_PREVIOUS -> PREVIOUS
                    EditorInfo.IME_ACTION_SEARCH -> SEARCH
                    EditorInfo.IME_ACTION_SEND -> SEND
                    EditorInfo.IME_ACTION_UNSPECIFIED -> UNSPECIFIED
                    else -> NONE
                }
            }
        }

        fun toInt(): Int {
            return when (this) {
                DONE -> EditorInfo.IME_ACTION_DONE
                GO -> EditorInfo.IME_ACTION_GO
                NEXT -> EditorInfo.IME_ACTION_NEXT
                NONE -> EditorInfo.IME_ACTION_NONE
                PREVIOUS -> EditorInfo.IME_ACTION_PREVIOUS
                SEARCH -> EditorInfo.IME_ACTION_SEARCH
                SEND -> EditorInfo.IME_ACTION_SEND
                UNSPECIFIED -> EditorInfo.IME_ACTION_UNSPECIFIED
            }
        }
    }
}

/**
 * Class which holds the same information as an [EditorInfo.inputType] int but more accessible and
 * readable.
 */
class InputAttributes private constructor(inputType: Int) {
    val type: Type
    val variation: Variation
    val capsMode: CapsMode
    var flagNumberDecimal: Boolean = false
        private set
    var flagNumberSigned: Boolean = false
        private set
    var flagTextAutoComplete: Boolean = false
        private set
    var flagTextAutoCorrect: Boolean = false
        private set
    var flagTextImeMultiLine: Boolean = false
        private set
    var flagTextMultiLine: Boolean = false
        private set
    var flagTextNoSuggestions: Boolean = false
        private set

    init {
        when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_DATETIME -> {
                type = Type.DATETIME
                variation = when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_DATETIME_VARIATION_DATE -> Variation.DATE
                    InputType.TYPE_DATETIME_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_DATETIME_VARIATION_TIME -> Variation.TIME
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.NONE
            }
            InputType.TYPE_CLASS_NUMBER -> {
                type = Type.NUMBER
                variation = when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_NUMBER_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD -> Variation.PASSWORD
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.NONE
                flagNumberDecimal = inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL > 0
                flagNumberSigned = inputType and InputType.TYPE_NUMBER_FLAG_SIGNED > 0
            }
            InputType.TYPE_CLASS_PHONE -> {
                type = Type.PHONE
                variation = Variation.NORMAL
                capsMode = CapsMode.NONE
            }
            InputType.TYPE_CLASS_TEXT -> {
                type = Type.TEXT
                variation = when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> Variation.EMAIL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> Variation.EMAIL_SUBJECT
                    InputType.TYPE_TEXT_VARIATION_FILTER -> Variation.FILTER
                    InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE -> Variation.LONG_MESSAGE
                    InputType.TYPE_TEXT_VARIATION_NORMAL -> Variation.NORMAL
                    InputType.TYPE_TEXT_VARIATION_PASSWORD -> Variation.PASSWORD
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> Variation.PERSON_NAME
                    InputType.TYPE_TEXT_VARIATION_PHONETIC -> Variation.PHONETIC
                    InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> Variation.POSTAL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> Variation.SHORT_MESSAGE
                    InputType.TYPE_TEXT_VARIATION_URI -> Variation.URI
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> Variation.VISIBLE_PASSWORD
                    InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> Variation.WEB_EDIT_TEXT
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> Variation.WEB_EMAIL_ADDRESS
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> Variation.WEB_PASSWORD
                    else -> Variation.NORMAL
                }
                capsMode = CapsMode.fromFlags(inputType)
                flagTextAutoComplete = inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE > 0
                flagTextAutoCorrect = inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT > 0
                flagTextImeMultiLine = inputType and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE > 0
                flagTextMultiLine = inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE > 0
                flagTextNoSuggestions = inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS > 0
            }
            else -> {
                type = Type.TEXT
                variation = Variation.NORMAL
                capsMode = CapsMode.NONE
            }
        }
    }

    companion object {
        fun fromInputTypeInt(inputType: Int): InputAttributes {
            return InputAttributes(inputType)
        }
    }

    enum class Type {
        DATETIME,
        NUMBER,
        PHONE,
        TEXT;
    }

    enum class Variation {
        DATE,
        EMAIL_ADDRESS,
        EMAIL_SUBJECT,
        FILTER,
        LONG_MESSAGE,
        NORMAL,
        PASSWORD,
        PERSON_NAME,
        PHONETIC,
        POSTAL_ADDRESS,
        SHORT_MESSAGE,
        TIME,
        URI,
        VISIBLE_PASSWORD,
        WEB_EDIT_TEXT,
        WEB_EMAIL_ADDRESS,
        WEB_PASSWORD;
    }

    enum class CapsMode {
        ALL,
        NONE,
        SENTENCES,
        WORDS;

        companion object {
            fun fromFlags(flags: Int): CapsMode {
                return when {
                    flags and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS > 0 -> ALL
                    flags and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES > 0 -> SENTENCES
                    flags and InputType.TYPE_TEXT_FLAG_CAP_WORDS > 0 -> WORDS
                    else -> NONE
                }
            }
        }

        fun toFlags(): Int {
            return when (this) {
                ALL -> InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                SENTENCES -> InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                WORDS -> InputType.TYPE_TEXT_FLAG_CAP_WORDS
                else -> 0
            }
        }
    }
}

/**
 * Class which marks a region of the [text] in [editorInstance].
 */
open class Region(private val editorInstance: EditorInstance) {
    var start: Int = -1
    var end: Int = -1
    val isValid: Boolean
        get() = start >= 0 && end >= 0 && length >= 0
    val length: Int
        get() = end - start
    val text: String
        get() {
            val eiText = editorInstance.cachedText
            return if (!isValid || start >= eiText.length) {
                ""
            } else {
                val end = if (end >= eiText.length) { eiText.length } else { end }
                editorInstance.cachedText.substring(start, end)
            }
        }

    override operator fun equals(other: Any?): Boolean {
        return if (other is Region) {
            start == other.start && end == other.end
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        return result
    }
}

/**
 * Class which holds selection attributes and returns the correct text for set selection based on
 * the text in [editorInstance].
 */
class Selection(private val editorInstance: EditorInstance) : Region(editorInstance) {
    val isCursorMode: Boolean
        get() = length == 0 && isValid
    val isSelectionMode: Boolean
        get() = length != 0 && isValid
}
