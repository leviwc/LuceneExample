package luceneExamples;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import luceneExample.utils.LuceneUtils;

import java.io.File;

public class LuceneSearchExample {

    public static void main( String[] args ) {
        try {

            String pathIndex = "C:\\Users\\T-GAMER\\Documents\\Lucene\\example_corpus\\example_index";

            Analyzer analyzer = new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents( String fieldName ) {
                    TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
                    ts = new TokenStreamComponents( ts.getSource(), new LowerCaseFilter( ts.getTokenStream() ) );
                    ts = new TokenStreamComponents( ts.getSource(), new KStemFilter( ts.getTokenStream() ) );
                    return ts;
                }
            };

            String field = "text"; // o campo que desejamos pesquisar
            QueryParser parser = new QueryParser( field, analyzer ); //  o query parser transforma o texto em um objeto de query Lucene

            String qstr = "query reformulation"; // Aqui é a busca textual
            Query query = parser.parse( qstr ); // Esse objeto é o query do Lucene

            // Abrimos o índice e iniciamos as buscas
            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
            IndexReader index = DirectoryReader.open( dir );

            IndexSearcher searcher = new IndexSearcher( index );

            // A classe de similaridade deve ser a mesma que utilizamos inicialmente
            searcher.setSimilarity( new BM25SimilarityOriginal() );

            int top = 5; // Pegamos os top 5 resultados
            TopDocs docs = searcher.search( query, top );

            System.out.printf( "%-10s%-20s%-10s%s\n", "Rank", "DocNo", "Score", "Author" );
            int rank = 1;
            for ( ScoreDoc scoreDoc : docs.scoreDocs ) {
                int docid = scoreDoc.doc;
                double score = scoreDoc.score;
                String docno = LuceneUtils.getDocno( index, "docno", docid );
                String author = LuceneUtils.getDocno( index, "author", docid );
                System.out.printf( "%-10d%-20s%-10.4f%s\n", rank, docno, score, author );
                rank++;
            }

            TopDocs textDocs = searcher.search( query, top );

            System.out.printf( "%-10s%-20s%-10s%s\n", "Rank", "DocNo", "Score", "Text" );
            rank = 1;
            for ( ScoreDoc scoreDoc : textDocs.scoreDocs ) {
                int docid = scoreDoc.doc;
                double score = scoreDoc.score;
                String docno = LuceneUtils.getDocno( index, "docno", docid );
                String text = LuceneUtils.getDocno( index, "text", docid );
                System.out.printf( "%-10d%-20s%-10.4f%s\n", rank, docno, score, text );
                rank++;
            }


            index.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
