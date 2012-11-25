package lux.index.field;

import java.util.Collections;

import lux.index.XPathValueMapper;
import lux.index.XmlIndexer;
import lux.index.analysis.QNameValueTokenStream;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.util.Version;

public class QNameValueField extends FieldDefinition {
    
    private static final QNameValueField instance = new QNameValueField();
    
    public static QNameValueField getInstance() {
        return instance;
    }
    
    protected QNameValueField () {
        super ("lux_path", new WhitespaceAnalyzer(Version.LUCENE_34), Store.NO, Type.TOKENS, TermVector.NO);
    }
    
    @Override
    public Iterable<Field> getFieldValues(XmlIndexer indexer) {
        // replace with a custom Fieldable
        XPathValueMapper mapper = (XPathValueMapper) indexer.getPathMapper();        
        return new FieldValues (indexer.getConfiguration(), this, Collections.singleton
                (new Field(indexer.getConfiguration().getFieldName(this), 
                        new QNameValueTokenStream (mapper.getPathValues()), TermVector.NO)));
    }

}
