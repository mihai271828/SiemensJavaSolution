package com.siemens.internship;


import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@SpringBootTest
class InternshipApplicationTests {

	@Mock
	private ItemRepository repo;

	@InjectMocks
	private ItemService service;

	@BeforeEach
	void setUp() {
	}

	@Test
	void save_AssignsId_WhenEmailIsValid() {

		Item input = new Item(null, "Pen", "Writes stuff", "ACTIVE", "pen@x.com");
		when(repo.save(any())).thenAnswer(i -> {
			Item saved = i.getArgument(0);
			saved.setId(42L);
			return saved;
		});

		Item result = service.save(input);

		assertEquals(42L, result.getId());
		verify(repo).save(input);
	}


	@Test
	void testFindAll() {
		List<Item> items = of(new Item(1L, "A", "Desc", "NEW", "a@x.com"));
		when(repo.findAll()).thenReturn(items);

		List<Item> result = service.findAll();

		assertEquals(1, result.size());
		assertEquals("A", result.get(0).getName());
		verify(repo).findAll();
	}

	@Test
	void testFindByIdFound() {
		Item item = new Item(1L, "B", "Desc", "NEW", "b@x.com");
		when(repo.findById(1L)).thenReturn(Optional.of(item));

		Optional<Item> result = service.findById(1L);

		assertTrue(result.isPresent());
		assertEquals("B", result.get().getName());
		verify(repo).findById(1L);
	}

	@Test
	void testFindByIdNotFound() {
		when(repo.findById(2L)).thenReturn(Optional.empty());

		Optional<Item> result = service.findById(2L);

		assertFalse(result.isPresent());
		verify(repo).findById(2L);
	}

	@Test
	void testSave() {
		Item item = new Item(null, "C", "Desc", "NEW", "c@x.com");
		Item saved = new Item(3L, "C", "Desc", "NEW", "c@x.com");
		when(repo.save(item)).thenReturn(saved);

		Item result = service.save(item);

		assertEquals(3L, result.getId());
		verify(repo).save(item);
	}

	@Test
	void testDeleteById() {
		service.deleteById(4L);

		verify(repo).deleteById(4L);
	}

	@Test
	void testProcessItemsAsync() throws Exception {

		when(repo.findAllIds()).thenReturn(of(1L, 2L));

		when(repo.findById(anyLong()))
				.thenAnswer(inv -> {
					Long id = inv.getArgument(0);
					return Optional.of(new Item(id, "Name" + id, "D", "NEW", id + "@x.com"));
				});

		when(repo.save(any(Item.class)))
				.thenAnswer(inv -> {
					Item input = inv.getArgument(0);
					input.setStatus("Processed");
					return input;
				});

		CompletableFuture<List<Item>> future = service.processItemsAsync();
		List<Item> result = future.get(1, TimeUnit.SECONDS);

		assertEquals(2, result.size(),     "Should process exactly 2 items");
		assertTrue(result.stream()
						.allMatch(i -> "Processed".equals(i.getStatus())),
				"Every item must be marked Processed");


		verify(repo).findAllIds();
		verify(repo, times(2)).findById(anyLong());
		verify(repo, times(2)).save(any(Item.class));
	}


}


