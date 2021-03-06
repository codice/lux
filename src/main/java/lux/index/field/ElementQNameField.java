package lux.index.field;

import lux.index.FieldRole;
import lux.index.XmlIndexer;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Field.Store;

public final class ElementQNameField extends FieldDefinition {

    public ElementQNameField () {
        super (FieldRole.ELT_QNAME, new KeywordAnalyzer(), Store.NO, Type.STRING);
    }
    
    @Override
    public Iterable<?> getValues(XmlIndexer indexer) {
        return indexer.getPathMapper().getEltQNameCounts().keySet();
    }

}
