package org.weasis.core.api.util;

import org.weasis.core.api.Messages;

public class StringUtil {

    public static final String COLON = Messages.getString("StringUtil.colon"); //$NON-NLS-1$
    public static final String COLON_AND_SPACE = Messages.getString("StringUtil.colon_space"); //$NON-NLS-1$

    private StringUtil() {
    }

    public static Character getFirstCharacter(String val) {
        if (StringUtil.hasText(val)) {
            return Character.valueOf(val.charAt(0));
        }
        return null;
    }

    public static String[] getStringArray(String val, String delimiter) {
        if (delimiter != null && StringUtil.hasText(val)) {
            return val.split(delimiter);
        }
        return null;
    }

    public static int[] getIntegerArray(String val, String delimiter) {
        if (delimiter != null && StringUtil.hasText(val)) {
            String[] vl = val.split(delimiter);
            int[] res = new int[vl.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = getInteger(vl[i]);
            }
            return res;
        }
        return null;
    }

    public static int getInteger(String val) {
        if (StringUtil.hasText(val)) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                System.out.print("Cannot convert " + val + " to int"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return 0;
    }

    public static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }

    public static boolean hasLength(String str) {
        return hasLength((CharSequence) str);
    }

    public static boolean hasText(CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasText(String str) {
        return hasText((CharSequence) str);
    }

}
