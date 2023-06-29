package com.codecad.core;

import com.codecad.core.exception.InterpreterException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public class Utils {
    public static Iterator<?> getIterator(Object obj){
        if(obj instanceof Object[]){
            return Arrays.stream((Object[])obj).iterator();
        }else if(obj instanceof Iterable<?>){
            return ((Iterable<?>) obj).iterator();
        }else if(obj instanceof Stream<?>){
            return ((Stream<?>)obj).iterator();
        }

        throw new InterpreterException("Value not iterable");
    }

    public Object flatten(Object[] arr){
        if(arr.length == 1){
            return arr[0];
        }else{
            return arr;
        }
    }
}
