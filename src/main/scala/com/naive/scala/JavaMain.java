package com.naive.scala;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guoning on 15/12/21.
 */
public class JavaMain {

    public static void main(String[] args) {


        List list = new ArrayList<Integer>();
        for (int i = 0; i <= 1000000; i++) {
            list.add(i);
        }

        long start = System.currentTimeMillis();
        long count = 0l;
        for (int i = 0; i <= 1000000; i++) {
            count += i;
        }

        long end = System.currentTimeMillis();
        System.out.println("count = " + count + " use time : " + (end - start));

    }
}
