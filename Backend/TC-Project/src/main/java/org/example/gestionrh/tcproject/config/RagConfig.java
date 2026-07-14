package org.example.gestionrh.tcproject.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class RagConfig {

    private static final String STORE_PATH = "vector_store.json";

    /**
     * Local embedding model — converts text to vectors.
     * Runs entirely on-device, no API calls needed for indexing.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * In-memory vector store persisted to disk as JSON.
     * Loads existing store from disk if already indexed.
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        Path path = Paths.get(STORE_PATH);
        if (Files.exists(path)) {
            System.out.println("Loading existing vector store from: " + STORE_PATH);
            return InMemoryEmbeddingStore.fromFile(path);
        } else {
            System.out.println("Creating new in-memory vector store...");
            return new InMemoryEmbeddingStore<>();
        }
    }
}
