package compiler;

import java.io.File;
import java.util.Scanner;

import compiler.analyzer.*;
import compiler.tokenizer.*;

public class App
{
    public static void main(String[] args) throws Exception
    {
        Scanner sc = new Scanner(new File(args[0]));
        StringIter it = new StringIter(sc);
        Tokenizer tokenizer = new Tokenizer(it);

     //   try
       // {
            Analyzer analyser = new Analyzer(tokenizer);
            analyser.analyseProgram(args[1]);

        //}

       // throw new Exception();
    }
}
