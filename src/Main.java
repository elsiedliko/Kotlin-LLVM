import grammar.KotlinLexer;
import grammar.KotlinParser;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import parser.LLVMActions;

public class Main {

    public static void main(String[] args) throws Exception {
        ANTLRFileStream input = new ANTLRFileStream("resources/sample.kt");
        KotlinLexer lexer = new KotlinLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        KotlinParser parser = new KotlinParser(tokens);

        ParseTree tree = parser.kotlinFile(); //TODO HAVE NO IDEA WHAT TO PUT HERE

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new LLVMActions(), tree);
    }
}
