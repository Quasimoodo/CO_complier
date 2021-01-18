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
      //  Objectfile obj=new 
     //   Analyzer aer = new Analyzer(ter);
    /*    try
        {
            aer.analyse();

        }
        */
        throw new Exception();
    }
}
