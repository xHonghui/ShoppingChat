package com.netease.nim.uikit.business.session.weight;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import com.laka.androidlib.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author:summer
 * @Date:2019/9/19
 * @Description:金额输入框过滤器
 */
public class CashierInputFilter implements InputFilter {

    Pattern mPattern;
    Pattern mPattern2;
    //输入的最大金额
    private static final int MAX_VALUE = Integer.MAX_VALUE;
    //小数点后的位数
    private static final int POINTER_LENGTH = 2;

    private static final String POINTER = ".";

    private static final String ZERO = "0";

    private double mMaxCash = MAX_VALUE;

    public void setMaxCash(double maxCash) {
        this.mMaxCash = maxCash;
    }

    public CashierInputFilter() {
        mPattern = Pattern.compile("([0-9]|\\.)*");
        mPattern2 = Pattern.compile("(\\.)*");
    }

    /**
     * @param source 新输入的字符串
     * @param start  新输入的字符串起始下标，一般为0
     * @param end    新输入的字符串终点下标，一般为source长度-1
     * @param dest   输入之前文本框内容
     * @param dstart 原内容起始坐标，一般为0
     * @param dend   原内容终点坐标，一般为dest长度-1
     * @return 输入内容
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String sourceText = source.toString();
        String destText = dest.toString();
        try {
            //验证删除等按键
            if (TextUtils.isEmpty(sourceText)) {
                if (dstart == 0 && destText.indexOf(POINTER) == 1) {
                    return "0";
                }
                return "";
            }
            Matcher matcher = mPattern.matcher(source);
            Matcher matcher2 = mPattern2.matcher(source);
            //已经输入小数点的情况下，只能输入数字
            if (destText.contains(POINTER)) {
                if (!matcher.matches()) {
                    return "";
                } else {
                    if (POINTER.equals(source)) {  //只能输入一个小数点
                        return "";
                    }
                }

                //验证小数点精度，保证小数点后只能输入两位
                int index = destText.indexOf(POINTER);
                int length = destText.trim().length() - index;


                if (length > POINTER_LENGTH && dstart > index) {
                    return "";
                }
            } else {
                //没有输入小数点的情况下，只能输入小数点和数字，但首位不能输入小数点和0
                if (!matcher.matches()) {
                    return "";
                } else {
                    if ((matcher2.matches()) && dstart == 0) {
                        return "0.";
                    }
                }
            }
            double sumText = Double.parseDouble(destText + sourceText);
            if (sumText > 9999999) { //限制不能输入超过七位数
                return dest.subSequence(dstart, dend);
            }
            if (sumText > MAX_VALUE) {
                return dest.subSequence(dstart, dend);
            }
            // 判断输入的值是否大于限定的最大值，如果大于，则返回最大值
//            if (sumText >= mMaxCash) {
//                return dest.subSequence(dstart, dend);
//                //return roundForHalf(mMaxCash + "");
//            }

            return dest.subSequence(dstart, dend) + sourceText;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            try {
                return dest.subSequence(dstart, dend) + sourceText;
            } catch (Exception e1) {
                e1.printStackTrace();
                return "";
            }
        }
    }

    private String roundForHalf(String str) {
        if (StringUtils.isEmpty(str)) return "0.00";
        return roundForHalf(new BigDecimal(str), RoundingMode.HALF_UP);
    }

    private String roundForHalf(BigDecimal arg, RoundingMode mode) {
        if (arg != null) {
            return arg.setScale(2, mode).toString();
        } else {
            return "";
        }
    }

}
