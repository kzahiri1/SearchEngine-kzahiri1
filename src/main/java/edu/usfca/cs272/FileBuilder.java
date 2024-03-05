package edu.usfca.cs272;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class for building and processing files/directories to generate word counts
 * and an inverted index to write to JSON file
 */
public class FileBuilder {

	/**
	 * The InvertedIndex class used for storing word counts and the inverted index
	 */
	private InvertedIndex indexer;

	/**
	 * Creates a new FileBuilder object with the InvertedIndex
	 *
	 * @param indexer the InvertedIndex object
	 */
	public FileBuilder(InvertedIndex indexer) {
		this.indexer = indexer;
	}

	/**
	 * Returns the InvertedIndex
	 *
	 * @return the InvertedIndex object
	 */
	public InvertedIndex getIndexer() {
		return indexer;
	}

	/**
	 * Builds word count and inverted index structures for the specified input path.
	 *
	 * @param inputPath The path of the file or directory to be processed
	 * @throws IOException If an I/O error occurs
	 */
	public void buildStructures(Path inputPath) throws IOException {
		if (inputPath != null && Files.isDirectory(inputPath)) {
			processDirectory(inputPath, false);
		} else {
			processFile(inputPath);
		}
	}

	/**
	 * Processes the files in the specified directory to generate word counts and
	 * the inverted index
	 *
	 * @param directory The directory to process
	 * @param both      A boolean indicating whether to build both structures
	 * @throws IOException If an I/O error occurs
	 */
	public void processDirectory(Path directory, boolean both) throws IOException {
		try (DirectoryStream<Path> listing = Files.newDirectoryStream(directory)) {
			HashMap<String, Integer> wordCounts = null;
			for (Path path : listing) {
				if (Files.isDirectory(path)) {
					processDirectory(path, both);
				} else {
					String relativePath = directory.resolve(path.getFileName()).toString();

					if (relativePath.toLowerCase().endsWith(".txt") || relativePath.toLowerCase().endsWith(".text")) {
						if (both) {
							wordCounts = processIndexFiles(path);
						} else {
							wordCounts = processCountsFiles(path);
						}

						int totalWords = wordCounts.values().stream().mapToInt(Integer::intValue).sum();
						if (totalWords > 0) {
							indexer.getFileWordCounts().put(relativePath, totalWords);
						}
					}
				}
			}
		}
	}

	/**
	 * Processes file to generate word counts and build the inverted index
	 *
	 * @param location The path of file to be processed
	 * @return A HashMap containing word counts for the file
	 * @throws IOException If an I/O error occurs
	 */
	public HashMap<String, Integer> processIndexFiles(Path location) throws IOException {
		TreeMap<String, TreeMap<String, TreeSet<Integer>>> invertedIndex = this.indexer.getInvertedIndex();

		List<String> lines = Files.readAllLines(location);
		HashMap<String, Integer> wordCounts = new HashMap<>();
		int position = 0;

		for (String line : lines) {
			List<String> wordStems = FileStemmer.listStems(line);

			for (String stemmedWord : wordStems) {
				position += 1;
				wordCounts.put(stemmedWord, wordCounts.getOrDefault(stemmedWord, 0) + 1);

				if (!invertedIndex.containsKey(stemmedWord)) {
					invertedIndex.put(stemmedWord, new TreeMap<>());
				}

				TreeMap<String, TreeSet<Integer>> fileMap = invertedIndex.get(stemmedWord);
				if (!fileMap.containsKey(location.toString())) {
					fileMap.put(location.toString(), new TreeSet<>());
				}

				TreeSet<Integer> positions = fileMap.get(location.toString());
				positions.add(position);
			}
		}
		return wordCounts;
	}

	/**
	 * Processes file to generate word counts
	 * 
	 * @param location The path of the file to be processed
	 * @return A HashMap containing the word counts for the file
	 * @throws IOException If an I/O error occurs
	 */
	public HashMap<String, Integer> processCountsFiles(Path location) throws IOException {
		List<String> lines = Files.readAllLines(location);
		HashMap<String, Integer> wordCounts = new HashMap<>();

		for (String line : lines) {
			List<String> wordStems = FileStemmer.listStems(line);

			for (String stemmedWord : wordStems) {
				if (wordCounts.containsKey(stemmedWord)) {
					int count = wordCounts.get(stemmedWord);
					wordCounts.put(stemmedWord, count + 1);
				} else {
					wordCounts.put(stemmedWord, 1);
				}
			}
		}
		return wordCounts;
	}

	/**
	 * Processes the specified file to generate word counts and an inverted index
	 *
	 * @param location The path of the file to process
	 * @throws IOException If an I/O error occurs
	 */
	public void processFile(Path location) throws IOException {
		if (location != null) {
			List<String> lines = Files.readAllLines(location);

			HashMap<String, Integer> wordCounts = new HashMap<>();
			TreeMap<String, TreeMap<String, TreeSet<Integer>>> invertedIndexMap = new TreeMap<>();

			int position = 0;

			for (String line : lines) {
				List<String> wordStems = FileStemmer.listStems(line);

				for (String stemmedWord : wordStems) {
					position += 1;

					wordCounts.put(stemmedWord, wordCounts.getOrDefault(stemmedWord, 0) + 1);

					if (!invertedIndexMap.containsKey(stemmedWord)) {
						invertedIndexMap.put(stemmedWord, new TreeMap<>());
					}

					TreeMap<String, TreeSet<Integer>> fileMap = invertedIndexMap.get(stemmedWord);
					if (!fileMap.containsKey(location.toString())) {
						fileMap.put(location.toString(), new TreeSet<>());
					}

					TreeSet<Integer> wordPosition = fileMap.get(location.toString());
					wordPosition.add(position);
				}
			}

			InvertedIndex indexer = getIndexer();
			indexer.setFileWordCounts(new TreeMap<>(wordCounts));
			indexer.setInvertedIndex(new TreeMap<>(invertedIndexMap));
		}
	}

    public static List<List<Map<String, Object>>> conductSearch(List<List<String>> processedQueries, InvertedIndex indexer) {
        List<List<Map<String, Object>>> searchResults = new ArrayList<>();

        System.out.println("Processing queries:");

        for (List<String> query : processedQueries) {
            System.out.println("Query: " + query);
            List<Map<String, Object>> result = new ArrayList<>();

            for (String word : query) {
                System.out.println("Searching for word: " + word);

                if (indexer.getInvertedIndex().containsKey(word)) {
                    TreeMap<String, TreeSet<Integer>> wordMap = indexer.getInvertedIndex().get(word);

                    System.out.println("Locations for word '" + word + "': " + wordMap.keySet());

                    for (String location : wordMap.keySet()) {
                        int matchCount = wordMap.get(location).size();
                        int totalWordCount = calculateWordCount(indexer);
                        double score = calculateScore(matchCount, totalWordCount);

                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("count", matchCount);
                        resultMap.put("score", score);
                        resultMap.put("where", location);

                        result.add(resultMap);
                    }
                } else {
                    System.out.println("Word '" + word + "' not found in the inverted index");
                }
            }

            System.out.println("Query result: " + result);
            searchResults.add(result);
        }

        System.out.println("Search results: " + searchResults);
        return searchResults;
    }

    private static int calculateWordCount(InvertedIndex indexer) {
        int totalWordCount = 0;
        for (String word : indexer.getInvertedIndex().keySet()) {
            TreeMap<String, TreeSet<Integer>> wordMap = indexer.getInvertedIndex().get(word);
            for (String location : wordMap.keySet()) {
                totalWordCount += wordMap.get(location).size();
            }
        }
        return totalWordCount;
    }

    private static double calculateScore(int matchCount, int totalWordCount) {
        return (double) matchCount / totalWordCount;
    }
}
