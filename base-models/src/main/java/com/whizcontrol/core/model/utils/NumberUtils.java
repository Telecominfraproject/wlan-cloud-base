/**
 * 
 */
package com.whizcontrol.core.model.utils;

/**
 * @author yongli
 *
 */
public class NumberUtils {
    private NumberUtils() {
    }

    public static boolean isDoubleZero(double val) {
        return (0 == Double.compare(val, 0.0d)) || (0 == Double.compare(val, -0.0d));
    }

    public static boolean isFloatZero(float val) {
        return (0 == Float.compare(val, 0.0f) || (0 == Float.compare(val, -0.0f)));
    }

    public static int getValueWithinRange(int a, int b, int c) {
        if (b >= c) {
            return c;
        } else if (b <= a) {
            return a;
        }
        return b;
    }
    
    public static Number max(Number left, Number right)
    {
        if(left != null && right != null)
        {
            return Math.max(left.doubleValue(), right.doubleValue());
        }
        else if(left != null)
        {
            return left;
        }
        else
        {
            return right;
        }
    }

    public static Number min(Number left, Number right) {
        if (left != null && right != null) {
            return Math.min(left.doubleValue(), right.doubleValue());
        } else if (left != null) {
            return left;
        } else {
            return right;
        }
    }

    public static Number sum(Number left, Number right) {
        if (left != null && right != null) {
            return left.doubleValue() + right.doubleValue();
        } else if (left != null) {
            return left;
        } else {
            return right;
        }

    }
}
