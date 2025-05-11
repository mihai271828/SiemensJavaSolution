package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@WebMvcTest(ItemController.class)
class ItemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/items returns list of items")
    void getAllItems_returnsList() throws Exception {
        List<Item> items = List.of(
                new Item(1L, "Pen", "Writes stuff", "NEW", "a@x.com"),
                new Item(2L, "Pencil", "Sketch tool", "NEW", "b@x.com")
        );
        when(itemService.findAll()).thenReturn(items);

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Pen"));
    }

    @Test
    @DisplayName("GET /api/items/{id} returns item when found")
    void getItemById_found() throws Exception {
        Item item = new Item(1L, "Pen", "Desc", "NEW", "a@x.com");
        when(itemService.findById(1L)).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("a@x.com"));
    }

    @Test
    @DisplayName("GET /api/items/{id} returns 404 when not found")
    void getItemById_notFound() throws Exception {
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/items with valid payload returns 201")
    void createItem_valid() throws Exception {
        Item input = new Item(null, "Pen", "Desc", "NEW", "c@x.com");
        Item saved = new Item(3L, "Pen", "Desc", "NEW", "c@x.com");
        when(itemService.save(any(Item.class))).thenReturn(saved);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("POST /api/items with invalid email returns 400")
    void createItem_invalidEmail() throws Exception {
        Item input = new Item(null, "Pen", "Desc", "NEW", "bad-email");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/items/{id} returns 204 when deleted")
    void deleteItem_found() throws Exception {
        when(itemService.findById(5L)).thenReturn(Optional.of(new Item()));
        Mockito.doNothing().when(itemService).deleteById(5L);

        mockMvc.perform(delete("/api/items/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/items/{id} returns 404 when not found")
    void deleteItem_notFound() throws Exception {
        when(itemService.findById(5L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/items/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/items/process invokes async processing")
    void processItems_invokesAsync() throws Exception {
        List<Item> processed = List.of(new Item(1L, "X", "D", "Processed", "x@x.com"));
        CompletableFuture<List<Item>> future = CompletableFuture.completedFuture(processed);
        when(itemService.processItemsAsync()).thenReturn(future);

        MvcResult mvcResult = mockMvc.perform(get("/api/items/process"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("Processed"));
    }
}
