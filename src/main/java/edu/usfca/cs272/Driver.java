package edu.usfca.cs272;

import java.nio.file.Path;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author Kayvan Zahiri
 * @author CS 272 Software Development (University of San Francisco)
 * @version Spring 2024
 */
public class Driver {
	/**
	 * Main method
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		ArgumentParser parser = new ArgumentParser(args);

		if (parser.hasFlag("-threads") || parser.hasFlag("-html")) {
			int numThreads = 5;
			try {
				numThreads = Integer.parseInt(parser.getString("-threads"));
			} catch (Exception e) {
				System.out.println("Invalid number of threads. Using default value.");
			}
			if (numThreads < 1) {
				System.out.println("Invalid number of threads. Using default value.");
				numThreads = 5;
			}

			CustomWorkQueue workQueue = new CustomWorkQueue(numThreads);
			ThreadSafeInvertedIndex indexer = new ThreadSafeInvertedIndex();
			
			ThreadedFileBuilder builder = new ThreadedFileBuilder(indexer, workQueue);
			if (parser.hasFlag("-text")) {
				Path inputPath = parser.getPath("-text");
				try {
					builder.buildStructures(inputPath);
				} catch (Exception e) {
					System.out.println("Error building the structures " + inputPath);
				}

			}

		    if (parser.hasFlag("-html")) {
		        String seed = parser.getString("-html");
		        int total = 1;
				try {
					total = Integer.parseInt(parser.getString("-crawl"));
				} catch (Exception e) {
					System.out.println("Invalid total. Using default value.");
				}
				if (total < 1) {
					System.out.println("Invalid total. Using default value.");
					total = 1;
				}
		        try {
		        	System.out.println("Total " + total);
		            WebCrawler crawler = new WebCrawler(indexer, workQueue);
		            crawler.startCrawl(seed, total);
		        } catch (Exception e) {
		            System.out.println("Error crawling HTML content from " + seed);
		            e.printStackTrace();
		        }
		    }
			ThreadedQueryFileProcessor mtProcessor = new ThreadedQueryFileProcessor(indexer, workQueue,
					parser.hasFlag("-partial"));
			if (parser.hasFlag("-query")) {
				Path queryPath = parser.getPath("-query");
				try {
					mtProcessor.processQueries(queryPath);
				} catch (Exception e) {
					System.out.println("Error reading the query file " + queryPath);
				}
			}

			workQueue.shutdown();

			if (parser.hasFlag("-counts")) {
				Path countsPath = parser.getPath("-counts", Path.of("counts.json"));
				try {
					indexer.writeCounts(countsPath);
				} catch (Exception e) {
					System.out.println("Error building the file word counts " + countsPath);
				}
			}

			if (parser.hasFlag("-index")) {
				Path indexPath = parser.getPath("-index", Path.of("index.json"));
				try {
					indexer.writeIndex(indexPath);
				} catch (Exception e) {
					System.out.println("Error building the inverted index " + indexPath);
				}
			}

			if (parser.hasFlag("-html")) {
				Path resultsPath = parser.getPath("-html", Path.of("html.json"));
				try {
					mtProcessor.writeResults(resultsPath);
				} catch (Exception e) {
					System.out.println("Error writing results to file " + resultsPath);
				}
			}

			if (parser.hasFlag("-results")) {
				Path resultsPath = parser.getPath("-results", Path.of("results.json"));
				try {
					mtProcessor.writeResults(resultsPath);
				} catch (Exception e) {
					System.out.println("Error writing results to file " + resultsPath);
				}
			}
		} else {
			InvertedIndex indexer = new InvertedIndex();
			FileBuilder fileBuilder = new FileBuilder(indexer);
			if (parser.hasFlag("-text")) {
				Path inputPath = parser.getPath("-text");
				try {
					fileBuilder.buildStructures(inputPath);
				} catch (Exception e) {
					System.out.println("Error building the structures " + inputPath);
				}
			}

			QueryFileProcessor processor = new QueryFileProcessor(indexer, parser.hasFlag("-partial"));
			if (parser.hasFlag("-query")) {
				Path queryPath = parser.getPath("-query");
				try {
					processor.processQueries(queryPath);
				} catch (Exception e) {
					System.out.println("Error reading the query file " + queryPath);
				}
			}

			if (parser.hasFlag("-counts")) {
				Path countsPath = parser.getPath("-counts", Path.of("counts.json"));
				try {
					indexer.writeCounts(countsPath);
				} catch (Exception e) {
					System.out.println("Error building the file word counts " + countsPath);
				}
			}

			if (parser.hasFlag("-index")) {
				Path indexPath = parser.getPath("-index", Path.of("index.json"));
				try {
					indexer.writeIndex(indexPath);
				} catch (Exception e) {
					System.out.println("Error building the inverted index " + indexPath);
				}
			}

			if (parser.hasFlag("-results")) {
				Path resultsPath = parser.getPath("-results", Path.of("results.json"));
				try {
					processor.writeResults(resultsPath);
				} catch (Exception e) {
					System.out.println("Error writing results to file " + resultsPath);
				}
			}
		}
	}
}
