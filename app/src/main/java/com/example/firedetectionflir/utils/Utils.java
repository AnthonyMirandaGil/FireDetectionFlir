package com.example.firedetectionflir.utils;

public class Utils {
    public static double maxArray(double [] array, Integer precision){
        double maxVal = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < array.length; i++) {
            double val = array[i];
            if(val > maxVal){
                maxVal = val;
                if ( precision != null && precision >= 0) {
                    double factor10 = Math.pow(10.0, precision);
                    maxVal = Math.round(maxVal * Math.pow(10.0, factor10)) / factor10;
                }
            }
        }
        return maxVal;
    }
}
