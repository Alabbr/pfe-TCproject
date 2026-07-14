package org.example.gestionrh.tcproject.Services;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class DocumentIngestionService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private static final String STORE_PATH = "vector_store.json";

    private final LlamaParseService llamaParseService;

    // Constructeur : injecte le modèle d'embedding, le vector store et le service LlamaParse
    public DocumentIngestionService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore, LlamaParseService llamaParseService) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.llamaParseService = llamaParseService;
    }

    // Ingère un fichier PDF : extraction Markdown via LlamaParse → chunking → embedding → stockage vectoriel
    public void ingestPdf(File file) {
        try {
            System.out.println("Starting LlamaParse Ingestion for PDF: " + file.getName());
            
            // 1. Extract Markdown from PDF using LlamaParse
            String fullMarkdownText = llamaParseService.extractMarkdownFromPdf(file);
            
            if (fullMarkdownText == null || fullMarkdownText.trim().isEmpty()) {
                System.out.println("Warning: No text extracted from " + file.getName());
                return;
            }

            // 2. Create LangChain4j Document from the Markdown text
            Document document = Document.from(fullMarkdownText);
            document.metadata().add("source", file.getName());

            // 3. Manually split document
            dev.langchain4j.data.document.DocumentSplitter splitter = DocumentSplitters.recursive(1000, 200);
            List<TextSegment> segments = splitter.split(document);

            // Injecter le titre du document dans chaque chunk
            String title = file.getName().replace(".pdf", "").replace("_", " ");
            List<TextSegment> modifiedSegments = segments.stream().map(segment -> {
                String newText = "[Source du document : " + title + "]\n\n" + segment.text();
                return TextSegment.from(newText, segment.metadata());
            }).collect(java.util.stream.Collectors.toList());

            // 4. Perform embedding and ingestion into Vector Store
            List<dev.langchain4j.data.embedding.Embedding> embeddings = embeddingModel.embedAll(modifiedSegments).content();
            embeddingStore.addAll(embeddings, modifiedSegments);
            
            System.out.println("Successfully ingested LlamaParse document: " + file.getName() + " with " + modifiedSegments.size() + " chunks.");
            
            saveStore();

        } catch (Exception e) {
            System.err.println("Error ingesting PDF with LlamaParse: " + file.getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Sauvegarde le vector store sur disque (fichier JSON) pour persistance entre redémarrages
    public void saveStore() {
        if (embeddingStore instanceof InMemoryEmbeddingStore) {
            ((InMemoryEmbeddingStore<TextSegment>) embeddingStore).serializeToFile(STORE_PATH);
            System.out.println("Vector store saved to disk at " + STORE_PATH);
        }
    }
}
