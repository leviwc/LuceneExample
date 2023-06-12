## Ambiente

* Oracle JDK 11
* Lucene 8.10.1

## Instalação

A maneira mais fácil de usar o Lucene em seu projeto é importá-lo usando o Maven.
Nós vamos disponibilizar esse projeto para ser utilizado como base. Mas você pode importar utilizando adicionando 
a dependencia no arquivo pom.xml.

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

Também será necessário adicionar o analyzer e o queryparser ```lucene-analyzers-common``` and ```lucene-queryparser```.

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-analyzers-common</artifactId>
    <version>8.10.1</version>
</dependency>
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-queryparser</artifactId>
    <version>8.10.1</version>
</dependency>
```

Caso queria adicionar de outra forma, poderá utilizar o link abaixo
http://archive.apache.org/dist/lucene/java/8.10.1/

Documentação Oficial:
http://lucene.apache.org/core/8_10_1/


## Build an Index

### Corpus

Fizemos uso de um trectext em formato corpus ( que é um conjunto grande e estruturado de textos)
Iremos fornecer o arquivo utilizado nos exemplos

O corpus inclui a informação de cerca de 100 artigos publicados nas conferências SIGIR(Grupo de Interesse Especial da Associação para Máquinas de Computação em Recuperação de Informações).
Cada artigo está no seguinte formato:

```xml
<DOC>
<DOCNO>ACM-383972</DOCNO>
<TITLE>Relevance based language models</TITLE>
<AUTHOR>Victor Lavrenko, W. Bruce Croft</AUTHOR>
<SOURCE>Proceedings of the 24th annual international ACM SIGIR conference on Research and development in information retrieval</SOURCE>
<TEXT>
We explore the relation between classical probabilistic models of information retrieval and the emerging language modeling approaches. It has long been recognized that the primary obstacle to effective performance of classical models is the need to estimate a relevance model : probabilities of words in the relevant class. We propose a novel technique for estimating these probabilities using the query alone. We demonstrate that our technique can produce highly accurate relevance models, addressing important notions of synonymy and polysemy. Our experiments show relevance models outperforming baseline language modeling systems on TREC retrieval and TDT tracking tasks. The main contribution of this work is an effective formal method for estimating a relevance model with no training data.
</TEXT>
</DOC>
```


Um documento tem cinco campos.
O campo DOCNO especifica um ID exclusivo para cada artigo.
Então nós precisamos construir um índice para os outros quatro campos de texto de modo que possamos recuperar os documentos.

### Processando o texto e criando Indices


Muitos sistemas de recuperação de dados podem exigir que você especifique algumas opções de processamento de texto para indexação e recuperação

* **Tokenização** -- dividir uma sequência de texto em tokens individuais.
* **Case-sensitive** -- A maioria dos sistemas  de recuperação de dados ignora as diferenças entre maiúsculas e minúsculas. 
Mas, às vezes, maiúsculas e minúsculas podem ser importantes, por exemplo, **esperto** e **ESPERTO** (o sistema de recuperação ESPERTO). 
* **Stop words** -- Com a remoção de stop words o tamanho do índice por ser reduzido drasticamente

* **Stemming** -- Nós podemos indexar palavras derivasdas em vez das originas, para ignorar pequenas diferanças.
Os sistemas de recuperação de dados geralmente utilizam a derivação de **Porter** ou **Krovetz**

Original    | Porter    	| Krovetz
--------    | -------   	| -------
relevância 	| relev			| relevância
idioma 		| idiom 		| idioma
modelos 	| modelo 		| modelo



Um documento indexado pode ter diferentes campos para armazenar diferentes tipos de informações.
A maioria dos sistemas de recuperação de dados suporta dois tipos de campos:

* **Metadata field** é semelhante a um campo de registro de banco de dados estruturado.
Eles são armazenados e indexados como um todo sem tokenização.

* **Normal text field** é adequado para conteúdo de texto regular. Os textos são tokenizados e indexados , de modo que você possa pesquisar usando técnicas normais de recuperação de texto.


### Lucene

Agora nós temos um exemplo de um programa que constroi um indice do corpus que utilizamos. 
```java
 
  String pathCorpus = "C:\\Users\\T-GAMER\\Documents\\Lucene\\example_corpus\\example_corpus.gz";
  String pathIndex = "C:\\Users\\T-GAMER\\Documents\\Lucene\\example_corpus\\example_index";
  
  Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
  
  //O analisador especifica opções para tokenização e normalização de texto (por exemplo, stemming, remoção de stop word, case-sensitive)
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
      d.add( new Field( "docno", docno, fieldTypeMetadata ) );
      d.add( new Field( "title", title, fieldTypeText ) );
      d.add( new Field( "author", author, fieldTypeText ) );
      d.add( new Field( "source", source, fieldTypeText ) );
      d.add( new Field( "text", text, fieldTypeText ) );
      System.out.println( "indexing document " + docno );
      ixwriter.addDocument( d );
  }
  
  ixwriter.close();
  dir.close();
```

## Utilizando Índices

### Abrindo e fechando indices

Lucene usa a classe IndexREader para trabalhar com arquivos indexados

```java
String pathIndex = "C:\\Users\\T-GAMER\\Documents\\Lucene\\example_corpus\\example_index"; 

// Abrimos o arquivo
Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );

// Com o arquivo aberto, usamos o IndexReader para acessar o Índice
IndexReader index = DirectoryReader.open( dir );

// Agora podemos manipular o índice sem grandes problemas

index.numDocs(); 

index.close();
dir.close();
``` 


###Acessando um arquivo indexado por um índice

Podemos acessar um documento indexado a partir de um índice.
O índice geralmente é armazenado como um vetor de documento,
que é uma lista de pares <palavra,frequência>

```java
String pathIndex = "C:\\Users\\T-GAMER\\Documents\\Lucene\\example_corpus\\example_index";

// Vamos recuperar o objeto para o Documento com ID interno=21
String field = "text";
int docid = 21;

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
IndexReader index = DirectoryReader.open( dir );

Terms vector = index.getTermVector( docid, field ); // Read the document's document vector.

// Preciamos usar TermsEnum para iterar em cada entrada do vetor do documento.
System.out.printf( "%-20s%-10s%-20s\n", "TERM", "FREQ", "POSITIONS" );
TermsEnum terms = vector.iterator();
PostingsEnum positions = null;
BytesRef term;
while ( ( term = terms.next() ) != null ) {
    
    String termstr = term.utf8ToString(); .
    long freq = terms.totalTermFreq(); // Obtem a frequência.
    
    System.out.printf( "%-20s%-10d", termstr, freq );
    
    // O PostingsEnum inclui apenas uma entrada de documento.
    positions = terms.postings( positions, PostingsEnum.POSITIONS );
    positions.nextDoc(); // you still need to move the cursor
    for ( int i = 0; i < freq; i++ ) {
        System.out.print( ( i > 0 ? "," : "" ) + positions.nextPosition() );
    }
    System.out.println();
}

index.close();
dir.close();

```

The output is:
```
TERM                FREQ      POSITIONS           
1,800               1         92
2007                1         79
95                  1         148
a                   2         19,119
acquire             1         86
algorithm           1         84
along               1         100
analysis            1         103
and                 3         42,111,132
appreciable         1         151
are                 1         51
as                  2         133,135
assessor            1         142
at                  1         77
available           1         52
be                  2         59,145
been                2         5,18
best                1         36
between             1         106
by                  1         147
can                 1         144
complete            1         15
cost                1         130
deal                1         21
deep                1         102
document            2         39,82
dozen               1         9
each                1         11
effective           1         131
effort              2         72,143
error               1         155
estimate            1         45
evaluate            6         2,26,46,62,121,154
few                 3         49,126,136
for                 1         89
great               1         20
has                 2         3,17
how                 2         32,43
in                  2         53,153
increase            1         152
information         1         0
investigate         1         104
is                  1         128
it                  1         57
judge               3         12,41,71
judgment            6         30,50,88,114,127,140
light               1         54
many                1         64
measure             1         47
million             1         74
more                6         65,69,90,123,129,139
much                2         28,68
near                1         14
no                  1         150
number              2         108,112
of                  6         22,38,55,97,109,113
on                  1         25
over                4         7,27,63,122
perform             1         6
point               1         120
possible            1         60
present             1         95
query               7         10,66,75,93,110,124,137
recent              1         23
reduce              1         146
relevance           1         87
reliable            1         134
result              1         96
retrieval           1         1
select              1         34
selection           1         83
set                 2         31,37
several             1         8
should              1         58
show                1         115
small               1         29
than                1         91
that                1         116
the                 4         35,73,98,107
there               1         16
this                1         56
to                  7         13,33,40,44,61,85,118
total               2         70,141
track               2         76,99
tradeoff            1         105
trec                1         78
two                 1         81
typically           1         4
up                  1         117
use                 1         80
we                  1         94
when                1         48
with                4         101,125,138,149
without             1         67
work                1         24
```


## Searching

The following program retrieves the top 10 articles for the query "query reformulation" 
from the example corpus using the BM25 search model. Note that we used the provided ```BM25SimilarityOriginal``` class for search because we built the example index using this class.
If you built your index based on Lucene's default ```BM25Similarity```, you should use the default ```BM25Similarity``` for BM25 search.  

```javaa
  String pathIndex = "C:\\Users\\T-GAMER\\Documents\\Lucene\\example_corpus\\example_index";
 
  Analyzer analyzer = new Analyzer() {{
      @Override
      protected TokenStreamComponents createComponents( String fieldName ) {
          
          TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
          ts = new TokenStreamComponents( ts.getSource(), new LowerCaseFilter( ts.getTokenStream() ) );
          ts = new TokenStreamComponents( ts.getSource(), new KStemFilter( ts.getTokenStream() ) )
          
          return ts;}
  };
  
  String field = "text"; // o campo que desejamos pesquisarr
  QueryParser parser = new QueryParser( field, analyzer ); // o query parser transforma o texto em um objeto de query Lucene
  
  String qstr = "query reformulation"; //Aqui é a busca textualy
  Query query = parser.parse( qstr ); // Esse objeto é o query do Lucenet
  
  // Abrimos o índice e iniciamos as buscass
  Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
  IndexReader index = DirectoryReader.open( dir );
 
  IndexSearcher searcher = new IndexSearcher( index );
 
  // A classe de similaridade deve ser a mesma que utilizamos inicialmenteg
  searcher.setSimilarity( new BM25SimilarityOriginal() );
  
  int top = 10; // Pegamos os top 10 resultadoss
  TopDocs docs = searcher.search( query, top );s
  
  System.out.printf( "%-10s%-20s%-10s%s\n", "Rank", "DocNo", "Score", "Title" );
  int rank = 1;
  for ( ScoreDoc scoreDoc : docs.scoreDocs ) {
      int docid = scoreDoc.doc;
      double score = scoreDoc.score;
      String docno = LuceneUtils.getDocno( index, "docno", docid );
      String title = LuceneUtils.getDocno( index, "title", docid );
      System.out.printf( "%-10d%-20s%-10.4f%s\n", rank, docno, score, title );
      rank++;
  y
  index.close();
  dir.close();
```

The output is:
```
Rank      DocNo               Score     Title
1         ACM-1835626         4.7595    Learning to rank query reformulations
2         ACM-2348355         4.3059    Generating reformulation trees for complex queries
3         ACM-2010085         2.9168    Modeling subset distributions for verbose queries
4         ACM-1277796         2.5193    Latent concept expansion using markov random fields
5         ACM-2484096         2.3669    Compact query term selection using topically related text
6         ACM-2009969         2.3407    CrowdLogging: distributed, private, and anonymous search logging
7         ACM-2609633         2.0099    Searching, browsing, and clicking in a search session: changes in user behavior by task and over time
8         ACM-2609467         0.3593    Diversifying query suggestions based on query documents
9         ACM-1835637         0.3551    Query term ranking based on dependency parsing of verbose queries
10        ACM-2348408         0.3544    Modeling higher-order term dependencies in information retrieval using query hypergraphs
```
