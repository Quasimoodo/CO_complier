package compiler.tokenizer;

public enum Tokentype {

    FN_KW ,   // -> 'fn'
    LET_KW,   // -> 'let'
    CONST_KW,  //-> 'const'
    AS_KW   ,  //-> 'as'
    WHILE_KW , //-> 'while'
    IF_KW     ,//-> 'if'
    ELSE_KW   ,//-> 'else'
    RETURN_KW ,//-> 'return'

// 这两个是扩展 c0 的
    BREAK_KW  ,//-> 'break'
    CONTINUE_KW,// -> 'continue'

    IDENT,  //标识符

    //字面量
    UINT_LITERAL,//无符号整数
    STRING_LITERAL,//字符串
    CHAR_LITERAL,
    DOUBLE_LITERAL,


    //
    COMENT,
    //运算符
    NEG,
    PLUS,
    MINUS,
    MUL,
    DIV,
    ASSIGN,
    EQ,
    NEQ,
    LT,
    GT,
    LE,
    GE,
    L_PAREN,
    R_PAREN,
    L_BRACE,
    R_BRACE,
    ARROW,
    COMMA,
    COLON,
    SEMICOLON,
    SHARP,

    EOF,
    TYPE;
    public String toString()
    {
        return this.name();

    }


}
