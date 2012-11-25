package lux.functions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import lux.IndexTestSupport;
import lux.saxon.Evaluator;
import lux.saxon.XdmResultSet;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmItem;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldTermsTest {
    
    private static IndexTestSupport index;
    
    @BeforeClass
    public static void setup () throws Exception {
        index = new IndexTestSupport("lux/reader-test.xml");
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        index.close();
    }

    @Test
    public void testFieldTerms () {
        ArrayList<String> terms = getFieldTerms("lux:fieldTerms('lux_elt_name')");
        assertArrayEquals (new String[] {"entities", "test", "title", "token"}, 
                terms.toArray(new String[0]));
    }
    
    @Test
    public void testAllTerms () {
        ArrayList<String> terms = getFieldTerms("lux:fieldTerms()");
        // make the condition a bit loose so that if we add fields or more
        // text, the test will still pass OK.  We basically want to make sure that
        // argument passing works when there is no field or start position specified
        assertTrue (terms.size() > 50);
    }
    
    @Test
    public void testFieldTermsStart () {
        ArrayList<String> terms = getFieldTerms("lux:fieldTerms('lux_elt_name', 'ti')");
        assertArrayEquals (new String[] {"title", "token"}, 
                terms.toArray(new String[0]));
    }

    private ArrayList<String> getFieldTerms(String xquery) {
        Evaluator eval = index.makeEvaluator();
        XQueryExecutable exec = eval.getCompiler().compile(xquery);
        XdmResultSet results = eval.evaluate(exec);
        ArrayList<String> terms = new ArrayList<String>();
        for (XdmItem term : results) {
            terms.add (term.getStringValue());
        }
        return terms;
    }    
}
