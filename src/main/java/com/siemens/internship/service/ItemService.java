package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    //proper singleTon
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    //thread-safe collections for shared state
    private final List<Item> processedItems = Collections.synchronizedList(new CopyOnWriteArrayList<>());
    //use AtomicInteger for concurrent updates to primitive counts
    private final AtomicInteger processedCount = new AtomicInteger(0);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds=itemRepository.findAllIds();
        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id->CompletableFuture.runAsync(()-> {
                    try {
                        Thread.sleep(100);
                        itemRepository.findById(id).ifPresent(item -> {
                            item.setStatus("Processed");
                            Item savedItem = itemRepository.save(item);
                            processedItems.add(savedItem);
                            processedCount.incrementAndGet();
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException("Processing interrupted", e);
                    } catch (Exception e) {
                        throw new CompletionException("Error processing item: " + id, e);
                    }
                },executor)
                        .exceptionally(ex->{
                                    System.err.println("Error processing item: " + ex.getMessage());
                                    return null;}))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processedItems);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try{
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }


}

