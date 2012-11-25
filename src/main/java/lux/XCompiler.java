package lux;

import java.io.IOException;
import java.io.StringReader;

import lux.compiler.PathOptimizer;
import lux.compiler.SaxonTranslator;
import lux.exception.LuxException;
import lux.functions.FieldTerms;
import lux.functions.LuxCount;
import lux.functions.LuxExists;
import lux.functions.LuxSearch;
import lux.functions.Transform;
import lux.index.FieldName;
import lux.index.IndexConfiguration;
import lux.saxon.Config;
import lux.saxon.DocIDNumberAllocator;
import lux.saxon.Evaluator.Dialect;
import lux.saxon.TransformErrorListener;
import lux.xpath.FunCall;
import lux.xquery.XQuery;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XsltCompiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XCompiler {
    private final Logger logger;
    private final Processor processor;
    private final Dialect dialect;
    private final CollectionURIResolver defaultCollectionURIResolver;
    private final String uriFieldName;
    private final IndexConfiguration indexConfig;
    // Warning: error listener may receive errors from multiple threads since the compiler
    // is shared.  This is a limitation of the Saxon API, which provides a threadsafe compiler
    // class whose error reporting is not thread-safe.
    private final TransformErrorListener errorListener;
    private XQueryCompiler xqueryCompiler;
    private XPathCompiler xpathCompiler;
    private XsltCompiler xsltCompiler;
    private boolean enableLuxOptimization;

    /** Creates a Compiler configured according to the capabilities of a wrapped instance of a Saxon Processor.
     * Saxon-HE allows us to optimize result sorting and lazy evaluation.  Saxon-PE and -EE provide
     * PTree storage mechanism, and their own optimizations.
     */
    public XCompiler (IndexConfiguration indexConfig) {
        this (makeProcessor(), indexConfig);
    }
    
    protected XCompiler(Processor processor, IndexConfiguration indexConfig) {
        dialect = Dialect.XQUERY_1;
        this.indexConfig = indexConfig;
        enableLuxOptimization = indexConfig != null && indexConfig.isIndexingEnabled();
        
        this.processor = processor;
        Configuration config = processor.getUnderlyingConfiguration();
        config.setDocumentNumberAllocator(new DocIDNumberAllocator());
        config.setConfigurationProperty(FeatureKeys.XQUERY_PRESERVE_NAMESPACES, false);
        config.getParseOptions().setEntityResolver(new EmptyEntityResolver());

        defaultCollectionURIResolver = config.getCollectionURIResolver();
        registerExtensionFunctions(processor);
        if (indexConfig != null && indexConfig.isIndexingEnabled()) {
            uriFieldName = indexConfig.getFieldName(FieldName.URI);
        } else {
            uriFieldName = null;
        }
        //this.dialect = dialect;
        logger = LoggerFactory.getLogger(getClass());
        errorListener = new TransformErrorListener();
    }
    
    static Processor makeProcessor () {
        try {
            if (Class.forName("com.saxonica.config.EnterpriseConfiguration") != null) {
                return new Processor (true);
            }
        } catch (ClassNotFoundException e) { }
        try {
            if (Class.forName("com.saxonica.config.ProfessionalConfiguration") != null) {
                return new Processor (true);
            }
        } catch (ClassNotFoundException e) { }
        return new Processor (new Config());
    }
    
    private void registerExtensionFunctions(Processor processor) {
        processor.registerExtensionFunction(new LuxSearch());
        processor.registerExtensionFunction(new LuxCount());
        processor.registerExtensionFunction(new LuxExists());
        processor.registerExtensionFunction(new FieldTerms());
        processor.registerExtensionFunction(new Transform());
    }
    
    class EmptyEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return new InputSource(new StringReader(""));
        }
    }
    
    public XQueryExecutable compile(String exprString) throws LuxException {
        switch (dialect) {
        /*
            case XPATH_1: case XPATH_2:
            return compileXPath(exprString);
         */
        case XQUERY_1:
            return compileXQuery(exprString);
        default:
            throw new LuxException ("Unsupported query dialect: " + dialect);
        }
    }

    // for testing
    private XQuery lastOptimized;
    XQuery getLastOptimized () { return lastOptimized; }
    
    private XQueryExecutable compileXQuery(String exprString) throws LuxException {
        XQueryExecutable xquery;
        try {
            xquery = getXQueryCompiler().compile(exprString);
        } catch (SaxonApiException e) {
            throw new LuxException (e);
        }
        if (! isEnableLuxOptimization()) {
            return xquery;
        }
        SaxonTranslator translator = makeTranslator();
        XQuery abstractQuery = translator.queryFor (xquery);
        PathOptimizer optimizer = new PathOptimizer(indexConfig);

        XQuery optimizedQuery = optimizer.optimize(abstractQuery);
        lastOptimized = optimizedQuery;
        try {
            xquery = getXQueryCompiler().compile(optimizedQuery.toString());
        } catch (SaxonApiException e) {
            throw new LuxException (e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("optimized xquery: " + optimizedQuery.toString());
        }
        return xquery;
    }

    public void declareNamespace (String prefix, String namespace) {
        switch (dialect) {
        case XPATH_1: case XPATH_2:
            getXPathCompiler().declareNamespace(prefix, namespace);
            break;
        case XQUERY_1:
            getXQueryCompiler().declareNamespace(prefix, namespace);
            break;
        default:
            break;
        }
    }
        
    public XsltCompiler getXsltCompiler () {
        if (xsltCompiler == null) {
            xsltCompiler = processor.newXsltCompiler();
            xsltCompiler.setErrorListener(errorListener);
        }
        errorListener.clear();
        return xsltCompiler;
    }

    public XQueryCompiler getXQueryCompiler () {
        if (xqueryCompiler == null) {
            xqueryCompiler = processor.newXQueryCompiler();
            xqueryCompiler.declareNamespace("lux", FunCall.LUX_NAMESPACE);
            xqueryCompiler.setErrorListener(errorListener);
        }
        errorListener.clear();
        return xqueryCompiler;
    }

    public XPathCompiler getXPathCompiler () {
        if (xpathCompiler == null) {
            xpathCompiler = processor.newXPathCompiler();
            xpathCompiler.declareNamespace("lux", FunCall.LUX_NAMESPACE);
            xpathCompiler.declareNamespace("fn", FunCall.FN_NAMESPACE);
        }
        return xpathCompiler;
    }

    public IndexConfiguration getIndexConfiguration () {
        return indexConfig;
    }

    public Processor getProcessor() {
        return processor;
    }
    
    public SaxonTranslator makeTranslator () {
        return new SaxonTranslator(processor.getUnderlyingConfiguration());
    }
    
    public CollectionURIResolver getDefaultCollectionURIResolver() {
        return defaultCollectionURIResolver;
    }

    public String getUriFieldName() {
        return uriFieldName;
    }

    public boolean isEnableLuxOptimization() {
        return enableLuxOptimization;
    }

    public void setEnableLuxOptimization(boolean enableLuxOptimization) {
        this.enableLuxOptimization = enableLuxOptimization;
    }

    public TransformErrorListener getErrorListener() {
        return errorListener;
    }
    
}