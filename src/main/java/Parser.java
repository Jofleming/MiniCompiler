import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Parser Class for mini compiler in Java.
 * @author Jordan Fleming
 * @version 1.0
 * @since 5/18/2024
 */
class Parser {

    private List<Token> source;
    private Token token;
    private int position;

    public static String ParseLexicalOutput(String result) { //TODO: CREATE METHOD)
        return "";
    }

    /**
     * Inner Node class for tree creation.
     */
    static class Node {
        public NodeType nt;
        public Node left, right;
        public String value;

        Node() {
            this.nt = null;
            this.left = null;
            this.right = null;
            this.value = null;
        }
        Node(NodeType node_type, Node left, Node right, String value) {
            this.nt = node_type;
            this.left = left;
            this.right = right;
            this.value = value;
        }
        public static Node make_node(NodeType nodetype, Node left, Node right) {
            return new Node(nodetype, left, right, "");
        }
        public static Node make_node(NodeType nodetype, Node left) {
            return new Node(nodetype, left, null, "");
        }
        public static Node make_leaf(NodeType nodetype, String value) {
            return new Node(nodetype, null, null, value);
        }
    }

    /**
     * Token class for token representation.
     */
    static class Token {
        public TokenType tokentype;
        public String value;
        public int line;
        public int pos;

        Token(TokenType token, String value, int line, int pos) {
            this.tokentype = token; this.value = value; this.line = line; this.pos = pos;
        }
        @Override
        public String toString() {
            return String.format("%5d  %5d %-15s %s", this.line, this.pos, this.tokentype, this.value);
        }
    }

    /**
     * Enum token types
     */
    static enum TokenType {
        End_of_input(false, false, false, -1, NodeType.nd_None),
        Op_multiply(false, true, false, 13, NodeType.nd_Mul),
        Op_divide(false, true, false, 13, NodeType.nd_Div),
        Op_mod(false, true, false, 13, NodeType.nd_Mod),
        Op_add(false, true, false, 12, NodeType.nd_Add),
        Op_subtract(false, true, false, 12, NodeType.nd_Sub),
        Op_negate(false, false, true, 14, NodeType.nd_Negate),
        Op_not(false, false, true, 14, NodeType.nd_Not),
        Op_less(false, true, false, 10, NodeType.nd_Lss),
        Op_lessequal(false, true, false, 10, NodeType.nd_Leq),
        Op_greater(false, true, false, 10, NodeType.nd_Gtr),
        Op_greaterequal(false, true, false, 10, NodeType.nd_Geq),
        Op_equal(false, true, true, 9, NodeType.nd_Eql),
        Op_notequal(false, true, false, 9, NodeType.nd_Neq),
        Op_assign(false, false, false, -1, NodeType.nd_Assign),
        Op_and(false, true, false, 5, NodeType.nd_And),
        Op_or(false, true, false, 4, NodeType.nd_Or),
        Keyword_if(false, false, false, -1, NodeType.nd_If),
        Keyword_else(false, false, false, -1, NodeType.nd_None),
        Keyword_while(false, false, false, -1, NodeType.nd_While),
        Keyword_print(false, false, false, -1, NodeType.nd_None),
        Keyword_putc(false, false, false, -1, NodeType.nd_None),
        LeftParen(false, false, false, -1, NodeType.nd_None),
        RightParen(false, false, false, -1, NodeType.nd_None),
        LeftBrace(false, false, false, -1, NodeType.nd_None),
        RightBrace(false, false, false, -1, NodeType.nd_None),
        Semicolon(false, false, false, -1, NodeType.nd_None),
        Comma(false, false, false, -1, NodeType.nd_None),
        Identifier(false, false, false, -1, NodeType.nd_Ident),
        Integer(false, false, false, -1, NodeType.nd_Integer),
        String(false, false, false, -1, NodeType.nd_String);

        private final int precedence;
        private final boolean right_assoc;
        private final boolean is_binary;
        private final boolean is_unary;
        private final NodeType node_type;

        TokenType(boolean right_assoc, boolean is_binary, boolean is_unary, int precedence, NodeType node) {
            this.right_assoc = right_assoc;
            this.is_binary = is_binary;
            this.is_unary = is_unary;
            this.precedence = precedence;
            this.node_type = node;
        }
        boolean isRightAssoc() { return this.right_assoc; }
        boolean isBinary() { return this.is_binary; }
        boolean isUnary() { return this.is_unary; }
        int getPrecedence() { return this.precedence; }
        NodeType getNodeType() { return this.node_type; }
    }
    static enum NodeType {
        nd_None(""), nd_Ident("Identifier"), nd_String("String"), nd_Integer("Integer"), nd_Sequence("Sequence"), nd_If("If"),
        nd_Prtc("Prtc"), nd_Prts("Prts"), nd_Prti("Prti"), nd_While("While"),
        nd_Assign("Assign"), nd_Negate("Negate"), nd_Not("Not"), nd_Mul("Multiply"), nd_Div("Divide"), nd_Mod("Mod"), nd_Add("Add"),
        nd_Sub("Subtract"), nd_Lss("Less"), nd_Leq("LessEqual"),
        nd_Gtr("Greater"), nd_Geq("GreaterEqual"), nd_Eql("Equal"), nd_Neq("NotEqual"), nd_And("And"), nd_Or("Or");

        private final String name;

        NodeType(String name) {
            this.name = name;
        }

        @Override
        public String toString() { return this.name; }
    }
    static void error(int line, int pos, String msg) {
        if (line > 0 && pos > 0) {
            System.out.printf("%s in line %d, pos %d\n", msg, line, pos);
        } else {
            System.out.println(msg);
        }
        System.exit(1);
    }
    Parser(List<Token> source) {
        this.source = source;
        this.token = null;
        this.position = 0;
    }
    Token getNextToken() {
        this.token = this.source.get(this.position++);
        return this.token;
    }

    /**
     * Method to break down expression into base parts and return node with token.
     * @param p  precedence as int
     * @return   node with token
     */
    Node expr(int p) {
        // create nodes for token types such as LeftParen, Op_add, Op_subtract, etc.
        // be very careful here and be aware of the precendence rules for the AST tree
        Node result = null, node;
        TokenType check;
        int q;

        if (this.token.tokentype == TokenType.LeftParen) {
            result = paren_expr();
        } else if (this.token.tokentype == TokenType.Op_add || this.token.tokentype == TokenType.Op_subtract) {
            check = (this.token.tokentype == TokenType.Op_subtract) ? TokenType.Op_negate : TokenType.Op_add;
            getNextToken();
            node = expr(TokenType.Op_negate.getPrecedence());
            result = (check == TokenType.Op_negate) ? Node.make_node(NodeType.nd_Negate, node) : node;
        } else if (this.token.tokentype == TokenType.Op_not) {
            getNextToken();
            result = Node.make_node(NodeType.nd_Not, expr(TokenType.Op_not.getPrecedence()));
        } else if (this.token.tokentype == TokenType.Identifier) {
            result = Node.make_leaf(NodeType.nd_Ident, this.token.value);
            getNextToken();
        } else if (this.token.tokentype == TokenType.Integer) {
            result = Node.make_leaf(NodeType.nd_Integer, this.token.value);
            getNextToken();
        }
        while (this.token.tokentype.isBinary() && this.token.tokentype.getPrecedence() >= p) {
            check = this.token.tokentype;
            getNextToken();
            q = check.getPrecedence();
            if (!check.isRightAssoc()) {
                q++;
            }
            node = expr(q);
            result = Node.make_node(check.getNodeType(), result, node);
        }
        return result;
    }
    Node paren_expr() {
        expect("paren_expr", TokenType.LeftParen);
        Node node = expr(0);
        expect("paren_expr", TokenType.RightParen);
        return node;
    }
    void expect(String msg, TokenType s) {
        if (this.token.tokentype == s) {
            getNextToken();
            return;
        }
        error(this.token.line, this.token.pos, msg + ": Expecting '" + s + "', found: '" + this.token.tokentype + "'");
    }

    /**
     * Method to handle token held in token holder variable. Will create node depending on type of token.
     * @return      Node with
     */
    Node stmt() {
        // this one handles TokenTypes such as Keyword_if, Keyword_else, nd_If, Keyword_print, etc.
        // also handles while, end of file, braces
        Node s, s2, t = null, e, v;
        if (this.token.tokentype == TokenType.Keyword_if) {
            getNextToken();
            e = paren_expr();
            s = stmt();
            s2 = null;
            if (this.token.tokentype == TokenType.Keyword_else) {
                getNextToken();
                s2 = stmt();
            }
            t = Node.make_node(NodeType.nd_If, e, Node.make_node(NodeType.nd_If, s, s2));
        } else if (this.token.tokentype == TokenType.Keyword_putc) {
            getNextToken();
            e = paren_expr();
            t = Node.make_node(NodeType.nd_Prtc, e);
            expect("Putc", TokenType.Semicolon);
        } else if (this.token.tokentype == TokenType.Keyword_print) {
            getNextToken();
            expect("Print", TokenType.LeftParen);
            while (true) {
                if (this.token.tokentype == TokenType.String) {
                    e = Node.make_node(NodeType.nd_Prts, Node.make_leaf(NodeType.nd_String, this.token.value));
                    getNextToken();
                } else {
                    e = Node.make_node(NodeType.nd_Prti, expr(0), null);
                }
                t = Node.make_node(NodeType.nd_Sequence, t, e);
                if (this.token.tokentype != TokenType.Comma) {
                    break;
                }
                getNextToken();
            }
            expect("Print", TokenType.RightParen);
            expect("Print", TokenType.Semicolon);
        } else if (this.token.tokentype == TokenType.Semicolon) {
            getNextToken();
        } else if (this.token.tokentype == TokenType.Identifier) {
            v = Node.make_leaf(NodeType.nd_Ident, this.token.value);
            getNextToken();
            expect("assign", TokenType.Op_assign);
            e = expr(0);
            t = Node.make_node(NodeType.nd_Assign, v, e);
            expect("assign", TokenType.Semicolon);
        } else if (this.token.tokentype == TokenType.Keyword_while) {
            getNextToken();
            e = paren_expr();
            s = stmt();
            t = Node.make_node(NodeType.nd_While, e, s);
        } else if (this.token.tokentype == TokenType.LeftBrace) {
            getNextToken();
            while (this.token.tokentype != TokenType.RightBrace && this.token.tokentype != TokenType.End_of_input) {
                t = Node.make_node(NodeType.nd_Sequence, t, stmt());
            }
            expect("LBrace", TokenType.RightBrace);
        } else if (this.token.tokentype == TokenType.End_of_input) {
        } else {
            error(this.token.line, this.token.pos, "Expecting start of statement, found: " + this.token.tokentype);
        }
        return t;
    }
    Node parse() {
        Node t = null;
        getNextToken();
        while (this.token.tokentype != TokenType.End_of_input) {
            t = Node.make_node(NodeType.nd_Sequence, t, stmt());
        }
        return t;
    }
    String printAST(Node t, StringBuilder sb) {
        int i = 0;
        if (t == null) {
            sb.append(";");
            sb.append("\n");
            System.out.println(";");
        } else {
            sb.append(t.nt);
            System.out.printf("%-14s", t.nt);
            if (t.nt == NodeType.nd_Ident || t.nt == NodeType.nd_Integer || t.nt == NodeType.nd_String) {
                sb.append(" " + t.value);
                sb.append("\n");
                System.out.println(" " + t.value);
            } else {
                sb.append("\n");
                System.out.println();
                printAST(t.left, sb);
                printAST(t.right, sb);
            }

        }
        return sb.toString();
    }

    static void outputToFile(String result) {
        try {
            FileWriter myWriter = new FileWriter("src/main/resources/hello.par");
            myWriter.write(result);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to run parser on a given file. Takes in a file name and parses the given file. Only able to parse 1 file
     * at a time.
     * @param args      Filename to parse.
     */
    public static void main(String[] args) {
        if (1==1) {
            try {
                String value, token;
                String result = " ";
                StringBuilder sb = new StringBuilder();
                int line, pos;
                Token t;
                boolean found;
                List<Token> list = new ArrayList<>();
                Map<String, TokenType> str_to_tokens = new HashMap<>();


                str_to_tokens.put("End_of_input", TokenType.End_of_input);
                str_to_tokens.put("Integer", TokenType.Integer);
                str_to_tokens.put("String", TokenType.String);
                str_to_tokens.put("Op_multiply", TokenType.Op_multiply);
                str_to_tokens.put("Op_divide", TokenType.Op_divide);
                str_to_tokens.put("Op_mod", TokenType.Op_mod);
                str_to_tokens.put("Op_add", TokenType.Op_add);
                str_to_tokens.put("Op_subtract", TokenType.Op_subtract);
                str_to_tokens.put("Op_less", TokenType.Op_less);
                str_to_tokens.put("Op_lessequal", TokenType.Op_lessequal);
                str_to_tokens.put("Op_greater", TokenType.Op_greater);
                str_to_tokens.put("Op_greaterequal", TokenType.Op_greaterequal);
                str_to_tokens.put("Op_equal", TokenType.Op_equal);
                str_to_tokens.put("Op_notequal", TokenType.Op_notequal);
                str_to_tokens.put("Op_assign", TokenType.Op_assign);
                str_to_tokens.put("Op_and", TokenType.Op_and);
                str_to_tokens.put("Op_or", TokenType.Op_or);
                str_to_tokens.put("Op_negate", TokenType.Op_negate);
                str_to_tokens.put("Op_not", TokenType.Op_not);
                str_to_tokens.put("Keyword_if", TokenType.Keyword_if);
                str_to_tokens.put("Keyword_else", TokenType.Keyword_else);
                str_to_tokens.put("Keyword_while", TokenType.Keyword_while);
                str_to_tokens.put("Keyword_print", TokenType.Keyword_print);
                str_to_tokens.put("Keyword_putc", TokenType.Keyword_putc);
                str_to_tokens.put("LeftParen", TokenType.LeftParen);
                str_to_tokens.put("RightParen", TokenType.RightParen);
                str_to_tokens.put("LeftBrace", TokenType.LeftBrace);
                str_to_tokens.put("RightBrace", TokenType.RightBrace);
                str_to_tokens.put("Semicolon", TokenType.Semicolon);
                str_to_tokens.put("Comma", TokenType.Comma);
                str_to_tokens.put("Identifier", TokenType.Identifier);
                // finish creating your Hashmap. I left one as a model
                File lexFile = new File(Args[0]);
                Scanner s = new Scanner(lexFile);
                String source = " ";
                while (s.hasNext()) {
                    String str = s.nextLine();
                    StringTokenizer st = new StringTokenizer(str);
                    line = Integer.parseInt(st.nextToken());
                    pos = Integer.parseInt(st.nextToken());
                    token = st.nextToken();
                    value = "";
                    while (st.hasMoreTokens()) {
                        value += st.nextToken() + " ";
                    }
                    found = false;
                    if (str_to_tokens.containsKey(token)) {
                        found = true;
                        list.add(new Token(str_to_tokens.get(token), value, line, pos));
                    }
                    if (found == false) {
                        throw new Exception("Token not found: '" + token + "'");
                    }
                }
                Parser p = new Parser(list);
                result = p.printAST(p.parse(), sb);
                outputToFile(result);
            } catch (FileNotFoundException e) {
                error(-1, -1, "Exception: " + e.getMessage());
            } catch (Exception e) {
                error(-1, -1, "Exception: " + e.getMessage());
            }
        } else {
            error(-1, -1, "No args");
        }
    }
}