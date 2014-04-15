package fi.aalto.ir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Comparison {

	public Directory directory = null;
	public Analyzer analyzer = null;
    public int docsCount = 0;

    public Similarity similarity = null;

	public Comparison() {

	}

    public Comparison(Similarity similarity, Directory directory, int docsCount) {
        this.similarity = similarity;
        this.directory = directory;
        this.docsCount = docsCount;
    }

    public void index(List<DocumentInCollection> docs) {
		this.analyzer = new StandardAnalyzer(Version.LUCENE_42); // Uses default
															// StopAnalyzer.ENGLISH_STOP_WORDS_SET

		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_42, this.analyzer);
		conf.setSimilarity(this.similarity);
		conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		try {
			IndexWriter writer = new IndexWriter(this.directory, conf);
			for (DocumentInCollection doc : docs) {
				String[] abs = doc.getAbstractText().split(" ");
				String stemmed = "";

				for (String word : abs) {
					stemmed += stem(word) + " ";
				}

				Document document = new Document();
				document.add(new Field("abstract", stemmed,
						TextField.TYPE_STORED));
				document.add(new Field("relevance", doc.isRelevant() && (doc.getSearchTaskNumber() == 16) ? "1" : "0",
                        TextField.TYPE_STORED));

				writer.addDocument(document);
			}
			writer.commit();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public List<String> search(List<String> inAbstract, String relevance, String type) {

		printQuery(inAbstract, type);

		List<String> results = new LinkedList<String>();

		try {
			IndexReader reader = DirectoryReader.open(this.directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(this.similarity);

			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.setMinimumNumberShouldMatch(1);

			if (inAbstract != null) {
				for (String term : inAbstract) {
					Term termm = new Term("abstract", term);
					Query query = new TermQuery(termm);
					if (term.contains("*")) {
						query = new WildcardQuery(termm);
					}
					booleanQuery.add(query, BooleanClause.Occur.SHOULD);
				}
			}
			
			if (relevance != null) {
				Query q = new TermQuery(new Term("relevance", "1"));
				booleanQuery.add(q, BooleanClause.Occur.SHOULD);
			}

			int total_results = 0;
			int relevant_results = 0;
			ScoreDoc[] docs = searcher.search(booleanQuery, this.docsCount).scoreDocs;
			for (ScoreDoc sd : docs) {
				results.add(searcher.doc(sd.doc).get("relevance"));
				if (searcher.doc(sd.doc).get("relevance").equals("1")) {
					relevant_results += 1;
				}
				total_results += 1;
			}
			
			System.out.println("Relevant results: " + relevant_results + ", total results: " + total_results);
			
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return results;
	}

    public void toCSV(String name, List<double[]> results) {
        File dataFile = new File(new File("diagrams"), name + ".csv");
        DecimalFormat df = new DecimalFormat("0.00");
        try {
            FileWriter stream = null;
            try {
                stream = new FileWriter(dataFile);
                // Write header line
                stream.write("recall, percision\n");
                for (double[] result : results) {
                    stream.write(df.format(result[0]) + ", " + df.format(result[1]) + "\n");
                }
            } finally {
                if (stream != null) stream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

	public void printQuery(List<String> inAbstract, String type) {
		System.out.print("Search (");
		if (inAbstract != null) {
			System.out.print("in abstract: " + inAbstract);
		}
		if (type != null) {
			System.out.print(", " + type);
		}
		System.out.println("):");
	}

	/*public void printResults(List<String> results) {
		if (results.size() > 0) {
			for (int i = 0; i < results.size(); i++)
				System.out.println(" " + (i + 1) + ". " + results.get(i));
		} else
			System.out.println(" no results");
	}*/

	public static String stem(String word) {
		Stemmer s = new Stemmer();
		s.add(word.toCharArray(), word.length());
		s.stem();
		return s.toString();
	}

    public static List<double[]> calculate(List<String> results, int countOfRelevants) {
        List<String> xs = new ArrayList<String>();
        List<double[]> prerecall = new ArrayList<double[]>();
        for (String result : results) {
            xs.add(result);
            prerecall.add(recallAndPrecision(xs, countOfRelevants));
        }
        return steps(prerecall);
    }

    public static List<double[]> steps(List<double[]> recalpres) {
        double step = 0.0;
        List<double[]> steps = new ArrayList<double[]>();
        for (double[] recalpre : averagePercision(recalpres)) {
            if (recalpre[0] >= step && step <= 1) {
                steps.add(recalpre);
                step += 0.1;
            }
        }
        return steps;
    }

    public static List<double[]> averagePercision(List<double[]> recalpres) {
        List<double[]> averages = new ArrayList<double[]>();
        for (Map.Entry<Double, List<Double>> grouped : groupByRecall(recalpres).entrySet()) {
            double sum = 0.0;
            int length = grouped.getValue().size();
            for (double val : grouped.getValue()) {
                sum += val;
            }
            averages.add(new double[]{grouped.getKey(), sum/length});
        }
        return averages;
    }

    public static Map<Double, List<Double>> groupByRecall(List<double[]> recalpres) {
        Map<Double, List<Double>> grouped = new TreeMap<Double, List<Double>>();
        for (double[] recalpre : recalpres) {
            List<Double> values = grouped.get(recalpre[0]);
            if (values != null) {
                values.add(recalpre[1]);
            } else {
                grouped.put(recalpre[0], new ArrayList<Double>(Arrays.asList(recalpre[1])));
            }
        }
        return grouped;
    }

    public static double[] recallAndPrecision(List<String> results, int countOfRelevants) {
        double tp = 0;
        double fp = 0;
        for (String result : results) {
            if (result.equals("1")) {
                tp++;
            } else {
                fp++;
            }
        }
        double fn = countOfRelevants - tp;
        double recall = tp / (tp + fn);
        double precision = tp / (tp + fp);
        return new double[]{recall, precision};
    }

	public static void main(String[] args) {
		if (args.length > 0) {
			String indexDir = System.getProperty("user.dir") + "/indicies";
            String indexDir2 = System.getProperty("user.dir") + "/indicies2";
            Directory directory = null;
            Directory directory2 = null;

			try {
				directory = FSDirectory.open(new File(indexDir));
                directory2  = FSDirectory.open(new File(indexDir2));
			} catch (IOException e) {
				e.printStackTrace();
			}

			String[] first_query = "Ergonomics and modern devices".split(" ");
			String[] second_query = "ergonomics tablet typing".split(" ");
			String[] third_query = "tablet and ergo*".split(" ");

			List<String> stemmed_first_query = new ArrayList<String>();
			List<String> stemmed_second_query = new ArrayList<String>();
			List<String> stemmed_third_query = new ArrayList<String>();

			for (String word : first_query) {
				stemmed_first_query.add(stem(word));
			}
			for (String word : second_query) {
				stemmed_second_query.add(stem(word));
			}
			for (String word : third_query) {
				stemmed_third_query.add(stem(word));
			}

			Map<String, List<String>> queries = new HashMap<String, List<String>>();
			queries.put("first", stemmed_first_query);
			queries.put("second", stemmed_second_query);
			queries.put("third", stemmed_third_query);


            DocumentCollectionParser parser = new DocumentCollectionParser();
            parser.parse(args[0]);
            List<DocumentInCollection> docs = parser.getDocuments();
            int count = docs.size();

            DefaultSimilarity vsm = new DefaultSimilarity();
            BM25Similarity bm25 = new BM25Similarity();
            Comparison comparisonVSM = new Comparison(vsm, directory, count);
			Comparison comparisonBM25 = new Comparison(bm25, directory2, count);

            comparisonVSM.index(docs);
			comparisonBM25.index(docs);


			List<String> inAbstract;
			List<String> results;
			
			// Count of relevant documents
			inAbstract = new LinkedList<String>();
			results = comparisonBM25.search(inAbstract, "1", "BM25");
			int countOfRelevant = results.size();

            for (Map.Entry<String, List<String>> query : queries.entrySet()) {
				inAbstract = new LinkedList<String>();
				for (String word : query.getValue()) {
					inAbstract.add(word);
				}

				results = comparisonBM25.search(inAbstract, null, "BM25");

                comparisonBM25.toCSV(query.getKey() + "_BM25", calculate(results, countOfRelevant));

				results = comparisonVSM.search(inAbstract, null, "VSM");

                comparisonVSM.toCSV(query.getKey() + "_VSM", calculate(results, countOfRelevant));
			}
			
			try {
				if (directory != null) directory.close();
                if (directory2 != null) directory2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("ERROR: the path of a ACM XML file has to be passed as a command line argument.");
		}

	}
}
