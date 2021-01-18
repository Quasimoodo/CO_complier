package compiler.analyzer;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import compiler.instruction.*;
import compiler.tokenizer.*;
import compiler.analyzer.*;
import compiler.util.Pos;



public class Analyzer {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;
    //    Tokentype NeedtoPush = null;
    int globalOffset = 0;
    int argsOffset = 0;
    int localOffset = 0;
    int fnOffset = 1;
    ArrayList<String> GlobalVariable=new ArrayList<>();
    ArrayList<Function_Instruction> fnLists = new ArrayList<>();
    ArrayList<Instruction> CurrentFunction_Instruction;
    boolean hasMain = false;
    int fnPos = 0;
    boolean maintype = false;


    ArrayList<Tokentype> Symbol = new ArrayList<Tokentype>(Arrays.asList(Tokentype.AS_KW, Tokentype.MUL, Tokentype.DIV, Tokentype.PLUS, Tokentype.MINUS, Tokentype.GT, Tokentype.LT, Tokentype.LE, Tokentype.GE, Tokentype.EQ, Tokentype.NEQ));

    public int[][] SymbolMatrix = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1}
    };


    /**
     * 当前偷看的 token
     */
    Token peekedToken = null;

    /**
     * 符号表
     */
    Stack<Symbol> symbolTable = new Stack<Symbol>();
    Stack<Integer> symbolInt = new Stack<>();
    HashMap<String, Integer> symbolHash = new HashMap<>();

    /**
     * 下一个变量的栈偏移
     */
    int nextOffset = 0;

    public Analyzer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        //this.instructions = new ArrayList<>();
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     *
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws Exception
     */
    private Token nextIf(Tokentype tt) throws Exception {
        Token token = peek();
        if (token.getTokentype() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 查看下一个 Token
     *
     * @return
     * @throws Exception
     */
    private Token peek() throws Exception {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     *
     * @param tt
     * @return
     * @throws Exception
     */
    private boolean check(Tokentype tt) throws Exception {
        Token token = peek();
        return token.getTokentype() == tt;
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws Exception
     */
    private Token next() throws Exception {
        if (peekedToken != null) {
            Token token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     *
     * @param tt 类型
     * @return 这个 token
     * @throws Exception 如果类型不匹配
     */
    private Token expect(Tokentype tt) throws Exception {
        Token token = peek();
        if (token.getTokentype() == tt) {
            return next();
        } else {
            throw new Exception();
        }
    }

    /**
     * 添加一个符号
     *
     * @param name       名字
     * @param isConstant 是否是常量
     * @param curPos     当前 token 的位置（报错用）
     * @throws Exception 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isConstant, Tokentype type, SymbolType symbolType, Pos curPos) throws Exception, Exception {

        if (this.symbolHash.get(name) != null && this.symbolHash.get(name) >= symbolInt.peek()) { //如果现在读到的已经在当前块
            throw new Exception();
        } else {
            if (this.symbolHash.get(name) != null) { //如果读到的在之前的块里出现过
                int chain = this.symbolHash.get(name);
                switch (symbolType) {
                    case global:
                        this.symbolTable.push(new Symbol(name, chain, type, isConstant, symbolType, globalOffset++));
                        if(isConstant){
                            GlobalVariable.add("0");
                        }else{
                            GlobalVariable.add("1");
                        }
                        break;
                    case args:
                        this.symbolTable.push(new Symbol(name, chain, type, isConstant, symbolType, argsOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new Symbol(name, chain, type, isConstant, symbolType, localOffset++));
                        break;
                }
                this.symbolHash.put(name, symbolTable.size() - 1);
            } else { //没出现过，先入符号栈，再加入hashmap
                switch (symbolType) {
                    case global:
                        this.symbolTable.push(new Symbol(name, -1, type, isConstant, symbolType, globalOffset++));
                        if(isConstant){
                            GlobalVariable.add("0");
                        }else{
                            GlobalVariable.add("1");
                        }
                        break;
                    case args:
                        this.symbolTable.push(new Symbol(name, -1, type, isConstant, symbolType, argsOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new Symbol(name, -1, type, isConstant, symbolType, localOffset++));
                        break;
                }
                this.symbolHash.put(name, symbolTable.size() - 1);
            }
        }
    }

    private Symbol addFnSymbol(String name, Pos curPos) throws Exception {
        if (this.symbolHash.get(name) != null) {
            throw new Exception();
        } else {
            this.symbolTable.push(new Symbol(name, true, globalOffset, fnOffset++));
            this.symbolHash.put(name, symbolTable.size() - 1);
            this.symbolInt.push(symbolTable.size());
            return this.symbolTable.peek();
        }

    }

    /**
     * 获取下一个变量的栈偏移
     *
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    public void analyseProgram(String name) throws Exception {
        // 程序 -> 主过程
        // 示例函数，示例如何调用子程序

        analyseMain();

        expect(Tokentype.EOF);
        System.out.println();
        for (String s : GlobalVariable) {
            System.out.println(s);
        }
        for (Function_Instruction fnList : fnLists) {
            System.out.println(fnList.toString());
        }

        Out.Out(name, GlobalVariable, fnLists); //转二进制
    }


    private void analyseMain() throws Exception {
        // 主过程 -> (变量声明|函数声明)*

        Function_Instruction startFn = new Function_Instruction();
        GlobalVariable.add("_start");
        globalOffset++;
        fnLists.add(startFn);
        while (true) { //这里一起判断了三种：decl_stmt -> let_decl_stmt | const_decl_stmt； function
            if (check(Tokentype.CONST_KW) || check(Tokentype.LET_KW)) { //如果读到下一个token类型是const或者let，那么不能前进一个token，说明此时进入decl_stmt
                // 变量声明 -> 变量声明 | 常量声明
                if (check(Tokentype.CONST_KW)) {
                    CurrentFunction_Instruction = startFn.getBodyItem();
                    analyseConstDeclaration(true); //进入常量声明分析过程 const
                } else if (check(Tokentype.LET_KW)) {
                    CurrentFunction_Instruction = startFn.getBodyItem();
                    analyseVariableDeclaration(true); //进入变量声明分析过程 let
                }
            } else if (check(Tokentype.FN_KW)) { //如果下一个token是fn，则前进一个token，并返回这个token（fn），此时应进入function分析过程
                System.out.println("进入fn了噢");
                analyseFunctionDeclaration(); //进入function分析过程
            } else {
                System.out.println("主过程错啦，既不是变量也不是常量！");
                break;
//                throw new Exception(ErrorCode.InvalidAssignment, );
            }
        }

        startFn.setName(0);
        startFn.setRet_slots(0);
        startFn.setParam_slots(0);
        startFn.setLoc_slots(0);
        if(hasMain){
            if(!maintype){
                startFn.getBodyItem().add(new Instruction(Operation.stackalloc, 0));
            }else{
                startFn.getBodyItem().add(new Instruction(Operation.stackalloc, 1));
            }
            startFn.getBodyItem().add(new Instruction(Operation.call, fnPos));
            if(maintype){
                startFn.getBodyItem().add(new Instruction(Operation.popn,1));
            }
        }
        startFn.setBodyCount(startFn.getBodyItem().size());

    }

    /**
     * const常量分析过程
     *
     * @throws Exception
     */
    private void analyseConstDeclaration(boolean isGlobal) throws Exception {
        //const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
        expect(Tokentype.CONST_KW);

        Token nameToken = expect(Tokentype.IDENT);

        String name = (String) nameToken.getValue();

        if(!isGlobal){
            CurrentFunction_Instruction.add(new Instruction(Operation.loca, localOffset));
        }else{
            CurrentFunction_Instruction.add(new Instruction(Operation.globa, globalOffset));
        }

        // 冒号
        expect(Tokentype.COLON);

        // ty
        Token tyToken = expect(Tokentype.IDENT);
        if(tyToken.getValue().equals("int")){
            tyToken.setTokentype(Tokentype.INT);
        }
        else if(tyToken.getValue().equals("double")){
            tyToken.setTokentype(Tokentype.DOUBLE);
        }
        else{
            throw new Exception();
        }


        // =
        expect(Tokentype.ASSIGN);

        // expr
        Tokentype t = analyseExpr(true);

        if(tyToken.getTokentype() != t){ //ty '=' expr 类型是否相同
            throw new Exception();
        }

        CurrentFunction_Instruction.add(new Instruction(Operation.store64));

        // ;
        expect(Tokentype.SEMICOLON);

        // 加入符号表
        if (isGlobal) {
            addSymbol(name, true, tyToken.getTokentype(), SymbolType.global, nameToken.getStartPos());
        } else {
            addSymbol(name, true, tyToken.getTokentype(), SymbolType.local, nameToken.getStartPos());
        }

        //TODO
        //入栈
//        instructions.add(new Instruction());
    }

    /**
     * variable变量分析过程
     *
     * @throws Exception
     */
    private void analyseVariableDeclaration(boolean isGlobal) throws Exception {
        //let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'

        expect(Tokentype.LET_KW);

        Token nameToken = expect(Tokentype.IDENT);


        //冒号
        expect(Tokentype.COLON);

        // ty
        Token tyToken = expect(Tokentype.IDENT);
        System.out.println(tyToken.getValue());
        if(tyToken.getValue().equals("int")){
            tyToken.setTokentype(Tokentype.INT);
        }
        else if(tyToken.getValue().equals("double")){
            tyToken.setTokentype(Tokentype.DOUBLE);
        }
        else{
            throw new Exception();
        }

        if (nextIf(Tokentype.ASSIGN) != null) {
            if(isGlobal){
                CurrentFunction_Instruction.add(new Instruction(Operation.globa, globalOffset));
            }else{
                CurrentFunction_Instruction.add(new Instruction(Operation.loca, localOffset));
            }
            Tokentype t = analyseExpr(true);
            if(tyToken.getTokentype() != t){ //ty ('=' expr)?
                throw new Exception();
            }
            CurrentFunction_Instruction.add(new Instruction(Operation.store64));
        }


        // ;
        expect(Tokentype.SEMICOLON);


        //TODO
        //加入符号表
        if (isGlobal) {
            addSymbol(nameToken.getValue().toString(), false, tyToken.getTokentype(), SymbolType.global, nameToken.getStartPos());
        } else {
            addSymbol(nameToken.getValue().toString(), false, tyToken.getTokentype(), SymbolType.local, nameToken.getStartPos());
        }


        // TODO
        //入栈
//        instructions.add(new Instruction());
    }

    /**
     * function的分析过程
     */
    private void analyseFunctionDeclaration() throws Exception {
        //function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt

        Function_Instruction fnInstruction = new Function_Instruction();
        fnLists.add(fnInstruction);
        CurrentFunction_Instruction = fnInstruction.getBodyItem();

        boolean hasReturn = false;

        expect(Tokentype.FN_KW);

        Token nameToken = expect(Tokentype.IDENT);
        GlobalVariable.add(nameToken.getValue().toString()); //存入全局变量表
        fnInstruction.setName(globalOffset++); //取现在的globalOffset再加一

        System.out.println("fn名字： " + nameToken);

        if(nameToken.getValue().toString().equals("main")){
            hasMain = true;
            fnPos = fnLists.size()-1;
        }
        Symbol currentSymbol = addFnSymbol(nameToken.getValue().toString(), nameToken.getStartPos()); //加入符号表


        // (
        expect(Tokentype.L_PAREN);


        //参数offset清零
        argsOffset = 0;

        //function_param_list
        if (check(Tokentype.CONST_KW) || check(Tokentype.IDENT)) {
            analyseFunctionParamList();
        }




        expect(Tokentype.R_PAREN);

        expect(Tokentype.ARROW);

        // ty
        Token tyToken = expect(Tokentype.IDENT);
        if(tyToken.getValue().equals("int")){
            tyToken.setTokentype(Tokentype.INT);
            fnInstruction.setRet_slots(1); //return数量置1
            for(int i = symbolTable.size()-1; symbolTable.get(i).getSymbolType() == SymbolType.args; i--){
                symbolTable.get(i).setOffset(symbolTable.get(i).getOffset()+1);
            }
            if(nameToken.getValue().toString().equals("main")){
                maintype = true;
            }
        }
        else if(tyToken.getValue().equals("double")){
            tyToken.setTokentype(Tokentype.DOUBLE);
            fnInstruction.setRet_slots(1);
            for(int i = symbolTable.size()-1; symbolTable.get(i).getSymbolType() == SymbolType.args; i--){
                symbolTable.get(i).setOffset(symbolTable.get(i).getOffset()+1);
            }
            if(nameToken.getValue().toString().equals("main")){
                maintype = true;
            }
        }
        else if(tyToken.getValue().equals("void")){
            tyToken.setTokentype(Tokentype.VOID);
            fnInstruction.setRet_slots(0); //return数量置0
            if(nameToken.getValue() == "main"){
                maintype = false;
            }
        }
        else{
            throw new Exception();
        }


        fnInstruction.setParam_slots(argsOffset); //设置参数数量

        currentSymbol.setType(tyToken.getTokentype()); //fn的type属性

        // block_stmt
        localOffset = 0;
        hasReturn = analyseBlockStmt(true, tyToken.getTokentype(), false, null, -1);
        fnInstruction.setLoc_slots(localOffset);

        if(tyToken.getTokentype()!=Tokentype.VOID && !hasReturn){ //如果是fn 需要有return
            throw new Exception();
        }else if(tyToken.getTokentype()==Tokentype.VOID && !hasReturn){
            CurrentFunction_Instruction.add(new Instruction(Operation.ret));
        }

        fnInstruction.setBodyCount(fnInstruction.getBodyItem().size());
    }

    /**
     * expr表达式分析过程
     */
    private Tokentype analyseExpr(boolean f) throws Exception {
        //expr->(negate_expr| assign_expr | call_expr | literal_expr | ident_expr | group_expr) {binary_operator expr|'as' ty}

        System.out.println("开始分析expr");
        Tokentype type = null;

        //negate_expr
        if (check(Tokentype.MINUS)) {
            System.out.println("这是negate_expr");
            type = analyseNegateExpr();
            if(type == Tokentype.INT){
                CurrentFunction_Instruction.add(new Instruction(Operation.negi));
            }
            else if(type == Tokentype.DOUBLE){
                CurrentFunction_Instruction.add(new Instruction(Operation.negf));
            }else{
                throw new Exception();
            }
            System.out.println("negate_expr结束啦");
        }

        //assign | call | ident分析
        if (peek().getTokentype() == Tokentype.IDENT) {
            Token nameToken = next();
            //TODO 只有ident

            Integer index = symbolHash.get(nameToken.getValue().toString());

            if (nextIf(Tokentype.ASSIGN) != null) {  //assign

                if (index == null) { //符号表没有这个符号
                    throw new Exception();
                }

                if(symbolTable.get(index).isConst()){
                    throw new Exception();
                }

                if(symbolTable.get(index).getSymbolType() == SymbolType.local){ //是局部变量
                    CurrentFunction_Instruction.add(new Instruction(Operation.loca, symbolTable.get(index).getOffset()));
                }else if(symbolTable.get(index).getSymbolType() == SymbolType.global){
                    CurrentFunction_Instruction.add(new Instruction(Operation.globa, symbolTable.get(index).getOffset()));
                }else{
                    CurrentFunction_Instruction.add(new Instruction(Operation.arga, symbolTable.get(index).getOffset()));
                }

                Tokentype l_type = symbolTable.get(index).getType(); //取l_expr的类型
                System.out.println("这是assign_expr");
                Tokentype r_type = analyseExpr(true); //r_expr的类型

                if (l_type != r_type) { //如果不相等 语义报错
                    throw new Exception();
                }

                CurrentFunction_Instruction.add(new Instruction(Operation.store64));
                type = Tokentype.VOID; //赋值表达式的值类型永远是 void
                System.out.println("assign_expr结束啦");
            } else if (nextIf(Tokentype.L_PAREN) != null) { //call
                System.out.println("这是call_expr");

                int currentGlobal = 0;
                ArrayList<Tokentype> call_array = null;
                Tokentype return_type;

                if (index == null) {
                    switch (nameToken.getValue().toString()) {
                        case "getint":
                        case "getchar":
                            call_array = new ArrayList<Tokentype>();
                            return_type = Tokentype.INT;
                            break;
                        case "getdouble":
                            call_array = new ArrayList<Tokentype>();
                            return_type = Tokentype.DOUBLE;
                            break;
                        case "putint":
                            call_array = new ArrayList<Tokentype>() {{
                                add(Tokentype.INT);
                            }};
                            return_type = Tokentype.VOID;
                            break;
                        case "putdouble":
                            call_array = new ArrayList<Tokentype>() {{
                                add(Tokentype.DOUBLE);
                            }};
                            return_type = Tokentype.VOID;
                            break;
                        case "putchar":
                            call_array = new ArrayList<Tokentype>() {{
                                add(Tokentype.INT);
                            }};
                            return_type = Tokentype.VOID;
                            break;
                        case "putstr":
                            call_array = new ArrayList<Tokentype>() {{
                                add(Tokentype.INT);
                            }};
                            return_type = Tokentype.VOID;
                            break;
                        case "putln":
                            call_array = new ArrayList<Tokentype>();
                            return_type = Tokentype.VOID;
                            break;
                        default:
                            throw new Exception();
                    }
                    GlobalVariable.add(nameToken.getValue().toString()); //把标准库函数存入全局变量
                    currentGlobal = globalOffset ++;
                } else { //取到参数列表和返回类型
                    Symbol call_index = symbolTable.get(index);
                    call_array = call_index.getParams();
                    return_type = call_index.getType();
                    System.out.println("此时调用的函数： "+ call_index.getName());
                    System.out.println("返回类型： "+call_index.getType());
                }

                if(return_type == Tokentype.INT || return_type == Tokentype.DOUBLE){ //stackalloc 按返回类型判断
                    CurrentFunction_Instruction.add(new Instruction(Operation.stackalloc, 1));
                }else if(return_type == Tokentype.VOID){
                    CurrentFunction_Instruction.add(new Instruction(Operation.stackalloc, 0));
                }



                if (nextIf(Tokentype.R_PAREN) != null) { //无参数调用
                    if (call_array.size() != 0) {
                        throw new Exception();
                    } else {
                        System.out.println("call_expr结束啦");
                        type = return_type;
                    }
                } else { //有参数调用
                    Tokentype param0 = analyseExpr(true); //
                    int i = 0;
                    if (param0 != call_array.get(i)) {
                        System.out.println("param0:"+param0);
                        System.out.println("call_array get0:" + call_array.get(0));
                        throw new Exception();
                    }
                    while (nextIf(Tokentype.COMMA) != null) {
                        i++;
                        if (call_array.size() < i) { //参数个数不同 报错
                            throw new Exception();
                        }
                        Tokentype param = analyseExpr(true);
                        if (param != call_array.get(i)) {
                            throw new Exception();
                        }
                    }
                    expect(Tokentype.R_PAREN);
                    System.out.println("call_expr结束啦");
                    type = return_type;
                }
                if(index != null){
                    CurrentFunction_Instruction.add(new Instruction(Operation.call, symbolTable.get(index).getFnoffset()));
                }else{
                    CurrentFunction_Instruction.add(new Instruction(Operation.callname, currentGlobal));
                }
            } else { //只有IDENT
                if(index==null&&nameToken.getValue().toString().equals("int")){
                    type=Tokentype.INT;
                }
                else if(index==null&&nameToken.getValue().toString().equals("double")){
                    type=Tokentype.DOUBLE;
                }
                else if (index == null) {
                    throw new Exception();
                }
                else{
                    Symbol symbol = symbolTable.get(index);

                    if(symbol.getSymbolType() == SymbolType.global){ //取地址
                        CurrentFunction_Instruction.add(new Instruction(Operation.globa, symbol.getOffset()));
                    }else if(symbol.getSymbolType() == SymbolType.local){
                        CurrentFunction_Instruction.add(new Instruction(Operation.loca, symbol.getOffset()));
                    }else{
                        CurrentFunction_Instruction.add(new Instruction(Operation.arga, symbol.getOffset()));
                    }

                    CurrentFunction_Instruction.add(new Instruction(Operation.load64)); //取值

                    type = symbolTable.get(index).getType();
                }
            }
        }

        //literal_expr
        else if (peek().getTokentype() == Tokentype.UINT_LITERAL || peek().getTokentype() == Tokentype.STRING_LITERAL || peek().getTokentype() == Tokentype.DOUBLE_LITERAL || peek().getTokentype() == Tokentype.CHAR_LITERAL) {
            System.out.println("这是literal_expr");

            if (peek().getTokentype() == Tokentype.UINT_LITERAL) { //是无符号整数
                System.out.println("这里有个UINT：" + peek());

                type = Tokentype.INT;

                CurrentFunction_Instruction.add(new Instruction(Operation.push, peek().getValue()));

                next();
                //TODO 注意此时还没有移动指针
            } else if (peek().getTokentype() == Tokentype.STRING_LITERAL) {//是字符串
                //字符串需要存在全局变量
                GlobalVariable.add(peek().getValue().toString());
                globalOffset++;
                type = Tokentype.INT;

                CurrentFunction_Instruction.add(new Instruction(Operation.push, (long)globalOffset-1));

                System.out.println("这里有个STRING：" + peek());
                next();
                //TODO 注意此时还没有移动指针
            } else if (peek().getTokentype() == Tokentype.DOUBLE_LITERAL) { //double
                System.out.println("这里有个DOUBLE：" + peek());
                type = Tokentype.DOUBLE;

                CurrentFunction_Instruction.add(new Instruction(Operation.push, Double.doubleToRawLongBits((double)peek().getValue())));

                next();
                //TODO 注意此时还没有移动指针
            } else if (peek().getTokentype() == Tokentype.CHAR_LITERAL) { //char

                System.out.println("这里有个CHAR：" + peek());

                type = Tokentype.INT;

                CurrentFunction_Instruction.add(new Instruction(Operation.push, (long)(char)peek().getValue()));

                next();
            }
            System.out.println("literal_expr结束啦");
        }

        //group_expr
        else if (check(Tokentype.L_PAREN)) {
            System.out.println("这是group_expr");
            type = analyseGroupExpr();
            System.out.println("group分析完之后需要重新入栈的：" + type);
            System.out.println(f);
        }

        if (f) { //OPG 判断operator_expr 和 as_expr
            Stack stack = new Stack();
            stack.push('#');
            Stack Nstack = new Stack<>();
            if (type != null) {
                Nstack.push(type);
                System.out.println("push了（外层）：" + type);
                System.out.println("此时Nstack栈：" + Nstack);
            }
            while (check(Tokentype.AS_KW) || check(Tokentype.PLUS) || check(Tokentype.MINUS) || check(Tokentype.MUL) || check(Tokentype.DIV) || check(Tokentype.EQ) || check(Tokentype.NEQ) || check(Tokentype.LT) || check(Tokentype.GT) || check(Tokentype.LE) || check(Tokentype.GE)) {
                OPGAnalyse(stack, Nstack);
                Tokentype second_type = analyseExpr(false);

                if (second_type != null) {
                    Nstack.push(second_type);
                    System.out.println("push了（内层）：" + second_type);
                    System.out.println("此时Nstack栈：" + Nstack);
                    second_type = null; //还原
                }

            }
            int sch = Symbol.indexOf(stack.peek());
            int ch = Symbol.indexOf(peek().getTokentype());
            while ((ch == -1 || SymbolMatrix[sch][ch] == 1) && stack.size() > 1) { //栈内大于当前 规约
                reduction(stack, Nstack);
            }
            type = (Tokentype) Nstack.pop();
        }
        return type;
    }

    /**
     * OPGAnalyse
     */
    private void OPGAnalyse(Stack<Tokentype> s, Stack Ns) throws Exception {
        System.out.println("OPG开始分析");
        while (true) { //栈内大于当前 规约
            int sch = Symbol.indexOf(s.peek());
            int ch = Symbol.indexOf(peek().getTokentype());


            if (sch == -1 && ch == -1) { //都为#
                System.out.println("没有符号可以规约啦 都是# 结束！");
                return;
            } else if (sch == -1 || SymbolMatrix[sch][ch] == 0) { //栈内优先级小于当前字符 入栈
                System.out.println("栈内的符号：" + s.peek() + " 栈外的符号：" + peek().getTokentype() + " 栈内优先级小于栈外，入栈！");
                s.push(Symbol.get(ch));

                next();
                System.out.println("此时栈中符号：" + s);
                return;
            } else if((ch == -1 || SymbolMatrix[sch][ch] == 1) && s.size() > 1){
                    System.out.println("站内符号：" + s.peek() + " 栈外符号：" + peek().getTokentype() + " 要规约了");
                    reduction(s, Ns);
            }
        }
    }

    /**
     * reduction 规约
     */
    private void reduction(Stack<Tokentype> s, Stack<Object> Ns) {
        System.out.println("规约了！");

        System.out.println("这时的非终结符栈：" + Ns);
        System.out.println("这时的符号栈：" + s);
        Tokentype pop = s.pop(); //符号栈弹一个

        Tokentype pop2 = (Tokentype) Ns.pop(); //非终结符栈弹两个

        Tokentype pop1 = (Tokentype) Ns.pop();

        Tokentype push = null;

        if (pop == Tokentype.AS_KW) { //as指令分析
            if (pop1 == Tokentype.DOUBLE || pop1 == Tokentype.INT) {
                if (pop2 == Tokentype.DOUBLE) {
                    push = Tokentype.DOUBLE;
                    if(pop1 == Tokentype.INT){
                        CurrentFunction_Instruction.add(new Instruction(Operation.itof));
                    }
                }
                if (pop2 == Tokentype.INT) {
                    push = Tokentype.INT;
                    if(pop1 == Tokentype.DOUBLE){
                        CurrentFunction_Instruction.add(new Instruction(Operation.ftoi));
                    }
                }
            } else {
                System.exit(-1);
            }
        } else {
            if (pop1 != pop2) {
                System.exit(-1);
            }


            switch (pop) { //
                case PLUS:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.INT;
                        CurrentFunction_Instruction.add(new Instruction(Operation.addi));
                    }else{
                        push = Tokentype.DOUBLE;
                        CurrentFunction_Instruction.add(new Instruction(Operation.addf));
                    }
                    break;
                case MINUS:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.INT;
                        CurrentFunction_Instruction.add(new Instruction(Operation.subi));
                    }else{
                        push = Tokentype.DOUBLE;
                        CurrentFunction_Instruction.add(new Instruction(Operation.subf));
                    }
                    break;
                case MUL:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.INT;
                        CurrentFunction_Instruction.add(new Instruction(Operation.muli));
                    }else{
                        push = Tokentype.DOUBLE;
                        CurrentFunction_Instruction.add(new Instruction(Operation.mulf));
                    }
                    break;
                case DIV:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.INT;
                        CurrentFunction_Instruction.add(new Instruction(Operation.divi));
                    }else{
                        push = Tokentype.DOUBLE;
                        CurrentFunction_Instruction.add(new Instruction(Operation.divf));
                    }
                    break;
                case EQ:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpi));
                        CurrentFunction_Instruction.add(new Instruction(Operation.not));
                    }else{
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpf));
                        CurrentFunction_Instruction.add(new Instruction(Operation.not));
                    }
                    break;
                case NEQ:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpi));
                    }else{
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpf));
                    }
                    break;
                case LT:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpi));
                        CurrentFunction_Instruction.add(new Instruction(Operation.setlt));
                    }else{
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpf));
                        CurrentFunction_Instruction.add(new Instruction(Operation.setlt));
                    }
                    break;
                case GT:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpi));
                        CurrentFunction_Instruction.add(new Instruction(Operation.setgt));
                    }else{
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpf));
                        CurrentFunction_Instruction.add(new Instruction(Operation.setgt));
                    }
                    break;
                case LE:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpi));
                        CurrentFunction_Instruction.add(new Instruction(Operation.setgt));
                        CurrentFunction_Instruction.add(new Instruction(Operation.not));
                    }else{
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpf));
                        CurrentFunction_Instruction.add(new Instruction(Operation.setgt));
                        CurrentFunction_Instruction.add(new Instruction(Operation.not));
                    }
                    break;
                case GE:
                    if(pop1 == Tokentype.INT){
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpi));
                        CurrentFunction_Instruction.add(new Instruction(Operation.setlt));
                        CurrentFunction_Instruction.add(new Instruction(Operation.not));
                    }else{
                        push = Tokentype.BOOL;
                        CurrentFunction_Instruction.add(new Instruction(Operation.cmpf));
                        CurrentFunction_Instruction.add(new Instruction(Operation.setlt));
                        CurrentFunction_Instruction.add(new Instruction(Operation.not));
                    }
                    break;
                default:
                    System.exit(-1);
            }
        }

        System.out.println("pop后的Ns： " + Ns);
        System.out.println("pop后的s: " + s);


        Ns.push(push);

        System.out.println("push N规约后 此时假装他是个IDENT,此时Ns：" + Ns);
    }

    /**
     * negate_expr
     */
    private Tokentype analyseNegateExpr() throws Exception {
        expect(Tokentype.MINUS);
        return analyseExpr(true);
    }

    /**
     * analyseGroupExpr
     */
    private Tokentype analyseGroupExpr() throws Exception {
        expect(Tokentype.L_PAREN);
        Tokentype tokenType = analyseExpr(true);
        expect(Tokentype.R_PAREN);
        System.out.println("group 分析完了！！！");
        return tokenType;
    }

    /**
     * function_param_list分析入口
     */
    private void analyseFunctionParamList() throws Exception {
        //function_param_list -> function_param (',' function_param)*
        //function_param -> 'const'? IDENT ':' ty
        analyseFunctionParam();

        while (nextIf(Tokentype.COMMA) != null) {
            analyseFunctionParam();
        }
    }

    /**
     * function_param分析
     */
    private void analyseFunctionParam() throws Exception {
        //function_param -> 'const'? IDENT ':' ty
        if (nextIf(Tokentype.CONST_KW) != null) { //如果有const，说明为常量
            Token nameToken = expect(Tokentype.IDENT); //取常量名
            expect(Tokentype.COLON); // :
            Token tyToken = expect(Tokentype.IDENT); //取常量值

            switch (tyToken.getValue().toString()) {
                case "double":
                    //加入符号表
                    addSymbol(nameToken.getValue().toString(), true, Tokentype.DOUBLE, SymbolType.args, nameToken.getStartPos()); //常量加入符号栈
                    this.symbolTable.get(this.symbolInt.peek() - 1).getParams().add(Tokentype.DOUBLE); //把形参放进fn的paramlist
                    break;
                case "int":
                    //加入符号表
                    addSymbol(nameToken.getValue().toString(), true, Tokentype.INT, SymbolType.args, nameToken.getStartPos()); //常量加入符号栈
                    this.symbolTable.get(this.symbolInt.peek() - 1).getParams().add(Tokentype.INT);
                    break;
                default:
                    throw new Exception();
            }


            //TODO
        } else { //没有const说明为变量
            Token nameToken = expect(Tokentype.IDENT); //取常量名
            expect(Tokentype.COLON); // :
            Token tyToken = expect(Tokentype.IDENT); //取常量值

            switch (tyToken.getValue().toString()) {
                case "double":
                    //加入符号表
                    addSymbol(nameToken.getValue().toString(), false, Tokentype.DOUBLE, SymbolType.args, nameToken.getStartPos()); //常量加入符号栈
                    this.symbolTable.get(this.symbolInt.peek() - 1).getParams().add(Tokentype.DOUBLE); //把形参放进fn的paramlist
                    break;
                case "int":
                    //加入符号表
                    addSymbol(nameToken.getValue().toString(), false, Tokentype.INT, SymbolType.args, nameToken.getStartPos()); //常量加入符号栈
                    this.symbolTable.get(this.symbolInt.peek() - 1).getParams().add(Tokentype.INT);
                    break;
                default:
                    throw new Exception();


                    //TODO
            }
        }
    }


    /**
     * stmt
     */
    private boolean analyseStmt(Tokentype tyTokentype, boolean isWhile , ArrayList<Integer> breakEndPos, int continuePos) throws Exception {
        //stmt ->
        //      expr_stmt
        //    | decl_stmt
        //    | if_stmt
        //    | while_stmt
        //    | break_stmt
        //    | continue_stmt
        //    | return_stmt
        //    | block_stmt
        //    | empty_stmt

        //expr_stmt
        if (check(Tokentype.MINUS) || check(Tokentype.IDENT) || check(Tokentype.UINT_LITERAL) || check(Tokentype.L_PAREN) || check(Tokentype.DOUBLE_LITERAL) || check(Tokentype.STRING_LITERAL) || check(Tokentype.CHAR_LITERAL)) { //expr_stmt
            System.out.println("expr_stmt分析");
            analyseExprStmt();
        }

        //decl_stmt
        if (check(Tokentype.CONST_KW)) { //decl_stmt
            System.out.println("decl语句开始分析");
            analyseConstDeclaration(false);
        }

        //let_stmt
        if (check(Tokentype.LET_KW)) {
            System.out.println("let语句开始分析");
            analyseVariableDeclaration(false);
        }

        //if_stmt
        if (check(Tokentype.IF_KW)) { //if_stmt
            System.out.println("if语句开始分析");
            return analyseIfStmt(tyTokentype, isWhile, breakEndPos, continuePos);
        }

        //while_stmt
        if (check(Tokentype.WHILE_KW)) {
            System.out.println("while语句开始分析");
            analyseWhileStmt(tyTokentype);
        }

        //break_stmt
        if (check(Tokentype.BREAK_KW)) {
            System.out.println("break语句开始分析");
            if(!isWhile){
                throw new Exception( );
            }
            analyseBreakStmt();
            CurrentFunction_Instruction.add(new Instruction(Operation.br));
            int breakPos = CurrentFunction_Instruction.size()-1;
            breakEndPos.add(breakPos);
        }

        //continue_stmt
        if (check(Tokentype.CONTINUE_KW)) {
            System.out.println("continue语句开始分析");
            if(!isWhile){
                throw new Exception( );
            }
            analyseContinueStmt();
            CurrentFunction_Instruction.add(new Instruction(Operation.br,continuePos-CurrentFunction_Instruction.size()));
        }

        //return_stmt
        if (check(Tokentype.RETURN_KW)) {
            System.out.println("return 语句开始分析");
            analyseReturnStmt(tyTokentype);
            return true; //有return
        }

        //block_stmt
        if (check(Tokentype.L_BRACE)) {
            System.out.println("block语句开始分析");
            return analyseBlockStmt(false, tyTokentype, isWhile, breakEndPos, continuePos);
        }

        //empty_stmt
        if (check(Tokentype.SEMICOLON)) {
            System.out.println("empty语句开始分析");
            analyseEmptyStmt();
        }
        return false;
    }


    /**
     * empty_stmt
     *
     * @throws Exception
     */
    private void analyseEmptyStmt() throws Exception {
        //empty_stmt -> ';'
        expect(Tokentype.SEMICOLON);
    }

    /**
     * block_stmt
     *
     * @throws Exception
     */
    private boolean analyseBlockStmt(boolean isFn, Tokentype tyTokentype, boolean isWhile, ArrayList<Integer> breakEndPos, int continuePos) throws Exception {
        //block_stmt -> '{' stmt* '}'
        boolean hasReturn = false;
        expect(Tokentype.L_BRACE);

        if (!isFn) {
            symbolInt.push(symbolTable.size());
        }
        System.out.println(check(Tokentype.MINUS));
        while (check(Tokentype.MINUS) || check(Tokentype.IDENT) || check(Tokentype.UINT_LITERAL) || check(Tokentype.DOUBLE_LITERAL) || check(Tokentype.STRING_LITERAL) || check(Tokentype.CHAR_LITERAL) || check(Tokentype.L_PAREN) || check(Tokentype.LET_KW) ||
                check(Tokentype.CONST_KW) || check(Tokentype.IF_KW) || check(Tokentype.WHILE_KW) || check(Tokentype.BREAK_KW) || check(Tokentype.CONTINUE_KW) || check(Tokentype.RETURN_KW) || check(Tokentype.SEMICOLON) || check(Tokentype.L_BRACE)) {
//            System.out.println("这是block里的stmt循环分析！");
            if(!hasReturn){
                hasReturn = analyseStmt(tyTokentype, isWhile, breakEndPos, continuePos);//进入stmt循环分析
            }
            else{
                analyseStmt(tyTokentype, isWhile, breakEndPos, continuePos); //进入stmt循环分析
            }
        }
        expect(Tokentype.R_BRACE);

        //删块
        int index = symbolInt.pop();
        while (symbolTable.size() > index) {
            Symbol s = symbolTable.pop();
            if (s.getChain() != -1) { //如果chain不为-1，更新hash表中的对应值
                symbolHash.put(s.getName(), s.getChain());
            } else { //没有重合元素，直接remove
                symbolHash.remove(s.getName());
            }
        }

        return hasReturn;
    }

    /**
     * return_stmt
     */
    private void analyseReturnStmt(Tokentype tyTokentype) throws Exception {
        //return_stmt -> 'return' expr? ';'
        expect(Tokentype.RETURN_KW);
        if(tyTokentype == Tokentype.INT || tyTokentype == Tokentype.DOUBLE){
            CurrentFunction_Instruction.add(new Instruction(Operation.arga, 0));
        }
        if (check(Tokentype.MINUS) || check(Tokentype.IDENT) || check(Tokentype.UINT_LITERAL) || check(Tokentype.L_PAREN) || check(Tokentype.DOUBLE_LITERAL) || check(Tokentype.STRING_LITERAL) || check(Tokentype.CHAR_LITERAL)) {
            Tokentype exprType = analyseExpr(true);
            if(exprType != tyTokentype){
                throw new Exception();
            }
        }else{
            if(tyTokentype != Tokentype.VOID){
                throw new Exception();
            }
        }
        if(tyTokentype == Tokentype.INT || tyTokentype == Tokentype.DOUBLE){
            CurrentFunction_Instruction.add(new Instruction(Operation.store64));
        }
        CurrentFunction_Instruction.add(new Instruction(Operation.ret));
        expect(Tokentype.SEMICOLON);
    }

    /**
     * continue_stmt
     */
    private void analyseContinueStmt() throws Exception {
        //continue_stmt -> 'continue' ';'
        expect(Tokentype.CONTINUE_KW);
        expect(Tokentype.SEMICOLON);
    }

    /**
     * break_stmt
     */
    private void analyseBreakStmt() throws Exception {
        //break_stmt -> 'break' ';'
        expect(Tokentype.BREAK_KW);
        expect(Tokentype.SEMICOLON);
    }

    /**
     * while_stmt
     */
    private void analyseWhileStmt(Tokentype tyTokentype) throws Exception {
        //while_stmt -> 'while' expr block_stmt
        expect(Tokentype.WHILE_KW);

        int InitPos=CurrentFunction_Instruction.size()-1;
        Tokentype whileExpr = analyseExpr(true);

        ArrayList<Integer> breakEndPos = new ArrayList<>();


        CurrentFunction_Instruction.add(new Instruction(Operation.brtrue, 1));

        CurrentFunction_Instruction.add(new Instruction(Operation.br));
        int currentPos = CurrentFunction_Instruction.size()-1;

        if(whileExpr == Tokentype.VOID){
            throw new Exception();
        }
        analyseBlockStmt(false, tyTokentype, true, breakEndPos, InitPos);
        CurrentFunction_Instruction.add(new Instruction(Operation.br, InitPos-CurrentFunction_Instruction.size()));
        CurrentFunction_Instruction.get(currentPos).setX(CurrentFunction_Instruction.size()-1 - currentPos);
        for(int i = 0; i < breakEndPos.size(); i ++){
            CurrentFunction_Instruction.get(breakEndPos.get(i)).setX(CurrentFunction_Instruction.size()-1-breakEndPos.get(i)); //存每一个break
        }
    }

    /**
     * if_stmt
     */
    private boolean analyseIfStmt(Tokentype tyTokentype, boolean isWhile, ArrayList<Integer> breakEndPos, int continuePos) throws Exception {
        //if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
        expect(Tokentype.IF_KW);
        Tokentype ifexpr = analyseExpr(true);
        if(ifexpr == Tokentype.VOID){
            throw new Exception();
        }
        boolean hasReturn = false;
        boolean hasElse = false;
        System.out.println("进入if的{}块了！");

        CurrentFunction_Instruction.add(new Instruction(Operation.brtrue, 1));
        CurrentFunction_Instruction.add(new Instruction(Operation.br));
        int currentPos = CurrentFunction_Instruction.size()-1; //br指令的当前位置

        hasReturn = analyseBlockStmt(false, tyTokentype, isWhile, breakEndPos, continuePos); //if 第一个block块
        CurrentFunction_Instruction.add(new Instruction(Operation.br)); //if块结束跳转
        int endPos = CurrentFunction_Instruction.size()-1;
        CurrentFunction_Instruction.get(currentPos).setX(CurrentFunction_Instruction.size()-1 - currentPos);



        ArrayList<Integer> Pos = new ArrayList<>();
        while (nextIf(Tokentype.ELSE_KW) != null) { //如果有else
            System.out.println("有else哦");
            if (nextIf(Tokentype.IF_KW) != null) { // 是else if的情况
                ifexpr = analyseExpr(true);
                CurrentFunction_Instruction.add(new Instruction(Operation.brtrue, 1));
                CurrentFunction_Instruction.add(new Instruction(Operation.br));
                int currentPos1 = CurrentFunction_Instruction.size()-1; //br指令的当前位置



                if(ifexpr == Tokentype.VOID){
                    throw new Exception();
                }
                hasReturn &= analyseBlockStmt(false, tyTokentype, isWhile, breakEndPos, continuePos);
                CurrentFunction_Instruction.add(new Instruction(Operation.br));
                Pos.add(CurrentFunction_Instruction.size()-1);
                CurrentFunction_Instruction.get(currentPos1).setX(CurrentFunction_Instruction.size()-1 - currentPos1);
            } else if (check(Tokentype.L_BRACE)) { //只有else的情况
                hasReturn &= analyseBlockStmt(false, tyTokentype, isWhile, breakEndPos, continuePos);
                hasElse = true;
                break;
            }
        }
        CurrentFunction_Instruction.get(endPos).setX(CurrentFunction_Instruction.size()-1-endPos);
        for(int i = 0; i < Pos.size(); i ++){
            CurrentFunction_Instruction.get(Pos.get(i)).setX(CurrentFunction_Instruction.size()-1-Pos.get(i)); //循环存每一个elseif
        }
        if(!hasElse){
            return false;
        }
        return hasReturn;
    }

    /**
     * expr_stmt
     */
    private void analyseExprStmt() throws Exception {
        //expr_stmt -> expr ';'
        Tokentype t = null;
        if (check(Tokentype.MINUS) || check(Tokentype.IDENT) || check(Tokentype.UINT_LITERAL) || check(Tokentype.L_PAREN) || check(Tokentype.DOUBLE_LITERAL) || check(Tokentype.STRING_LITERAL) || check(Tokentype.CHAR_LITERAL)) {
            t = analyseExpr(true);
        }
        if(t != Tokentype.VOID){
            CurrentFunction_Instruction.add(new Instruction(Operation.popn, 1));
        }
        expect(Tokentype.SEMICOLON);
    }


}

