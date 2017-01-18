package com.example.miki.alexasmschecker;

import android.os.StrictMode;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by mpokr on 1/16/2017.
 */

public class NameFinder {

    private String name;

    public NameFinder(String name) {
        this.name = name;
    }

    public ArrayList<String> findVariations() {
        try {
            Document doc = Jsoup.connect( "http://www.bestlittlebaby.com/alternate-name-speller.aspx?name=" + name).get();
            Elements variations = doc.select("a.emp");
            ArrayList<String> varList = new ArrayList<>();
            for (int i = 0; i < variations.size(); i++) {
                varList.add(variations.get(i).textNodes().get(0).toString());
            }
            return varList;
        }
        catch (IOException e) {
            return null;
        }
    }

    public static void main(String[] args)  {
        NameFinder nameFinder = new NameFinder("Debbi");
        nameFinder.findVariations();
    }
}
