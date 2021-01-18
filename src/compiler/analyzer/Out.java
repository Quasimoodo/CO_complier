package compiler.analyzer;


//import instruction.Function_Instruction;
import compiler.instruction.*;


import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class Out {
    public static void Out(String name, ArrayList<String> globalV, ArrayList<Function_Instruction> fnList) throws Exception{
        FileOutputStream file = new FileOutputStream(new File(name));
        file.write(intToByte(0x72303b3e));
        file.write(intToByte(0x1));

        file.write(intToByte(globalV.size()));

        for(int i = 0; i < globalV.size(); i ++){ //全局
            if(globalV.get(i).equals("1")){
                file.write(0);
                file.write(intToByte(8));
                file.write(longToByte(0L));
            }else if(globalV.get(i).equals("0")){
                file.write(1);
                file.write(intToByte(8));
                file.write(longToByte(0L));
            }
            else{ //函数名、字符串
                file.write(1);
                file.write(intToByte(globalV.get(i).length()));
                file.write(globalV.get(i).getBytes());
            }
        }

        file.write(intToByte(fnList.size()));// functions.count

        for(int i = 0; i < fnList.size(); i ++){ //function
            file.write(intToByte(fnList.get(i).getName()));
            file.write(intToByte(fnList.get(i).getRet_slots()));
            file.write(intToByte(fnList.get(i).getParam_slots()));
            file.write(intToByte(fnList.get(i).getLoc_slots()));
            file.write(intToByte(fnList.get(i).getBodyCount()));

            ArrayList<Instruction> fninstructions = fnList.get(i).getBodyItem();

            for(int j = 0; j < fninstructions.size(); j ++){
                file.write(fninstructions.get(j).getOpt().getI());
                if(fninstructions.get(j).getX() != null){ //有操作数
                    if(fninstructions.get(j).getOpt() == Operation.push){ //是push
                        file.write(longToByte((long)fninstructions.get(j).getX()));
                    }
                    else{
                        file.write(intToByte((int)fninstructions.get(j).getX()));
                    }
                }
            }
        }
    }

    public static byte[] longToByte(long val) {
        byte[] b = new byte[8];
        b[7] = (byte) (val & 0xff);
        b[6] = (byte) ((val >> 8) & 0xff);
        b[5] = (byte) ((val >> 16) & 0xff);
        b[4] = (byte) ((val >> 24) & 0xff);
        b[3] = (byte) ((val >> 32) & 0xff);
        b[2] = (byte) ((val >> 40) & 0xff);
        b[1] = (byte) ((val >> 48) & 0xff);
        b[0] = (byte) ((val >> 56) & 0xff);
        return b;
    }

    public static byte[] intToByte(int val) {
        byte[] b = new byte[4];
        b[3] = (byte) (val & 0xff);
        b[2] = (byte) ((val >> 8) & 0xff);
        b[1] = (byte) ((val >> 16) & 0xff);
        b[0] = (byte) ((val >> 24) & 0xff);
        return b;
    }
}
