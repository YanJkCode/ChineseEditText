package com.android.chineseedittext;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChineseEditText extends AppCompatEditText {
    /**
     * 偏旁部首
     */
    public static final String CHINESE_RADICAL_DIGISTS =
            "[犭凵巛冖氵廴纟讠礻亻钅宀亠忄辶弋饣刂阝冫卩疒艹疋豸冂匸扌丬屮衤勹彳彡]";

    private LimitInputTextWatcher mLimitInputTextWatcher;

    public ChineseEditText(Context context) {
        super(context);
        mLimitInputTextWatcher = new LimitInputTextWatcher(this);
        addTextChangedListener(mLimitInputTextWatcher);
    }

    public ChineseEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLimitInputTextWatcher = new LimitInputTextWatcher(this);
        addTextChangedListener(mLimitInputTextWatcher);
    }

    public ChineseEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLimitInputTextWatcher = new LimitInputTextWatcher(this);
        addTextChangedListener(mLimitInputTextWatcher);
    }

    /**
     * 输入法
     *
     * @param outAttrs
     * @return
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new InnerInputConnecttion(super.onCreateInputConnection(outAttrs),
                false);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (mOnKeyboardBackListener != null) {
            return mOnKeyboardBackListener.onItemClick(event);
        }
        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        if (mLimitInputTextWatcher != null) {
            mLimitInputTextWatcher.setNewWatcher(watcher);
        }
        super.addTextChangedListener(mLimitInputTextWatcher);
    }
    class InnerInputConnecttion extends InputConnectionWrapper implements InputConnection {

        public InnerInputConnecttion(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        /**
         * 对输入的内容进行拦截
         *
         * @param text
         * @param newCursorPosition
         * @return
         */
        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            // 只能输入汉字
            if (!TextUtils.isEmpty(text) && (!isContainChinese(text.toString()) ||
                    isContainRadical(text.toString()) || isHasDigit(text.toString()))) {
                Toast.makeText(getContext(), "只能输入中文", Toast.LENGTH_SHORT).show();
                return false;
            }
            return super.commitText(text, newCursorPosition);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            // 拦截换行键
            return event.getKeyCode() != KeyEvent.KEYCODE_ENTER && super.sendKeyEvent(event);
        }

        @Override
        public boolean setSelection(int start, int end) {
            return super.setSelection(start, end);
        }
    }

    /**
     * 字符串是否包含中文
     */
    public static boolean isContainChinese(String str) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    /**
     * 字符串是否包含偏旁部首
     */
    public static boolean isContainRadical(String str) {
        Pattern p = Pattern.compile(CHINESE_RADICAL_DIGISTS);
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    /**
     * 判断字符串是否含有数字
     */
    public boolean isHasDigit(String str) {
        Pattern p = Pattern.compile(".*\\d+.*");
        Matcher m = p.matcher(str);
        if (m.matches()) {
            return true;
        }
        return false;
    }

    private OnKeyboardBackListener mOnKeyboardBackListener;

    public void setOnKeyboardBackListener(OnKeyboardBackListener onKeyboardBackListener) {
        mOnKeyboardBackListener = onKeyboardBackListener;
    }

    public interface OnKeyboardBackListener {
        boolean onItemClick(KeyEvent event);
    }

    class LimitInputTextWatcher implements TextWatcher {
        /**
         * et
         */
        private EditText et = null;
        /**
         * 筛选条件
         */
        private String regex;
        /**
         * 默认的筛选条件(正则:只能输入简体中文)
         */
        private String DEFAULT_REGEX = "[^\u4E00-\u9FA5]";
        /**
         * 二次回调
         */
        private TextWatcher newWatcher;

        /**
         * 构造方法
         *
         * @param et
         */
        public LimitInputTextWatcher(EditText et) {
            this.et = et;
            this.regex = DEFAULT_REGEX;
        }

        public void setNewWatcher(TextWatcher newWatcher) {
            if (newWatcher != this) {
                this.newWatcher = newWatcher;
            }
        }

        /**
         * 构造方法
         *
         * @param et
         */
        public LimitInputTextWatcher(EditText et, TextWatcher watcher) {
            this.et = et;
            this.regex = DEFAULT_REGEX;
            newWatcher = watcher;
        }

        /**
         * 构造方法
         *
         * @param et    et
         * @param regex 筛选条件
         */
        public LimitInputTextWatcher(EditText et, String regex) {
            this.et = et;
            this.regex = regex;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (newWatcher != null)
                newWatcher.beforeTextChanged(charSequence, i, i1, i2);
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (newWatcher != null)
                newWatcher.onTextChanged(charSequence, i, i1, i2);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            String str = editable.toString();
            String inputStr = clearLimitStr(regex, str);
            et.removeTextChangedListener(this);
            // et.setText方法可能会引起键盘变化,所以用editable.replace来显示内容
            editable.replace(0, editable.length(), inputStr.trim());
            et.addTextChangedListener(this);
            if (newWatcher != null)
                newWatcher.afterTextChanged(editable);
        }

        /**
         * 清除不符合条件的内容
         *
         * @param regex
         * @return
         */
        private String clearLimitStr(String regex, String str) {
            int length = str.length();
            String replaceAll = str.replaceAll(regex, "");
            if (length != replaceAll.length()) {
                Toast.makeText(et.getContext(), "只能输入中文", Toast.LENGTH_SHORT).show();
            }
            return replaceAll;
        }
    }
}