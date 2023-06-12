package luceneExamples;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class LuceneBuildIndex {

    public static void main( String[] args ) {
        try {

            // change the following input and output paths to your local ones
            String pathCorpus = "C:\\Users\\T-GAMER\\Documents\\Lucene\\example_corpus\\example_corpus.gz";
            String pathIndex = "C:\\Users\\T-GAMER\\Documents\\Lucene\\example_corpus\\example_index";

            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );

            // Analyzer specifies options for text tokenization and normalization (e.g., stemming, stop words removal, case-folding)
            Analyzer analyzer = new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents( String fieldName ) {
                    // Etapa 1: tokenização (o StandardTokenizer do Lucene é adequado para a maioria das ocasiões de recuperação de texto)
                    TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
                    // Etapa 2: transformar todos os tokens em minúsculos 
                    ts = new TokenStreamComponents( ts.getSource(), new LowerCaseFilter( ts.getTokenStream() ) );
                    // Etapa 3: remover palavras de parada (desnecessário remover palavras de parada, a menos que você não possa pagar o espaço extra em disco)
                    // A linha abaixo é utilizada para remover stop-words
                    // ts = new TokenStreamComponents( ts.getSource(), new StopFilter( ts.getTokenStream(), EnglishAnalyzer.ENGLISH_STOP_WORDS_SET ) );
                    // Etapa 4: aplicar ou não stemming
                    // Podemos escolher qual modelo de steamming pretendemos utilizar
                    ts = new TokenStreamComponents( ts.getSource(), new KStemFilter( ts.getTokenStream() ) );
                    // ts = new TokenStreamComponents( ts.getSource(), new PorterStemFilter( ts.getTokenStream() ) );
                    return ts;
                }
            };
            
            IndexWriterConfig config = new IndexWriterConfig( analyzer );
            // Observe que IndexWriterConfig.OpenMode.CREATE substituirá o índice original na pasta
            config.setOpenMode( IndexWriterConfig.OpenMode.CREATE );
            // O BM25Similarity padrão do Lucene armazena o tamanho do campo do documento usando um método de baixa precisão.
            // Utilizamos o BM25SimilarityOriginal para armazenar os valores de similaridade do documento original no índice.
            config.setSimilarity( new BM25SimilarityOriginal() );
            
            IndexWriter ixwriter = new IndexWriter( dir, config );
            
            // Aqui configuramos o Field
            FieldType fieldTypeMetadata = new FieldType();
            fieldTypeMetadata.setOmitNorms( true );
            fieldTypeMetadata.setIndexOptions( IndexOptions.DOCS );
            fieldTypeMetadata.setStored( true );
            fieldTypeMetadata.setTokenized( false );
            fieldTypeMetadata.freeze();
            
            // Esta é a configuração de Field para Textfield normal 
            FieldType fieldTypeText = new FieldType();
            fieldTypeText.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
            fieldTypeText.setStoreTermVectors( true );
            fieldTypeText.setStoreTermVectorPositions( true );
            fieldTypeText.setTokenized( true );
            fieldTypeText.setStored( true );
            fieldTypeText.freeze();
            
            
            // É necessário ler cada texto existente no corpus 
            // Criar um objeto Document para o documento analisado e adicionar o objeto, utilizando o metodo addDocument()  
            
            InputStream instream = new GZIPInputStream( new FileInputStream( pathCorpus ) );
            String corpusText = new String( IOUtils.toByteArray( instream ), "UTF-8" );
            instream.close();
            
            Pattern pattern = Pattern.compile(
                "<DOC>.+?<DOCNO>(.+?)</DOCNO>.+?<TITLE>(.+?)</TITLE>.+?<AUTHOR>(.+?)</AUTHOR>.+?<SOURCE>(.+?)</SOURCE>.+?<TEXT>(.+?)</TEXT>.+?</DOC>",
                Pattern.CASE_INSENSITIVE + Pattern.MULTILINE + Pattern.DOTALL
            );
            
            Matcher matcher = pattern.matcher( corpusText );
            
            while ( matcher.find() ) {
            
                String docno = matcher.group( 1 ).trim();
                String title = matcher.group( 2 ).trim();
                String author = matcher.group( 3 ).trim();
                String source = matcher.group( 4 ).trim();
                String text = matcher.group( 5 ).trim();
                
                // Cria o Document
                Document d = new Document();
                // Add each field to the document with the appropriate field type options
                d.add( new Field( "docno", docno, fieldTypeMetadata ) );
                d.add( new Field( "title", title, fieldTypeText ) );
                d.add( new Field( "author", author, fieldTypeText ) );
                d.add( new Field( "source", source, fieldTypeText ) );
                d.add( new Field( "text", text, fieldTypeText ) );
                // Add the document to the index
                System.out.println( "indexing document " + docno );
                ixwriter.addDocument( d );
            }
            
            ixwriter.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
